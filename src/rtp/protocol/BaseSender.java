package rtp.protocol;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;

import rtp.codec.RTPDecoder;
import rtp.codec.RTPEncoder;
import rtp.model.RTPHeader;
import rtp.model.RTPMessage;
import rtp.model.RTPMessageFactory;
import rtp.network.UDPClient;

public abstract class BaseSender {
    protected static final int TIMEOUT = 100;
    protected UDPClient udp;
    protected int currSeq = 0;
    protected InetAddress receiver;
    protected int port;
    protected int window;
    protected int retransmissions = 0;

    /**
     * Classe base do SENDER.
     */
    protected BaseSender(UDPClient udp, InetAddress receiver, int port, int window) throws SocketException {
        this.udp = udp;
        this.receiver = receiver;
        this.port = port;
        this.window = window;

        udp.setTimeout(TIMEOUT);
    }

    /**
     * Conecta o sender ao receiver definido no construtor.
     * 
     * @throws IOException
     *      Em caso de problemas com o socket UDP.
     */
    public void connect() throws IOException {
        while(true) {
            int newWindow = window;
            RTPMessage syn = RTPMessageFactory.syn(window);

            send(syn);
            System.out.println("<Sender> SEND SYN \n");
            RTPMessage resp = null;
            try {
                 resp = receive();
            } catch (Exception e) {
                System.out.println("<Sender> Retransmitindo SYN");
            }

            if(resp == null) {continue;}
            
            //se recebeu syn+ack...
            if(isSynAck(resp)) {
                send(RTPMessageFactory.ack(0));

                long start = System.currentTimeMillis();
                while(System.currentTimeMillis() - start < 3000) { //fica um tempo esperando pelo syn+ack 
                    try {
                        RTPMessage synAckDup = receive();
                        System.out.println("<Sender> RECV SYN+ACK novamente \n");
                        
                        //definindo qual eh o "window" menor
                        if(synAckDup.getHeader().getLength() < window) {
                            newWindow = synAckDup.getHeader().getLength();
                        }

                        if(isSynAck(synAckDup)) {
                            send(RTPMessageFactory.ack(0));
                            System.out.println("<Sender> SEND ACK novamente \n");
                        }
                    } catch (SocketTimeoutException e) {
                        System.out.println("<Sender> Nada de SYN+ACK duplicado ainda\n");
                    }
                }
            
                System.out.println("<Sender> Trocando porta de listen...");

                //apos terminar fase de conexao, troca seu listen para "port+1"
                udp = new UDPClient(port+1);
                udp.setTimeout(TIMEOUT);

                System.out.println("<Sender> oldWindow="+window+" | newWindow="+newWindow);
                window = newWindow;

                return;
            }
        }
    }

    /**
     * Trata o processo de termino da conexão.
     * 
     * @throws IOException
     *      Em caso de problemas com o socket UDP.
     */
    public void disconnect() throws IOException {
        System.out.println("Sender desconectando...");
        while(true) {
            RTPMessage fin = RTPMessageFactory.fin(currSeq);
            send(fin);
            System.out.println("<Sender> SEND FIN \n");

            try {
                RTPMessage resp = receive();

                if(isFinAck(resp)) {
                    System.out.println("<Sender> RECV FIN+ACK \n");
                    udp.close();
                    System.out.println("<Sender> desconectado!");
                    showResults();
                    return;
                }
            } catch (SocketTimeoutException e) {
                System.out.println("<Sender> Timeout durante DISCONNECT");
                retransmissions++;
            }
        }
    }

    /**
     * Mostra informações adicionais sobre a conexão em geral
     */
    protected void showResults() {
        System.out.println("TOTAL RETRANSMISSIONS=" + retransmissions);
    }

    /**
     * Envia mensagens através do socket UDP.
     * 
     * @param msg
     *      Mensagem a ser enviada.
     * 
     * @throws IOException
     *      Em caso de problemas com o socket UDP.
     */
    protected void send(RTPMessage msg) throws IOException {
        byte[] packet = RTPEncoder.encode(msg);
        System.out.println("ENVIANDO="+msg);

        udp.send(packet, receiver, port);
    }

    public abstract void sendFile(File file) throws IOException;

    /**
     * Espera em socket até receber qualquer mensagem.
     * 
     * @return
     *      Objeto {@code RTPMessage} representante da mensagem em bytes.
     * 
     * @throws IOException
     *      Em caso de problemas com o socket UDP.
     */
    protected RTPMessage receive() throws IOException {
        byte[] packet = udp.receive();

        RTPMessage msg = RTPDecoder.decode(packet);

        if(msg == null) {
            System.out.println("<Sender> CRC invalidado, descartando pacote...");
            System.out.println("RECEBENDO="+msg);
            System.out.println("<Sender> RECV INVALID\n");
            return RTPMessageFactory.invalid();
        }
        System.out.println("RECEBENDO="+msg);

        return msg;
    }

    protected boolean isSynAck(RTPMessage msg) {
        RTPHeader h = msg.getHeader();
        return h.isSyn() && h.isAckFlag();
    }
    protected boolean isAckFor(RTPMessage msg, int seq) {
        RTPHeader h = msg.getHeader();
        return h.isAckFlag() && h.getAck() == seq;
    }
    protected boolean isFinAck(RTPMessage msg) {
        RTPHeader h = msg.getHeader();
        return h.isFin() && h.isAckFlag();
    }
    protected boolean isAck(RTPMessage msg) {
        RTPHeader h = msg.getHeader();
        return h.isAckFlag();
    }
    protected boolean isNack(RTPMessage msg) {
        RTPHeader h = msg.getHeader();
        return h.isNack();
    }
}
