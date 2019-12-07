FROM openjdk:8-jdk

ARG app_name=euler-boot-demo
ARG app_port=8080
ARG app_path=/app/${app_name}
ARG app_runtime_path=${app_path}/runtime
ARG app_conf_path=${app_runtime_path}/conf
ARG app_log_path=${app_runtime_path}/log

ENV EULER_JAVA_OPTS="-Dlogging.path=${app_log_path} \
-Dserver.port=${app_port} \
-Dspring.application.name=${app_name} \
-Dspring.config.additional-location=file:${app_conf_path}/ \
-Dyunxiao.application.runtime-path=${app_runtime_path}"

EXPOSE ${app_port}

WORKDIR ${app_path}

VOLUME ${app_runtime_path}

COPY init.sh ${app_path}/init.sh

RUN chmod 755 ${app_path}/init.sh

COPY target/*.jar ${app_path}/app.jar

ENTRYPOINT [ "./init.sh" ]
