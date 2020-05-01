FROM openjdk:8


RUN mkdir /opt/jiraminer

ARG JAR_FILE

ADD bin/jiraminer.sh /opt/jiraminer
ADD target/${JAR_FILE} /opt/jiraminer/jiraminer.jar

RUN chmod +x /opt/jiraminer/jiraminer.sh

WORKDIR /opt/jiraminer