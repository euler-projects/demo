#!/usr/bin/env sh

log_path="/usr/local/tomcat/logs"

GC_LOG_OPTS="-Xloggc:${log_path}/gc.log -XX:+PrintGCDetails -XX:+PrintPromotionFailure -XX:+PrintGCApplicationStoppedTime \
-XX:NumberOfGCLogFiles=2 -XX:GCLogFileSize=64M"

SNAPSHOT_OPTS="-XX:+PrintCommandLineFlags -XX:ErrorFile=${log_path}/hs_err_pid%p.err \
-XX:HeapDumpPath=${log_path} -XX:+HeapDumpOnOutOfMemoryError"

OTHER_OPTS="-XX:+UnlockExperimentalVMOptions -XX:+UseCGroupMemoryLimitForHeap -Dfile.encoding=UTF-8"

JAVA_OPTS="${JAVA_OPTS} ${EULER_JAVA_OPTS} ${GC_LOG_OPTS} ${SNAPSHOT_OPTS} ${OTHER_OPTS}"

echo "JAVA_OPTS: ${JAVA_OPTS}"

export JAVA_OPTS="${JAVA_OPTS}"

catalina.sh run
