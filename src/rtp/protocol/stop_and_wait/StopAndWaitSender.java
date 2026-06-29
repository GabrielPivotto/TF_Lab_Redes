package rtp.protocol.stop_and_wait;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;

import rtp.data_source.DataProvider;
import rtp.model.RTPMessage;
import rtp.model.RTPMessageFactory;
import rtp.model.RTPPayload;
import rtp.network.UDPClient;
import rtp.protocol.BaseSender;

public class StopAndWaitSender extends BaseSender {
    

    /**
     * Envia para receiver arquivos definidos pelo usuário.
     * 
     * @param udp
     *      Objeto UDP que vai conectar os hosts.
     *      
     * @param receiver
     *      Endereço do receiver a enviar o arquivo.
     * 
     * @param port
     *      Porta que a conexão utiliza.
     * 
     * @throws SocketException
     */
    public StopAndWaitSender(UDPClient udp, InetAddress receiver, int port, int window) throws SocketException {
        super(udp, receiver, port, window);
    }

    /**
     * Trata de todo o processo de envio do arquivo ao receiver.
     * 
     * @param file
     *      Arquivo que se deseja enviar.
     * 
     * @throws IOException
     */
    public void sendFile(File file) throws IOException {
        DataProvider provider = new DataProvider(file, 255);

        RTPPayload payload;

        //int chunkNum = 0;
        //loop de envio dos dados
        while(provider.hasNextChunk()) {
            payload = provider.getNextChunk();

            if(payload == null) {continue;}

            RTPMessage msg = RTPMessageFactory.data(currSeq, payload);
            waitForAck(msg);

            //atualiza a seq do pacote
            currSeq = (currSeq + 1) % 16384;
        }
    }

    /**
     * Espera de forma indefinida pelo ACK, retransmitindo em caso de TIMEOUT.
     * 
     * @param msg
     *      Mensagem que se espera o ACK.
     * 
     * @throws IOException
     */
    private void waitForAck(RTPMessage msg) throws IOException {
        while(true) {
            send(msg);
            System.out.println("<Sender> SEND DATA(" + msg.getHeader().getSeq() + ")\n");

            try {
                RTPMessage resp = receive();

                //checa se o ACK esta correto
                if(isAckFor(resp, currSeq)) {
                    System.out.println("<Sender> RECV ACK(" + resp.getHeader().getAck() + ") \n");
                    return;
                }

                //em caso de ACK inesperado ou atrasado/duplicado
                System.out.println("<Sender> ACK inesperado = " + resp.getHeader().getAck() + "\n");

            } catch (SocketTimeoutException e) {
                System.out.println("<Sender> Timeout, retransmitindo DATA(" + currSeq +")\n");
                retransmissions++;
            }
        }
    }
}
