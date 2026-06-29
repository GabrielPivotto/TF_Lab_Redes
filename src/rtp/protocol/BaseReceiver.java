package rtp.protocol;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
//import java.nio.file.Files;
//import java.nio.file.Path;

import rtp.codec.RTPDecoder;
import rtp.codec.RTPEncoder;
import rtp.model.RTPHeader;
import rtp.model.RTPMessage;
import rtp.model.RTPMessageFactory;
import rtp.network.UDPClient;

public abstract class BaseReceiver {
    protected int port;
    protected int expectedSeq;
    protected final UDPClient udp;
    protected ByteArrayOutputStream buffer;
    protected InetAddress sender;
    protected int window;

    /**
     * Classe base para RECEIVER.
     */
    public BaseReceiver(UDPClient udp, int port, int window) {
        this.udp = udp;
        this.port = port;
        this.window = window;
    }

    public abstract void listen() throws Exception;

    protected boolean isFin(RTPMessage msg) {
        return msg.getHeader().getFin() && msg.getHeader().getSeq() == expectedSeq;
    }
    protected boolean isSyn(RTPMessage msg) {
        RTPHeader h = msg.getHeader();
        return h.isSyn();
    }
    protected boolean isAck(RTPMessage msg) {
        RTPHeader h = msg.getHeader();
        return h.isAckFlag();
    }
    protected boolean isData(RTPMessage msg) {
        return !msg.getPayload().theresNoPayload();
    }

    protected void send(RTPMessage msg) throws IOException {
        byte[] packet = RTPEncoder.encode(msg);
        System.out.println("ENVIANDO="+msg);

        udp.send(packet, sender, port);
    }
    protected RTPMessage receive() throws IOException {
        byte[] packet = udp.receive();
        if(sender == null) {sender = udp.getOtherHost();}

        RTPMessage msg = RTPDecoder.decode(packet);
        if(msg == null) {
            System.out.println("<Receiver> CRC invalidado, descartando pacote...");
            System.out.println("RECEBENDO="+msg);
            System.out.println("<Receiver> RECV INVALID\n");
            return RTPMessageFactory.invalid();
        }

        System.out.println("RECEBENDO="+msg);

        return msg;
    }

    /**
     * Trata da fase de handshake da conexão
     * 
     * @throws IOException
     */
    protected void handleHandshake() throws IOException {
        //Obs.: receiver ASSUME que sender vai REINICIAR O EVENTO DE CONEXAO  
        while(true) {
            
            int newWindow = window;
            RTPMessage msg = receive(); //comeca esperando um SYN
            if(!isSyn(msg)) {continue;} //se nao for, da loop, e espera novamente

            //definindo qual eh o "window" menor
            if(msg.getHeader().getLength() < window) {
                newWindow = msg.getHeader().getLength();
            }

            System.out.println("<Receiver> RECV SYN\n");
            while(true) {
                udp.setTimeout(100);
                send(RTPMessageFactory.synAck(window)); //SYN + ACK para o SYN
                System.out.println("<Receiver> SEND SYN+ACK\n");
                
                RTPMessage ack = null;
                try {
                    ack = receive();
                } catch (SocketTimeoutException  e) {
                    System.out.println("<Receiver> ACK nao chegou");
                }

                if(ack != null && isAck(ack)) {
                    System.out.println("<Receiver> RECV ACK\n");
                    //se a conexao foi bem sucedida, atualiza send para "currPort+1"
                    port = port+1;
                
                    System.out.println("<Sender> oldWindow="+window+" | newWindow="+newWindow);
                    window = newWindow;
                    udp.setTimeout(0);
                    return;
                }
            }
        }
    }

    /**
     * Fase de termino da conexão. 
     * 
     * @throws IOException
     */
    protected void handleDisconnect() throws IOException {
        sender = null;
        System.out.println("<Receiver> Removido Sender da conexao");
        
        udp.close();
        return;
    }

    public ByteArrayOutputStream getBuffer() {return buffer;}
    public String getBufferString() {
        System.out.println("\nResultados=============================");
        return buffer.toString();
    }
}