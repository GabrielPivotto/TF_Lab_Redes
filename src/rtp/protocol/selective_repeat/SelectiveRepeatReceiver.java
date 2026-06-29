package rtp.protocol.selective_repeat;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import rtp.model.RTPMessage;
import rtp.model.RTPMessageFactory;
import rtp.network.UDPClient;
import rtp.protocol.BaseReceiver;

public class SelectiveRepeatReceiver extends BaseReceiver{
    private Map<Integer, RTPMessage> bufferMap = new HashMap<>();

    public SelectiveRepeatReceiver(UDPClient udp, int port, int window) {
        super(udp, port, window);
    }

    @Override
    public void listen() throws Exception {
        System.out.println("\n<Receiver> Iniciando fase de HANDSHAKE\n");
        handleHandshake();
        System.out.println("\n<Receiver> Iniciando fase de RECEIVEDATA\n");
        receiveData();
        System.out.println("\n<Receiver> Iniciando fase de DISCONNECT\n");
        handleDisconnect();    
    }

    private void receiveData() throws IOException {
        buffer = new ByteArrayOutputStream();
        expectedSeq = 0;

        while(true) {
            RTPMessage msg = receive();

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
     * Envia ACK individual para os pacotes recebidos, e detecta lacunas entre pacotes.
     * @param msg
     * @throws IOException
     */
    private void handleData(RTPMessage msg) throws IOException {
        int seq = msg.getHeader().getSeq();        
        System.out.println("\n<Receiver> RECV DATA(" + seq + ")\n");


        //se receber um pacote ainda em buffer
        if(bufferMap.containsKey(seq)) {
            send(RTPMessageFactory.ack(seq));
            System.out.println("\n<Receiver> Pacote em buffer, SEND ACK = " + seq + "\n");
            return;
        }

        //pacotes duplicados
        if(seq < expectedSeq){ 
            send(RTPMessageFactory.ack(seq));
            System.out.println("\n<Receiver> Pacote duplicado, SEND ACK(" + seq + ")\n");
            return;
        }

        //fora da janela
        if(seq >= expectedSeq + window) {
            System.out.println("\n<Receiver> Pacote fora da janela\n");
            return;
        }
        
        //novo pacote
        if(!bufferMap.containsKey(seq)) {
            bufferMap.put(seq, msg);
            send(RTPMessageFactory.ack(seq));
            System.out.println("\n<Receiver> SEND ACK(" + seq + ")\n");

            //tem lacuna?
            if(seq > expectedSeq) {
                send(RTPMessageFactory.nack(expectedSeq));
                System.out.println("\n<Receiver> Lacuna detectada, SEND NACK(" + expectedSeq + ")\n");
            }
        }

        

        writeInBuffer();
    }

    /**
     * Escreve o conteudo do HashMap no buffer
     */
    private void writeInBuffer() {
        //enquanto tiver pacotes para serem escritos
        while(bufferMap.containsKey(expectedSeq)) {
            RTPMessage packet = bufferMap.remove(expectedSeq);

            try {
                buffer.write(packet.getPayload().getPayload());
            } catch (IOException e) {
                System.out.println("<Receiver> Erro ao escrever no buffer");
            }

            expectedSeq++;
        }
    }
}

