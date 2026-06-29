package rtp.protocol.stop_and_wait;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import rtp.model.RTPHeader;
import rtp.model.RTPMessage;
import rtp.model.RTPMessageFactory;
import rtp.network.UDPClient;
import rtp.protocol.BaseReceiver;

public class StopAndWaitReceiver extends BaseReceiver {
    boolean sendDuplicate = true;

    /**
     * Escuta passivamente o canal, enviando ACK a cada mensagem recebida corretamente.
     * 
     * @param udp
     *      Objeto UDP que vai conectar os hosts.
     * 
     * @param port
     *      Porta que a conexão utiliza.
     */
    public StopAndWaitReceiver(UDPClient udp, int port, int window) {
        super(udp, port, window);
    }

    /**
     * Controla o estado atual do sender. Passa pelas seguintes fases na ordem dada:
     * <ul>
     *  <li> handleHandshake() -> fase de conexão entre hosts
     *  <li> receiveData() -> fase de recebimento dos dados
     *  <li> handleDisconnect() -> fase de termino da conexão
     * <ul>
     * @throws IOException
     */
    public void listen() throws IOException {
        System.out.println("\n<Receiver> Iniciando fase de HANDSHAKE\n");
        handleHandshake();
        System.out.println("\n<Receiver> Iniciando fase de RECEIVEDATA\n");
        receiveData();
        System.out.println("\n<Receiver> Iniciando fase de DISCONNECT\n");
        handleDisconnect();
    }

    /**
     * Fase de preparação para recebimento de mensagens pós handshake.
     * 
     * @throws IOException
     */
    private void receiveData() throws IOException {
        buffer = new ByteArrayOutputStream(); //buffer que guarda, em ordem, os dados com payload recebidos

        //inicia a seq em 0
        expectedSeq = 0; 

        //loop de recebimento dos dados
        while(true) {
            RTPMessage msg = receive();
            //timeoutTest = !timeoutTest;

            //msg == null quando checksum falhou
            if(msg == null) {
                System.out.println("Erro de CHECKSUM");
                continue;
            }

            //checa se a msg eh de desconexao
            if(isFin(msg)) {
                System.out.println("<Receiver> FIN\n");
                send(RTPMessageFactory.finAck());
                break;
            }

            //se nao eh, comeca a receber os dados
            if(isData(msg)) {handleData(msg);}
        }
    }

    /**
     * Método usado para receber mensagens RTP com payload.
     * 
     * @param msg
     *      Mensagem recebida.
     * 
     * @throws IOException
     */
    private void handleData(RTPMessage msg) throws IOException {
        RTPHeader h = msg.getHeader();
        int seq = h.getSeq();
        System.out.println("<Receiver> RECV DATA(" + seq + ")\n");

        //pacote esperado - envia ACK (stop-and-wait ja deixa os dados ordenados)
        if(seq == expectedSeq) {
            buffer.write(msg.getPayload().getPayload());

            RTPMessage ack = RTPMessageFactory.ack(expectedSeq);
            send(ack); //envia ACK
            System.out.println("<Receiver> SEND ACK(" + expectedSeq + ")\n");

            //atualiza ACK (e controla tam. maximo)
            expectedSeq = (expectedSeq + 1) % 16384;
            return;
        }

        //pacote duplicado - reenvia ACK
        if(seq == expectedSeq - 1) {
            send(RTPMessageFactory.ack(seq));
            System.out.println("<Receiver> DUPLICATE DATA");
            System.out.println("<Receiver> SEND ACK(" + expectedSeq + ")\n");

            return;
        }

        if(h.getSeq() != expectedSeq) {
            System.out.println("<Receiver> EXPECTEDSEQ != SEQ");
            return;
        }
    }

    public ByteArrayOutputStream getBuffer() {return buffer;}
    public String getBufferString() {return buffer.toString();}
}
