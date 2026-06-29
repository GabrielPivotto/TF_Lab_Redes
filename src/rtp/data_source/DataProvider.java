package rtp.data_source;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

import rtp.model.RTPPayload;

/**
 * Responsável por enviar grupos de bytes ao payload da mensagem RTP. Usar apenas para definir o payload da classe {@code RTPPayload}.
 */
public class DataProvider {
    //private final File f;
    private final InputStream input;        //stream de dados que esta sendo lido
    private final int chunkSize;            //tam. do byte[] que recebe os dados
    private boolean emptyPayloadPending;    //detecta edge case em multiplos de 255
    private boolean hasNextChunk;

    /**
     * Responsável por pegar grupos de bytes do arquivo para envio em payload.
     * 
     * @param file
     *      Arquivo que se deseja enviar.
     * 
     * @param chunkSize
     *      Tamanho do {@code byte[]} que será devolvido em {@code getNextChunk()} (pode não preencher todo o array caso não haja bytes o suficiente)
     * 
     * @throws FileNotFoundException 
     */
    public DataProvider(File file, int chunkSize) throws FileNotFoundException {
        this.input = new FileInputStream(file);
        this.chunkSize = chunkSize;
        emptyPayloadPending = false;
        hasNextChunk = true;
    }

    /**
     * Envia próximo grupo de bytes do arquivo enviado no construtor da classe
     * 
     * @return
     *      {@code byte[]} dos dados do arquivo.
     * 
     * @throws IOException
     */
    public RTPPayload getNextChunk() throws IOException{ //esse metodo foi criada com auxilio do ChatGPT pois nao se sabia usar "FileInputStream"
        /*/
         * Length == 255 -> pacote intermediario (receiver bufferiza o pacote)
         * Length < 255  -> ultimo pacote do stream (receiver entrega o buffer completo à aplicação)
         * Length == 0   -> edge case (arquivo é multiplo exato de 255 bytes; Sinaliza fim de stream sem payload residual)
        /*/
        byte[] buffer = new byte[chunkSize];
        int bytesRead = input.read(buffer); //"read" devolve a qtd lida ou -1 caso EOF
        //System.out.println("||bytesRead = " + bytesRead + "||");
        
        //se nao tem nada mais para ler
        if(bytesRead == -1) {
            if(emptyPayloadPending) { //checa se o ultimo lido era exatamente 255 bytes (EDGE CASE)
                emptyPayloadPending = false;
                hasNextChunk = false;
                return new RTPPayload(new byte[0]); //se nao for, so termina a transmissao de dados
            }

            hasNextChunk = false;
            return null;
        }

        emptyPayloadPending = false;

        //checa se o valor lida foi EXATAMENTE 255 bytes
        if(bytesRead == chunkSize) {
            emptyPayloadPending = true;
        }

        return new RTPPayload(Arrays.copyOf(buffer, bytesRead));
    }

    /**
     * Diz se objeto {@code DataProvider} ainda tem dados à enviar.
     * 
     * @return
     *      {@code True} caso tenha dados à enviar; {@code False} caso não tenha mais dados à enviar. 
     */
    public boolean hasNextChunk() {return hasNextChunk;}
}
