FROM cfrost/tomcat:7.0.88-jre8-oracle

COPY target/demo.war /

RUN yum update -y \
    && yum install zip unzip -y \
    &&  rm -rf ${CATALINA_HOME}/webapps/* \
    && mkdir ${CATALINA_HOME}/webapps/ROOT \
    && unzip /demo.war -d ${CATALINA_HOME}/webapps/ROOT

EXPOSE 8080

ENTRYPOINT [ "run-tomcat7.sh" ]
