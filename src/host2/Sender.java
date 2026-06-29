package host2;
import java.io.File;
import java.net.InetAddress;

import rtp.network.UDPClient;
import rtp.protocol.BaseSender;
import rtp.protocol.ProtocolFactory;

public class Sender {
    private static final int PORT = 5000;
    public static void main(String[] args) throws Exception {
        InetAddress receiver = InetAddress.getByName("host1");
        BaseSender sender;
        UDPClient udpS = new UDPClient(PORT);
        int window = 0;

        if(args.length == 2) {
            window = Integer.parseInt(args[1]);
            System.out.println("Window size = " + window);
        }
        System.out.println("Modo = " + args[0]);

        sender = ProtocolFactory.createSender(args[0], udpS, receiver, PORT, window);

        String str = "src/files/50.255+0.txt";
        System.out.println("Usando arquivo \"" + str + "\"");
        File file = new File(str);
        System.out.println("<Sender> Iniciando etapa de HANDSHAKE");
        sender.connect();
        System.out.println("<Sender> Lendo arquivo e enviando");

        long start = System.currentTimeMillis();
        sender.sendFile(file);
        long end = System.currentTimeMillis();

        long bytesSize = file.length();
        System.out.println("<Sender> Iniciando etapa de DISCONNECT");
        sender.disconnect();  

        double seconds = (end-start)/1000.0;
        double throughput = bytesSize/seconds;

        System.out.println("ARQUIVO="+bytesSize);
        System.out.println("TEMPO="+seconds+" s");
        System.out.println("THROUGHPUT="+throughput);
    }
}
