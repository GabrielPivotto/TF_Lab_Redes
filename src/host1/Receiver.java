package host1;

import java.nio.file.Files;
import java.nio.file.Path;

import rtp.network.UDPClient;
import rtp.protocol.BaseReceiver;
import rtp.protocol.ProtocolFactory;

public class Receiver {
    private static final int PORT = 5000;
    public static void main(String[] args) throws Exception {
        BaseReceiver receiver;
        UDPClient udpR = new UDPClient(PORT);
        int window = 0;

        if(args.length == 2) {
            window = Integer.parseInt(args[1]);
            System.out.println("Window size = " + window);
        }

        receiver = ProtocolFactory.createReceiver(args[0], udpR, PORT, window);

        receiver.listen();
        
        System.out.println("Escrevendo em received.txt");
        Files.write(Path.of("received.txt"), receiver.getBuffer().toByteArray());
    }
}