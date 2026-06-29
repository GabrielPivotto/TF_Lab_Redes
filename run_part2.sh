#!/bin/bash

export MSYS_NO_PATHCONV=1

PROTOCOLS=("gbn" "sr")
WINDOWS=(4 16)

mkdir -p captures

run_test() {

    NAME=$1
    RULE=$2
    PROTOCOL=$3
    WINDOW=$4

    echo ""
    echo "===================================="
    echo "TESTE: $PROTOCOL | W=$WINDOW | $NAME"
    echo "===================================="

    # limpa configuração anterior
    docker exec host2 tc qdisc del dev eth0 root 2>/dev/null

    # aplica cenário
    if [ "$RULE" != "none" ]; then
        docker exec host2 tc qdisc add dev eth0 root netem $RULE
    fi

    PCAP="${PROTOCOL}_w${WINDOW}_${NAME}.pcap"

    # inicia captura
    docker exec -d host2 tcpdump -i eth0 udp port 5000 -w /tmp/$PCAP

    sleep 1

    # inicia receiver
    docker exec -d host1 java -cp bin host1.Receiver $PROTOCOL $WINDOW

    sleep 2

    # executa sender
    docker exec host2 java -cp bin host2.Sender $PROTOCOL $WINDOW

    sleep 2

    # encerra captura corretamente
    docker exec host2 pkill -INT tcpdump

    # checa integridade dos arquivos
    ORIG=$(docker exec host2 sha256sum /app/src/files/50.255+0.txt | cut -d' ' -f1)
    RECV=$(docker exec host1 sha256sum /app/bin/files/received.txt | cut -d' ' -f1)
    if [ "$ORIG" = "$RECV" ]; then
        echo "[OK] Arquivo íntegro"
    else
        echo "[ERRO] Arquivo corrompido"
    fi
    sleep 2

    docker cp host2:/tmp/$PCAP captures/$PCAP

    echo "Captura salva: captures/$PCAP"

    docker exec host2 tc qdisc del dev eth0 root 2>/dev/null
}

for PROTOCOL in "${PROTOCOLS[@]}"
do
    for WINDOW in "${WINDOWS[@]}"
    do

        # LATÊNCIA

        run_test  "L0" "none" "$PROTOCOL" "$WINDOW"
        run_test  "L1" "delay 50ms" "$PROTOCOL" "$WINDOW"
        run_test  "L2" "delay 100ms" "$PROTOCOL" "$WINDOW"
        run_test  "L3" "delay 150ms" "$PROTOCOL" "$WINDOW"

        # PERDA

        run_test  "P0" "none" "$PROTOCOL" "$WINDOW"
        run_test  "P1" "loss 1%" "$PROTOCOL" "$WINDOW"
        run_test  "P2" "loss 5%" "$PROTOCOL" "$WINDOW"
        run_test  "P3" "loss 10%" "$PROTOCOL" "$WINDOW"
        run_test  "P4" "loss 25%" "$PROTOCOL" "$WINDOW"

    done
done

echo ""
echo "===================================="
echo "TODOS OS TESTES FINALIZADOS"
echo "===================================="