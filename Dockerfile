FROM eclipse-temurin:21-jre-alpine

RUN apk upgrade --no-cache && mkdir /opt/jiraminer

COPY bin/jiraminer.sh /opt/jiraminer/
COPY target/jiraminer.jar /opt/jiraminer/jiraminer.jar

RUN chmod +x /opt/jiraminer/jiraminer.sh

WORKDIR /opt/jiraminer
ENTRYPOINT ["./jiraminer.sh"]
