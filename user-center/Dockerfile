FROM tomcat:9-jdk8

ARG app_conf_path=/usr/local/app/conf
ARG app_runtime_path=/usr/local/app/runtime

RUN mkdir -p ${app_conf_path} ${app_runtime_path} \
    && rm -rf ${CATALINA_HOME}/webapps/*

ENV EULER_JAVA_OPTS="-Dcore.runtimePath=${app_runtime_path} \
-Dcore.additionalConfigPath=${app_conf_path}"

VOLUME ${app_runtime_path}

COPY init.sh ${CATALINA_HOME}/bin/init.sh
COPY target/uc*.war ${CATALINA_HOME}/webapps/ROOT.war

ENTRYPOINT [ "init.sh" ]