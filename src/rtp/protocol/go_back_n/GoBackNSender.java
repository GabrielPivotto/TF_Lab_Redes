package rtp.protocol.go_back_n;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.List;

import rtp.data_source.DataProvider;
import rtp.model.RTPMessage;
import rtp.model.RTPMessageFactory;
import rtp.network.UDPClient;
import rtp.protocol.BaseSender;
import rtp.model.RTPPayload;

public class GoBackNSender extends BaseSender{
    private int base = 0;
    private int nextSeqNum = 0;
    private List<RTPMessage> packets;

    public GoBackNSender(UDPClient udp, InetAddress receiver, int port, int window) throws SocketException {
        super(udp, receiver, port, window);
    }

    @Override
    public void sendFile(File file) throws IOException {
        //fragmenta todo o arquivo em sequencias de mensagens DATA de 255 byte
        packets = fragment(file);

        //define quantos ja foram enviados + confirmados
        base = 0;

        //define quem eh o proximo a ser enviado
        nextSeqNum = 0;

        //enquanto nao enviou tudo
        while(base < packets.size()) {

            //envia toda a janela
            while(nextSeqNum < base + window && nextSeqNum < packets.size()) {
                send(packets.get(nextSeqNum));
                System.out.println("<Sender> SEND DATA(" + nextSeqNum + ")");
                System.out.println("<Sender> BASE = " + base + " | NEXT = " + nextSeqNum + "\n");

                nextSeqNum++;
            }

            //processo de esperar pelos ACKs
            try {

                // !!== talvez fazer a leitura de ACK em loop? ==!!
                RTPMessage resp = receive();

                if(isNack(resp)) {
                    //NACK aponta para o primeiro valor a ser retransmitido
                    int nack = resp.getHeader().getAck();
                    System.out.println("<Sender> RECV NACK(" + nack + ")\n");

                    //checa se NACK esta na janela
                    if(nack >= base && nack < nextSeqNum) {
                        retransmitFrom(nack);
                    }
                }
                else if(isAck(resp)) {
                    int ack = resp.getHeader().getAck();
                    System.out.println("<Sender> RECV ACK(" + ack + ")\n");

                    //evita ACKs fora de window (ACK(100) por exemplo)
                    if(ack >= base && ack < nextSeqNum) {
                        base = ack + 1;
                    }
                    
                }
                
            } catch (SocketTimeoutException e) {
                System.out.println("<Sender> Timeout, retransmitindo a partir da base = " + base + "\n");

                //em caso de timeout, retransmite da base
                retransmitFrom(base);
            }
        }
    }

    /**
     * Método usado para retransmitir tudo a partir de {@code seq} até {@code nextSeqNum}.
     * 
     * @param seq
     *      Valor sequência que se deseja começar a retransmissão.
     * 
     * @throws IOException 
     *      Em caso de erro com o socket UDP.
     */
    private void retransmitFrom(int seq) throws IOException {
        System.out.println("<Sender> Retransmitindo de " + seq + " ate " + (nextSeqNum-1));
        for(int i = seq; i < nextSeqNum; i++) {
            send(packets.get(i));
            retransmissions++;
        }
        System.out.println("<Sender> Retransmissao acabou\n");
    }

    /**
     * Armazena todas as mensagens para uma lista.
     * 
     * @param file
     *      Arquivo a ser enviado.
     * 
     * @return
     *      Lista com todas as mensagens DATA necessárias.
     * 
     * @throws IOException
     *      Em caso de erro com a leitura do arquivo.
     */
    private List<RTPMessage> fragment(File file) throws IOException {
        currSeq = 0;

        List<RTPMessage> l = new ArrayList<RTPMessage>();
        DataProvider provider = new DataProvider(file, 255);

        while(true) {
            RTPPayload payload = provider.getNextChunk();
            if(payload == null) {break;}

            l.add(RTPMessageFactory.data(currSeq, payload));
            currSeq++;
        }

        return l;
    }
}
