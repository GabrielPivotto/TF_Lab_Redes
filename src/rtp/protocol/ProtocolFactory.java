package rtp.protocol;

import java.net.InetAddress;
import java.net.SocketException;

import rtp.network.UDPClient;
import rtp.protocol.go_back_n.*;
import rtp.protocol.selective_repeat.*;
import rtp.protocol.stop_and_wait.*;

/**
 * Cria de forma padronizada, os protocolos escolhidos
 */
public class ProtocolFactory {
    /**
     * Cria os 3 tipos de sender usando {@code mode} 
     * - StopAndWait = "saw"
     * - GoBackN = "gbn"
     * - SelectiveRepeat = "sr"
     *  
     * @param mode
     *      Tipo de sender.
     * 
     * @param udp
     *      Socket UDP para comunicar.
     * 
     * @param receiver
     *      
     * 
     * @param port
     * @param window
     * @return
     * @throws SocketException
     */
    public static BaseSender createSender(String mode, UDPClient udp, InetAddress receiver, int port, int window) throws SocketException {
        switch(mode) {
            case "saw":
                return new StopAndWaitSender(udp, receiver, port, 1);

            case "gbn":
                return new GoBackNSender(udp, receiver, port, window);

            case "sr":
                return new SelectiveRepeatSender(udp, receiver, port, window);

            default:
                throw new IllegalArgumentException("Tipo \"" + mode + "\" nao identificado.");
        }
    }

    public static BaseReceiver createReceiver(String mode, UDPClient udp, int port, int window) throws SocketException {
        switch(mode) {
            case "saw":
                return new StopAndWaitReceiver(udp, port, 1);

            case "gbn":
                return new GoBackNReceiver(udp, port, window);

            case "sr":
                return new SelectiveRepeatReceiver(udp, port, window);

            default:
                throw new IllegalArgumentException("Tipo \"" + mode + "\" nao identificado.");
        }
    }
}
