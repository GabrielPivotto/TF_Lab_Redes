package rtp.codec;

import java.nio.ByteBuffer;
import java.util.zip.CRC32;

import rtp.model.RTPHeader;
import rtp.model.RTPMessage;

public class RTPEncoder {

    /**
     * Codifica um objeto {@code RTPMessage} em um {@code byte[]}.
     * 
     * @param msg
     *      Objeto {@code RTPMessage} que se deseja transformar.
     * 
     * @return
     *      {@code byte[]} equivalente.
     */
    public static byte[] encode(RTPMessage msg) {
        /*/
         * - Config. para o Header da mensagem
         * Byte 0-1 (16) <SYN (1 bit) | FIN (1 bit) | SEQ (14 bits)>
         * Byte 2-3 (16) <ACKFLAG (1 bit) | NACK (1 bit) | ACK (14 bits)>
         * Byte 4   (8)  <LENGTH (8 bits)>
         * BYte 5-8 (32) <CRC32 (32 bits)>
        /*/
        
        byte[] payload;

        //checar depois se payload devolve null alguma vez
        if(msg.getPayload().getPayload() == null) {payload = new byte[0];}
        else {payload = msg.getPayload().getPayload();}

        //variavel que vai serializar o objeto
        ByteBuffer packet = ByteBuffer.allocate(9 + payload.length);

        encodeHeader(packet, msg.getHeader());
        packet.put(payload);

        //calc. do CRC32
        CRC32 crc = new CRC32();
        crc.update(packet.array());

        //pega o checksum gerado em "update"
        long crcValue = crc.getValue();

        //System.out.println("CRC calculado = " + crcValue);
        //escrita do CRC32 no cabecalho
        packet.putInt(5, (int) crcValue);
        //System.out.println("CRC escrito = " + ByteBuffer.wrap(packet.array()).getInt(5));

        return packet.array();
    }

    /**
     * Segmenta o header, e o insere em {@code byte[] b}.
     * 
     * @param b
     *      Buffer a receber o header em forma de bytes.
     * 
     * @param h
     *      Header que se deseja transformar em uma sequência de bytes. 
     */
    private static void encodeHeader(ByteBuffer b, RTPHeader h) { //metodo criado com auxilio do ChatGPT pois nao se sabia como separar a mensagem em bytes
        //Byte 0-1 ===============================
        int first16 = 0;
        if(h.isSyn()) {first16 |= (1 << 15);}
        if(h.isFin()) {first16 |= (1 << 14);}
        first16 |= (h.getSeq() & 0x3FFF);
        b.putShort((short)first16);
        
        //Byte 2-3 ===============================
        int second16 = 0;
        if(h.isAckFlag()) {second16 |=(1 << 15);}
        if(h.isNack()) {second16 |=(1 << 14);}
        second16 |= (h.getAck() & 0x3FFF);
        b.putShort((short)second16);
        
        //Byte 4-8 ===============================
        b.put((byte)h.getLength());
        b.putInt(0);
    }
}
