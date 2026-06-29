package rtp.network;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.Arrays;

public class UDPClient {
    private DatagramSocket socket;
    private InetAddress otherHost;

    /**
     * Responsável por conectar hosts e gerenciar a comunicação entre eles
     * @param port
     *      Porta definida na qual o host local irá usar para se comunicar com o host remoto.
     * 
     * @throws SocketException
     */
    public UDPClient(int port) throws SocketException {
        socket = new DatagramSocket(port);
    }

    /**
     * Método que envia um packet UDP através do socket definido no construtor.
     * 
     * @param msg
     *      Mensagem em {@code byte[]} a ser enviada.
     * 
     * @param host
     *      Host remoto que se deseja enviar a mensagem.
     * 
     * @param port
     *      Porta do host remoto.
     * 
     * @throws IOException
     */
    public void send(byte[] msg, InetAddress host, int port) throws IOException {
        DatagramPacket packet = new DatagramPacket(msg, msg.length, host, port);

        socket.send(packet);
    }

    /**
     * Envio de arquivos usando {@code InetAddress otherHost} obtido em {@code receive()}.
     * @param msg
     * @param port
     * @throws IOException
     */
    public void send(byte[] msg, int port) throws IOException {
        DatagramPacket packet = new DatagramPacket(msg, msg.length, otherHost, port);

        socket.send(packet);
    }

    /**
     * Método que espera por um packet UDP através do socket definido. Seu tempo de timeout pode ser definido na função {@code setTimeout(int ms)}
     * 
     * @return
     *      {@code byte[]} da mensagem enviada.
     * 
     * @throws IOException
     */
    public byte[] receive() throws IOException {
        byte[] buffer = new byte[264];

        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

        socket.receive(packet);
        otherHost = packet.getAddress();

        byte[] packetArray = Arrays.copyOf(packet.getData(), packet.getLength());

        return packetArray;
    }

    /**
     * Define, em milessegundos, o tempo máximo de espera para recebimento de um packet UDP.
     * 
     * @param ms
     *      Tempo de espera para timeout.
     * 
     * @throws SocketException
     */
    public void setTimeout(int ms) throws SocketException {
        socket.setSoTimeout(ms);
    }

    /**
     * Método que termina a comunicação, fechando o socket do host local. Usar apenas em caso de FIN + ACK.
     */
    public void close() {socket.close();}

    public InetAddress getOtherHost() {return otherHost;}
}
