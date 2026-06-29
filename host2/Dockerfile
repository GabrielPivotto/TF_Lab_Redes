FROM eclipse-temurin:21

WORKDIR /app

COPY . .

RUN mkdir -p bin && apt-get update && apt-get install -y iproute2 tcpdump

RUN find src -name "*.java" > sources.txt && \
    javac -d bin @sources.txt

CMD ["java", "-cp", "bin", "src.App"]