FROM tomcat:9-jdk8

RUN rm -rf ${CATALINA_HOME}/webapps/*

COPY target/uc*.war ${CATALINA_HOME}/webapps/ROOT.war
