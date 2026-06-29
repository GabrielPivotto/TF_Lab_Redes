package rtp.model;

public class RTPPayload {
    private byte[] payload;
    private boolean theresNoPayload = false;

    /**
     * Wraper basico usado para representar o payload da mensagem, limitando o tamanho do payload a 255 bytes.
     * 
     * @param payload
     *      {@code byte[]} que contem os dados a serem enviados.
     */
    public RTPPayload(byte[] payload) {
        if(payload.length > 255) {
            throw new IllegalArgumentException("Payload maior que 255 bytes");
        }

        this.payload = payload;
    }
    
    /**
     * Cria payload vazio em casos de mensagens que não precisam dele.
     * 
     * @return
     *      Objeto {@code RTPPayload} com {@code byte[]} de tamanho 0
     */
    public static RTPPayload empty() {
        return new RTPPayload(new byte[0], true);
    }
    private RTPPayload(byte[] payload, boolean theresNoPayload) {
        this.theresNoPayload = theresNoPayload;
        
        if(payload.length > 255) {
            throw new IllegalArgumentException("Payload maior que 255 bytes");
        }

        this.payload = payload;
    }

    public boolean theresNoPayload() {return theresNoPayload;}
    public int size() {return payload.length;}
    public byte[] getPayload() {return this.payload;}
}
