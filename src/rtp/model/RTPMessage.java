package rtp.model;

/**
 * Representação da mensagem RTP em classe alto nivel. Para enviá-la por rede, é necessário utilizar da classe {@code Encoder}.
 */
public class RTPMessage {
    RTPHeader head;     //contem todas as informacoes da mensagem RTP
    RTPPayload payload; //contem os dados a serem enviados na mensagem RTP (pode ou nao existir)

    /**
     * Construtor para mensagens de envio de dados com RTP.
     * 
     * @param head
     *      O header da mensagem RTP.
     * 
     * @param payload
     *      Dado a ser enviado na mensagem.
     */
    public RTPMessage(RTPHeader head, RTPPayload payload) {
        this.head = head;
        this.payload = payload;
    }

    /**
     * Construtor para mensagens RTP que não exigem payload.
     * @param head
     *      O header da mensagem RTP.
     */
    public RTPMessage(RTPHeader head) {
        this.head = head;
        this.payload = RTPPayload.empty();
    }

    public RTPHeader getHeader()      {return this.head;}
    public RTPPayload getPayload()   {return this.payload;}

    @Override
    public String toString() {
    return "RTPMessage {" +
            "\n  " + this.head +
            "\n  payloadSize=" +
            (payload == null ? 0 : payload.getPayload().length) +
            "\n}";
}
}
