package rtp.model;

public class RTPHeader {
    private int seq;            //num., em pacotes, de sequencia do pacote atual 
    /*
     * - Inicia 0 no primeiro pacote de dados APOS o handshake
     * - 0 a 16383 (Wrap-around deve ser tratado)
     * - Todos os pacotes de dados tem payload 255 bytes, exceto ultimo pacote 
     */

    private boolean syn;        //flag que estabelece conexao
    private boolean fin;        //flag que termina conexao
    private int ack;            //num. do ACK
    private boolean ackFlag;    //indica que campo ACK eh valido
    private boolean nack;       //negacao do ACK
    
    private int length;
    /*
     * Durante handshake (SYN/SYN + ACK) ele representa o tam. da janela proposto pelo emissor
     * Durante transf. de dados ele representa a qtd. de bytes presentes no payload
     */
    
    
    private long crc32;         //calc. sobre cabeçalho completo + payload

    /**
     * Header da mensagem RTP.
     * 
     * @param seq
     *          Sequência atual do pacote a ser enviado. Iniciar com 0 como primeiro pacote PÓS handshake
     */
    public RTPHeader(int seq, boolean syn, boolean fin, int ack, boolean ackFlag, boolean nack, int length, long crc32) {
        //funcoes setters foram usadas apenas para verificacao simples de consistencia dos dados
        setSeq(seq);
        this.syn = syn;
        this.fin = fin;
        setAck(ack);
        this.ackFlag = ackFlag;
        this.nack = nack;
        setLength(length);
        setCrc32(crc32);
    }

    public int getSeq()                     {return this.seq;}
    public void setSeq(int seq) {
        if(seq < -1 || seq > 16383) {
            throw new IllegalArgumentException("Valor de sequencia se limita entre 0 e 16383");
        }

        this.seq = seq;
    }

    public boolean isSyn()                  {return this.syn;}
    public boolean getSyn()                 {return this.syn;}
    public void setSyn(boolean syn)         {this.syn = syn;}

    public boolean isFin()                  {return this.fin;}
    public boolean getFin()                 {return this.fin;}
    public void setFin(boolean fin)         {this.fin = fin;}

    public int getAck()                     {return this.ack;}
    public void setAck(int ack)             {
         if (ack < -1 || ack > 16383) {
            throw new IllegalArgumentException("Valor de ACK se limita entre 0 e 16383");
        }

        this.ack = ack;
    }

    public boolean isAckFlag()              {return this.ackFlag;}
    public boolean getAckFlag()             {return this.ackFlag;}
    public void setAckFlag(boolean ackFlag) {this.ackFlag = ackFlag;}

    public boolean isNack()                 {return this.nack;}
    public boolean getNack()                {return this.nack;}
    public void setNack(boolean nack)       {this.nack = nack;}

    public int getLength()                  {return this.length;}
    public void setLength(int length) {
        if (length < 0 || length > 255) {
            throw new IllegalArgumentException("Valor de Length se limita entre 0 e 255 bytes");
        }
    
        this.length = length;
    }

    public long getCrc32()                  {return this.crc32;}
    public void setCrc32(long crc32) {
        if (crc32 > 0) {
            throw new IllegalArgumentException("CRC32 deve-se manter zerado no header");
        }
        
        this.crc32 = crc32;
    }

    public String toString() {
    return "RTPHeader {" +
            "seq=" + seq +
            ", syn=" + syn +
            ", fin=" + fin +
            ", ack=" + ack +
            ", ackFlag=" + ackFlag +
            ", nack=" + nack +
            ", length=" + length +
            ", crc32=" + crc32 +
            "}";
}
}
