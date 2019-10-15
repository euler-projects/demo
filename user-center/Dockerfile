FROM cfrost/tomcat:7.0.88-jre8-oracle

ENV LANG en_US.utf8

RUN yum update -y && rm -rf ${CATALINA_HOME}/webapps/*

COPY target/uc*.war ${CATALINA_HOME}/webapps/ROOT.war
