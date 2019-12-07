#!/usr/bin/env sh

JAVA_OPTS="${JAVA_OPTS} ${EULER_JAVA_OPTS}"

echo "JAVA_OPTS: ${JAVA_OPTS}"

java ${JAVA_OPTS} -jar app.jar
