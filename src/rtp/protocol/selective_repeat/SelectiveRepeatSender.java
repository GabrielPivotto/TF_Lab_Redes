package rtp.protocol.selective_repeat;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import rtp.data_source.DataProvider;
import rtp.model.RTPMessage;
import rtp.model.RTPMessageFactory;
import rtp.model.RTPPayload;
import rtp.network.UDPClient;
import rtp.protocol.BaseSender;

public class SelectiveRepeatSender extends BaseSender{
    private int base = 0;
    private int nextSeqNum = 0;
    private List<RTPMessage> packets;
    private List<Boolean> confirmed;    //armazena a confirmacao de cada pacote
    private List<Long> sendTimes;       //armazena, em ms, o tempo do envio dos pacotes

    public SelectiveRepeatSender(UDPClient udp, InetAddress receiver, int port, int window) throws SocketException {
        super(udp, receiver, port, window);
    }

    @Override
    public void sendFile(File file) throws IOException {
        //fragmenta todo o arquivo em sequencias de mensagens DATA de 255 byte
        packets = fragment(file);

        //lista dos pacotes confirmados (ACK)
        confirmed = new ArrayList<>(Collections.nCopies(packets.size(), false));

        //lista dos timers dos pacotes
        sendTimes = new ArrayList<>(Collections.nCopies(packets.size(), 0L));
        //sendTimes.set(nextSeqNum, System.currentTimeMillis());

        //define quantos ja foram enviados + confirmados
        base = 0;

        //define quem eh o proximo a ser enviado
        nextSeqNum = 0;

        //enquanto nao enviou tudo
        while(base < packets.size()) {

            //envia toda a janela
            while(nextSeqNum < base + window && nextSeqNum < packets.size()) {
                send(packets.get(nextSeqNum));
                sendTimes.set(nextSeqNum, System.currentTimeMillis());
                System.out.println("<Sender> SEND DATA(" + nextSeqNum + ")");
                System.out.println("<Sender> BASE = " + base + " | NEXT = " + nextSeqNum + "\n");

                nextSeqNum++;
            }

            //processo de esperar pelos ACKs
            try {

                // !!== talvez fazer a leitura de ACK em loop? == !!
                RTPMessage resp = receive();

                if(isNack(resp)) {
                    int nack = resp.getHeader().getAck();
                    System.out.println("<Sender> RECV NACK(" + nack + ")\n");

                    if(nack >= base && nack < nextSeqNum) {
                        retransmit(nack);
                    }
                }
                else if(isAck(resp)) {
                    int ack = resp.getHeader().getAck();
                    System.out.println("<Sender> RECV ACK(" + ack + ")\n");

                    if(ack >= base && ack < nextSeqNum && !confirmed.get(ack)) {
                        confirmed.set(ack, true);
                        updateBase();
                    }
                }
                
            } catch (SocketTimeoutException e) {
                System.out.println("<Sender> Timeout");
            }

            //a cada fim de iteracao, eh checado os timeouts dos pacotes
            checkTimeouts();
        }
    }
    
    private void checkTimeouts() throws IOException {
        for(int i = base; i < nextSeqNum; i++) {
            if(timeout(i)) {
                System.out.println("<Sender> Timeout de DATA(" + i + ")");
                send(packets.get(i));
                sendTimes.set(i, System.currentTimeMillis());
                System.out.println("<Sender> SEND DATA(" + i + ")\n");

            }
            
        }
    }

    private boolean timeout(int seq) {
        //se deu timeout, mas ele ja foi reconhecido
        if(confirmed.get(seq)) {return false;}

        long now = System.currentTimeMillis();
        return now - sendTimes.get(seq) >= TIMEOUT;
    }

    /**
     * Atualiza a base da janela até o próximo pacote não confirmado
     */
    private void updateBase() {
        System.out.println("<Sender> Atualizando base...");
        for(; base < confirmed.size(); base++) {
            if(!confirmed.get(base)) {return;}
        }
    }

    /**
     * Método usado para retransmitir todos os pacotes ainda não reconhecidos na janela.
     * 
     * @param seq
     *      Valor sequência que se deseja começar a retransmissão.
     * 
     * @throws IOException 
     *      Em caso de erro com o socket UDP.
     */
    private void retransmit(int seq) throws IOException {
        if(!confirmed.get(seq)) {
            System.out.println("<Sender> Retransmitindo nao-confirmado DATA(" + seq + ")\n");
            send(packets.get(seq));
            sendTimes.set(seq, System.currentTimeMillis());
            System.out.println("<Sender> SEND DATA (" + seq + ")\n");
            retransmissions++;
        }
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
