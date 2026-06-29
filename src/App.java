import java.io.File;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import rtp.network.UDPClient;
import rtp.protocol.BaseReceiver;
import rtp.protocol.BaseSender;
import rtp.protocol.ProtocolFactory;

public class App {
    public static void main(String[] args) throws Exception {
        if(args.length < 3) {
            System.out.println("Quantidade de argumentos minimos nao atingido");
            System.out.println("listen <port> [saw|gbn|sr] <window size>");
            System.out.println("host <ip> <port> <file> [saw|gbn|sr] <window size>");
            return;
        }

        int i = 0;
        for(String s : args) {
            System.out.println("args[" + i + "] -> " + s + " ");
            i++;
        }

        if(args[0].equals("listen")) {
            System.out.println("Criando Receiver...");
            int port = 0;
            try {
                port = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                System.out.println("\nErro ao fazer parse da porta");
                return;
            }

            int windowSize = 0;
            try {
                windowSize = Integer.parseInt(args[3]);
            } catch (NumberFormatException e) {
                System.out.println("\nErro ao definir tamanho da janela");
                return;
            }

            UDPClient udpR = new UDPClient(port);

            BaseReceiver rcv = null;
            try {
                rcv = ProtocolFactory.createReceiver(args[2], udpR, port, windowSize);
            } catch (IllegalArgumentException e) {
                System.out.println("\nProtocolo invalido");
                return;
            }

            System.out.println("Iniciando receiver");
            rcv.listen();
            System.out.println("Escrevendo em received.txt");
            Files.write(Paths.get("src/received.txt"), rcv.getBuffer().toByteArray());
            if(args.length == 5 && args[4].equals("print")) {
                System.out.println(rcv.getBufferString());
            }
            if(Files.mismatch(Paths.get("src/received.txt"), Paths.get("src/files/50.255+0.txt")) < 0) {
                System.out.println("Os arquivos sao identicos");
            }
            else {
                System.out.println("Os arquivos diferem");
            }
            
            return;
        }

        if(args[0].equals("host")) {
            System.out.println("Criando Sender...");
            //host <ip> <port> <file> [saw|gbn|sr] <window size>
            String ip = args[1];

            InetAddress ipReal = null;
            try {
                ipReal = InetAddress.getByName(ip);
            } catch (UnknownHostException e) {
                System.out.println("IP em formato invalido");
                return;
            }

            int port = 0;
            try {
                port = Integer.parseInt(args[2]);
            } catch (NumberFormatException e) {
                System.out.println("\nErro ao fazer parse da porta");
                return;
            }

            int windowSize = 0;
            try {
                windowSize = Integer.parseInt(args[5]);
            } catch (NumberFormatException e) {
                System.out.println("\nErro ao definir tamanho da janela");
                return;
            }

            String path = "src/files/" + args[3];

            UDPClient udpR = new UDPClient(port);
            BaseSender snd = null;
            try {
                snd = ProtocolFactory.createSender(args[4], udpR, ipReal, port, windowSize);
            } catch (IllegalArgumentException e) {
                System.out.println("\nProtocolo invalido");
                return;
            }

            File file = new File(path);
            System.out.println("<Sender> Iniciando etapa de HANDSHAKE");
            snd.connect();
            System.out.println("<Sender> Lendo arquivo e enviando");
            snd.sendFile(file);
            System.out.println("<Sender> Iniciando etapa de DISCONNECT");
            snd.disconnect();  
        }
    
    }
}
