#!/bin/bash

export MSYS_NO_PATHCONV=1

SCENARIOS=(
    "L0:none"
    "L1:delay 50ms"
    "L2:delay 100ms"
    "L3:delay 150ms"
    "P0:none"
    "P1:loss 1%"
    "P2:loss 5%"
    "P3:loss 10%"
    "P4:loss 25%"
)

mkdir -p captures

for item in "${SCENARIOS[@]}"
do
    NAME="${item%%:*}"
    RULE="${item#*:}"

    echo "=== Executando $NAME ==="

    # limpa regra anterior
    docker exec host2 tc qdisc del dev eth0 root 2>/dev/null

    # aplica nova regra
    if [ "$RULE" != "none" ]; then
        docker exec host2 tc qdisc add dev eth0 root netem $RULE
    fi

    # inicia captura
    docker exec -d host2 \
        tcpdump -i eth0 udp port 5000 \
        -w /tmp/$NAME.pcap

    # inicia receiver
    docker exec -d host1 java -cp bin host1.Receiver saw

    sleep 2

    # executa sender
    docker exec host2 \
        java -cp bin host2.Sender saw

    sleep 2

    # encerra captura
    docker exec host2 pkill tcpdump

    # checa integridade dos arquivos
    ORIG=$(docker exec host2 sha256sum /app/src/files/50.255+0.txt | cut -d' ' -f1)
    RECV=$(docker exec host1 sha256sum /app/bin/files/received.txt | cut -d' ' -f1)
    if [ "$ORIG" = "$RECV" ]; then
        echo "[OK] Arquivo íntegro"
    else
        echo "[ERRO] Arquivo corrompido"
    fi
    sleep 2

    # copia arquivo para o host
    docker cp host2:/tmp/$NAME.pcap captures/$NAME.pcap

    echo "Captura salva em captures/$NAME.pcap"
done

# remove qualquer regra restante
docker exec host2 tc qdisc del dev eth0 root 2>/dev/null