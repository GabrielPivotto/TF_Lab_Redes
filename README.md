# Projeto TF-LabRedes
Este projeto visa implementar o protocolo RTP (Reliable Transport Protocol) para transferencia de arquivos, criado para o Trabalho Final. Nele é implementado dois tipos de host:
- Receiver: host "servidor" que recebe dados do Sender e ao final da comunicação, escreve todo o conteudo em um arquivo texto;
- Sender: host "cliente" que recebe como argumento um arquivo, e envia-o para o Receiver;
A codificação e decodificação das mensagens são feitas no projeto, e cada mensagem é enviada via UDP. Os protocolos de comunicação implementado nele são: Stop-And-Wait, Go-Back-N e Selective Repeat. Ao final do envio, o Receiver checa se o arquivo recebido é, ou não, o mesmo que o original.

## Indices
- [Execução do Ambiente](#execução-do-ambiente)
    - [Local](#ambiente-local-executar-somente-sender-ou-receiver)
        - [Compilação em Windows (CMD)](#em-windows-cmd)
        - [Compilação em Linux](#em-linux)
        - [Execução do Projeto](#executando)
    - [Docker](#em-ambiente-docker-executar-ambos-sender-e-receiver)
- [Outros](#misc)
- [Componentes do Projeto](#overview-da-estrutura-do-projeto)


# Execução do Ambiente

## Ambiente Local (executar somente Sender ou Receiver)

### Em Windows (CMD)

1. Compilando

Na raiz do projeto execute:

```dir /s /b src\*.java > sources.txt``` \
```javac -d out @sources.txt```

### Em Linux

1. Compilando

Na raiz do projeto executa:

```javac -d bin $(find src -name "*.java")```

### Executando

Para ambos, a execução se mantem o mesmo:

2. Execução do Receiver 

Em um terminal CMD, execute:

```java -cp out App listen <port> [saw|gbn|sr] <window size>```

Exemplos:

```java -cp out App listen 5000 saw 1``` \
```java -cp out App listen 5000 gbn 4``` \
```java -cp out App listen 5000 sr 16```

3. Execução do Sender

Em um terminal CMD, execute:

```java -cp out App host <ip> <port> <file> [saw|gbn|sr] <window size>```

Exemplos:

```java -cp out App host 192.168.200.1 5000 received.txt saw 1``` \
```java -cp out App host 192.172.200.155 5000 texto.md gbn 10``` \
```java -cp out App host 10.45.200.4 5000 lista.txt sr 4```

## Em ambiente Docker (executar ambos Sender e Receiver)

1. Inicialização dos containers

Na raiz do projeto execute:

```docker compose up --build -d```

Este comando contrói as imagens Docker e inicia os container `host1` e `host2`.

2. Execução do Receiver 

Em um terminal, execute:

```docker exec -it host1 java -cp bin host1.Receiver [saw|gbn|sr]```

Exemplos:

```docker exec -it host1 java -cp bin host1.Receiver saw``` \
```docker exec -it host1 java -cp bin host1.Receiver gbn``` \
```docker exec -it host1 java -cp bin host1.Receiver sr```

3. Execução do Sender 

Em outro terminal, execute:

docker exec -it host2 java -cp bin host2.Sender [saw|gbn|sr]

Exemplos:

```docker exec -it host2 java -cp bin host2.Sender saw``` \
```docker exec -it host2 java -cp bin host2.Sender gbn``` \
```docker exec -it host2 java -cp bin host2.Sender sr```

O protocolo informado ao Sender e ao Receiver deve ser o mesmo.

4. Encerramento \
Após concluir os testes, os containers podem ser encerrados com:

```docker compose down```

# Misc

## Arquivos "run"

Os arquivos `.sh` contidos na pasta raiz do projeto são programas feitos para fazer os testes em rede usando tc e wireshark. Ao rodar qualquer um usando um terminal bash, o mesmo irá rodar ambos hosts, e capturar as saidas wireshark deles. `run_test.sh` roda todos os casos L e P para Stop-And-Wait. `run_part2.sh` roda todos os casos L e P para Go-Back-N e Selective Repeat para ambos tamanhos de janelas (4 e 16). `run_Rtests.sh` roda todos os casos R para Go-Back-N e Selective Repeat para ambos tamanhos de janelas (4 e 16). Para rodar os arquivos basta escrever `./<nome do teste>` que o mesmo fará todo o resto automaticamente. Além disso, todos os testes calculam o throughput e tempo de execução dos programas.

## Pasta "capture"
Nesta pasta está contido todas as capturas feitas pelo wireshark definida no trabalho.

# *Overview* da estrutura do projeto

Esta seção dará uma breve explicação sobre todas as classes criadas neste projeto, a ordem das classes se da pela quantidade de dependências.
Todo o código está comentado como também seus métodos caso for visitá-los.

## Pasta "model"

Esta pasta tem todos os elementos básicos necessários para o envio das mensagens, contendo nele todas as classes "atômicas".
- RTPHeader: representa o cabeçalho da mensagem RTP. Apenas encapsula os valores do campo header definido;
- RTPPayload: representa o payload da mensagem RTP;
- RTPMessage: representa a mensagem RTP por inteiro, encapsulando RTPHeader e RPTPayload;
- RTPMessageFactory: classe usada para facilmente criar o objeto RTPMessage de forma pré-definida, evitando inconsistências.

## Pasta "codec"

O conteúdo desta pasta se resume às classes responsáveis por codificar os objetos RTPMessage em uma sequencia de byte, e decodificar estas sequencias para o objeto.
- RTPEncoder: serializa o objeto RTPMessage e calcula seu CRC32;
- RTPDecoder: recebe sequencias de byte pelo socket UDP, valida seu CRC32, e transforma-o em objeto RTPMessage. 

## Pasta "network"

Pasta usada para classes que efetivamente interagem com a rede.
- UDPClient: responsável pela conexão, envio e recebimento das mensagens RTP entre Receiver e Sender. Usa RTPEncoder e RTPDecoder para adequar os dados ao meio.

## Pasta "data_source"

Pasta usada para armazenar quaisquer classes que interagem diretamente com o arquivo que será enviado ao payload.
- DataProvider: responsável por serializar os dados do arquivo a ser enviado pelo Sender.

## Pasta "protocol"

Nesta pasta está todos os tipos de comunicação exigidas no trabalho. Dentro está as classes bases, e pastas que mantêm a implementação dos tipos.
- BaseReceiver: classe base para todos os Receivers implementados. Nela está definido como responder a uma requisição de handshake, envio e recebimento de mensagens, e como responder à terminação de uma comunicação;
- BaseSender: classe base para todos os Senders implementados. Nela está definido como fazer o handshake, envio e recebimento de mensagens, como também terminação da comunicação;
- ProtocolFactory: fabrica de criação dos protocolos, usando BaseReceiver e BaseSender como padrão para gerar os demais objetos.

## Pasta "protocol/stop_and_wait"

Pasta que armazena a implementação da comunicação por *Stop-and-Wait*.
- StopAndWaitReceiver: extensão da classe BaseReceiver. Ao receber mensagens, o mesmo sempre envia um ACK, descartando silenciosamente em caso de pacotes com sequência incorreta;
- StopAndWaitSender: extensão da classe BaseSender. Envia mensagens de forma bloqueante, esperando o ACK de mesma sequência para enviar o próximo pacote.

## Pasta "protocol/go_back_n"

Pasta que armazena a implementação da comunicação por *Go-Back-N*.
- GoBackNReceiver: extensão da classe BaseReceiver. Similar à implementação do StopAndWaitReceiver, porém envia NACKs em caso de pacotes incorretos;
- GoBackNSender: extensão da classe BaseSender. Similar à implementação do StopAndWaitSender, porém é efetivamente usado a variável "window" na qual o Sender envia "window" pacotes sem ACK para a rede. Além disso, o mesmo retransmite TODOS os pacotes ainda não confirmados em caso de NACK.

## Pasta "protocol/selective_repeat"

Pasta que armazena a implementação da comunicação por *Selective Repeat*.
- SelectiveRepeatReceiver: extensão da classe BaseReceiver. Recebe pacotes e envia ACKs para cada pacote individualmente;
- SelectiveRepeatSender: extensão da classe BaseSender. Envia n pacotes (onde n = window) e para cada pacote, mantem um timer de timeout para retransmissão individual de cada pacote.