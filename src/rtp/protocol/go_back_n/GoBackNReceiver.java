package rtp.protocol.go_back_n;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import rtp.model.RTPMessage;
import rtp.model.RTPMessageFactory;
import rtp.network.UDPClient;
import rtp.protocol.BaseReceiver;

public class GoBackNReceiver extends BaseReceiver {

    public GoBackNReceiver(UDPClient udp, int port, int window) {
        super(udp, port, window);
    }

    @Override
    public void listen() throws Exception {
        handleHandshake();
        receiveData();
        handleDisconnect();
    }

    /**
     * Recebe pacotes e aceita apenas aqueles em ordem, rejeita todos fora, enviando NACK com a sequencia esperada.
     * @throws IOException
     */
    private void receiveData() throws IOException {
        buffer = new ByteArrayOutputStream();
        expectedSeq = 0;

        while(true) {
            RTPMessage msg = receive();

            //msg == null quando checksum falhou
            if(msg == null) {
                System.out.println("<Receiver> Erro de CHECKSUM");
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
     * Envia ACK quando recebe os pacotes na ordem correta, NACK para pacotes em ordem errada.
     * 
     * @param msg
     *      Mensagem recebida.
     * 
     * @throws IOException
     *      Em caso de problemas na escrita do buffer e/ou problemas no socket UDP.
     */
    private void handleData(RTPMessage msg) throws IOException {
        int seq = msg.getHeader().getSeq();

        //correto
        if(seq == expectedSeq) {
            System.out.println("<Receiver> RECV DATA(" + seq + ")\n");
            buffer.write(msg.getPayload().getPayload());
            send(RTPMessageFactory.ack(expectedSeq));
            System.out.println("<Receiver> SEND ACK(" + expectedSeq + ")\n");

            expectedSeq++;
            return;
        }

        //duplicado
        if(seq < expectedSeq) {
            System.out.println("<Receiver> RECV DUPLICATE(" + seq + ")\n");
            send(RTPMessageFactory.ack(seq));
            System.out.println("<Receiver> SEND");
            return;
        }

        //fora de ordem
        System.out.println("<Receiver> RECV DATA(" + seq +"), EXPECTED DATA(" + expectedSeq + ")\n");
        send(RTPMessageFactory.nack(expectedSeq));
        System.out.println("<Receiver> SEND NACK(" + expectedSeq +")\n");
    }
}
