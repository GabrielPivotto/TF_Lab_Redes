#!/bin/bash

export MSYS_NO_PATHCONV=1

#mkdir -p captures

run_test() {

    PROTOCOL=$1
    WINDOW=$2
    NAME=$3
    NETEM=$4

    echo ""
    echo "===================================="
    echo "$PROTOCOL | WINDOW=$WINDOW | $NAME"
    echo "NETEM: $NETEM"
    echo "===================================="

    # limpa netem anterior
    docker exec host2 tc qdisc del dev eth0 root 2>/dev/null

    # aplica reordenação
    if [ -n "$NETEM" ]; then
        docker exec host2 tc qdisc add dev eth0 root netem $NETEM
    fi

    echo "QDISC:"
    docker exec host2 tc qdisc show dev eth0

    PCAP="${PROTOCOL}_w${WINDOW}_${NAME}.pcap"

    # inicia captura
    docker exec -d host2 \
        tcpdump -i eth0 udp port 5000 \
        -w /tmp/$PCAP

    sleep 1

    # inicia receiver
    docker exec -d host1 \
        java -cp bin host1.Receiver $PROTOCOL $WINDOW

    sleep 2

    # executa sender
    docker exec host2 \
        java -cp bin host2.Sender $PROTOCOL $WINDOW

    sleep 2

    # encerra captura
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

    echo "Captura salva em captures/$PCAP"

    docker exec host2 tc qdisc del dev eth0 root 2>/dev/null
}

#
# SOMENTE REORDENAÇÃO
#

for PROTOCOL in gbn sr
do
    for WINDOW in 4 16
    do
        run_test "$PROTOCOL" "$WINDOW" "R0" ""

        run_test "$PROTOCOL" "$WINDOW" "R1" "delay 1ms reorder 10% 50%"
        
        run_test "$PROTOCOL" "$WINDOW" "R2" "delay 1ms reorder 25% 50%"

    done
done

echo ""
echo "===================================="
echo "TESTES DE REORDENAÇÃO FINALIZADOS"
echo "===================================="