package rtp.model;

/**
 * Classe responsavel por criar os tipos de {@code RTPMessages}. <p>
 * Exemplo:
 * - Mensagem RTP para ACK
 * - Mensagem RTP para SYN + ACK
 * - Mensagem RTP para FIN
 */
public class RTPMessageFactory {

    /**
     * Cria mensagem RTP para envio de payload (intermediario ou final). 
     * <p>
     * 
     * @param seq
     *      Identificação da sequência do pacote a ser enviado.
     * 
     * @param data
     *      Payload que será enviado na mensagem.
     * 
     * @return
     *      {@code RTPMessage} com os parâmetros já definidos e payload aplicado.
     */
    public static RTPMessage data(int seq, RTPPayload data) {
        RTPHeader h = new RTPHeader(
            seq,
            false,
            false,
            0,
            false,
            false,
            data.size(),
            0
        );

        return new RTPMessage(h, data);
    }

    /**
     * Cria mensagem de requisição de estabelecimento da conexão entre hosts.
     * 
     * @param window
     *      Quantos pacotes se deseja enviar sem confirmação ACK.
     * 
     * @return
     *      {@code RTPMessage} com flag SYN ativada, e window definida.
     */
    public static RTPMessage syn(int window) {
        RTPHeader h = new RTPHeader(
            0,
            true,
            false,
            0,
            false,
            false,
            window,
            0
        );

        return new RTPMessage(h);
    }

    /**
     * Cria mensagem de confirmação do estabelecimento da conexão entre hosts.
     * 
     * @param window
     *      Qtd de pacotes que podem ser transmitidos sem ACK.
     * 
     * @return
     *      {@code RTPMessage} com flag SYN e ACK ativada, e window definida.
     */
    public static RTPMessage synAck(int window) {
        RTPHeader h = new RTPHeader(
            0,
            true,
            false,
            0,
            true,
            false,
            window,
            0
        );

        return new RTPMessage(h);
    }

    /**
     * Cria mensagem de confirmação do recebimento de um pacote.
     * 
     * @param ackNumber
     *      Identificação do pacote reconhecido.
     * 
     * @return
     *      {@code RTPMessage} com flag ACK ativada e num. de ACK definido.
     */
    public static RTPMessage ack(int ackNumber) {
        RTPHeader h = new RTPHeader(
            0,
            false,
            false,
            ackNumber,
            true,
            false,
            0,
            0
        );

        return new RTPMessage(h);
    }

    /**
     * Cria mensagem de negação de pacote.
     * 
     * @param lastAckNumber
     *      Identificação do ultimo pacote reconhecido.
     * 
     * @return
     *      {@code RTPMessage} com flag NACK ativada.
     */
    public static RTPMessage nack(int lastAckNumber) {
        RTPHeader h = new RTPHeader(
            0,
            false,
            false,
            lastAckNumber,
            true,
            true,
            0,
            0
        );

        return new RTPMessage(h);
    }

    /**
     * Cria mensagem de encerramento da conexão entre os hosts.
     * 
     * @return
     *      {@code RTPMessage} com flag FIN ativada.
     */
    public static RTPMessage fin(int seq) {
        RTPHeader h = new RTPHeader(
            seq,
            false,
            true,
            0,
            false,
            false,
            0,
            0
        );

        return new RTPMessage(h);
    }

    /**
     * Cria mensagem de confirmação do encerramento da conexão entre os hosts.
     * 
     * @return
     *      {@code RTPMessage} com flag FIN e ACK ativada.
     */
    public static RTPMessage finAck() {
        RTPHeader h = new RTPHeader(
            0,
            false,
            true,
            0,
            true,
            false,
            0,
            0
        );

        return new RTPMessage(h);
    }

    public static RTPMessage invalid() {
        RTPHeader h = new RTPHeader(-1, false, false, -1, false, false, 0, 0);

        return new RTPMessage(h);
    }
}
