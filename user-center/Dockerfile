FROM eclipse-temurin:25.0.2_10-jdk

RUN mkdir /app

COPY target/*.jar /app/app.jar
COPY run.sh /app/run.sh

RUN chmod -R 755 /app

EXPOSE 8080

WORKDIR /app

ENTRYPOINT [ "./run.sh" ]
