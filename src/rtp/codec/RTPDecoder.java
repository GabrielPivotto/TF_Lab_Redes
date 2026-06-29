package rtp.codec;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.zip.CRC32;

import rtp.model.RTPHeader;
import rtp.model.RTPMessage;
import rtp.model.RTPPayload;

public class RTPDecoder {
    private static int corruptCrc32 = 0;
    /**
     * Decodifica um {@code byte[]} e transforma-o em um objeto {@code RTPMessage}.
     * 
     * @param rtpMessage
     *      {@code byte[]} que se deseja transformar.
     * 
     * @return
     *      {@code RTPMessage} equivalente.
     */
    public static RTPMessage decode(byte[] rtpMessage) { //metodo criado com auxilio do ChatGPT pois nao se sabia como separar a mensagem em bytes
        ByteBuffer buffer = ByteBuffer.wrap(rtpMessage);

        //segmentando os setores do buffer ================
        int first16 = buffer.getShort() & 0xFFFF;   //pega 16 bits do buffer
        int second16 = buffer.getShort() & 0xFFFF;  //pega 16 bits do buffer
        int length = buffer.get() & 0xFF;           //pega 8 bits do buffer
        long crc = buffer.getInt() & 0xFFFFFFFFL;   //pega 4 bytes e preserva eles em um long

        //desempacotamento do header ======================
        boolean syn = (first16 & (1 << 15)) != 0; 
        boolean fin = (first16 & (1 << 14)) != 0;
        int seq = first16 & 0x3FFF;

        boolean ackFlag = (second16 & (1 << 15)) != 0;
        boolean nack = (second16 & (1 << 14)) != 0;
        int ack =  second16 & 0x3FFF;

        //desempacotamento do payload =====================
        byte[] payload = Arrays.copyOfRange(rtpMessage, 9, 9+length);
        
        //checagem de consistencia do CRC
        if(!verifyCRC(rtpMessage, crc)) {
            System.out.println("CRC difere");
            return null;
        }

        RTPHeader h = new RTPHeader(seq, syn, fin, ack, ackFlag, nack, length, 0);
        RTPPayload p = new RTPPayload(payload);
        return new RTPMessage(h, p);
    }

    /**
     * Verifica o checksum da mensagem enviada, neste caso, CRC32.
     * 
     * @param rtpMessage
     *      Mensagem a ser verificada.
     * 
     * @param receivedCRC
     *      Checksum contido na mensagem.
     *      
     * @return
     *      Resultado booleando da igualdade entre o CRC calculado a partir de {@code rtpMessage} e do contido no {@code rtpMessage} ({@code receivedCRC}).
     */
    private static boolean verifyCRC(byte[] rtpMessage, long receivedCRC) {
        byte[] copy = Arrays.copyOf(rtpMessage, rtpMessage.length);
    
        //zera o campo do checksum
        for(int i = 5; i < 9; i++) {
            copy[i] = 0;
        }
    
        CRC32 crc = new CRC32();
    
        //calcula por conta propria o CRC32 da mensagem
        crc.update(copy);
        long calculatedCRC = crc.getValue();

        //System.out.println("receivedCRC = " + receivedCRC);
        //System.out.println("calcCRC = " + calculatedCRC);
        
        if(corruptCrc32 == 10) {
            corruptCrc32++;
            return (calculatedCRC+1) == receivedCRC;
        }

        corruptCrc32++;
        return calculatedCRC == receivedCRC;
    }
}
