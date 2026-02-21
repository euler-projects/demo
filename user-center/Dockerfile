FROM openjdk:17.0.2

RUN mkdir /app

COPY target/*.jar /app/app.jar
COPY run.sh /app/run.sh

RUN chmod -R 755 /app

EXPOSE 8080

WORKDIR /app

ENTRYPOINT [ "./run.sh" ]

# docker buildx build --platform linux/amd64,linux/arm64 --builder container-builder -t registry.cn-shenzhen.aliyuncs.com/euler-project/user-center:1.0.0 --push .
