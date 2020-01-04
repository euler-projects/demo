#!/usr/bin/env bash

docker_context_path=
dockerfile=Dockerfile
artifact_path=target

if test -z ${docker_context_path}
then
    absolute_docker_context_path=`pwd`
    absolute_dockerfile_path=`pwd`"/${dockerfile}"
    absolute_artifact_path=`pwd`"/${artifact_path}"
else
    absolute_docker_context_path=`pwd`"/${docker_context_path}"
    absolute_dockerfile_path=`pwd`"/${dockerfile}"
    absolute_artifact_path=`pwd`"/${artifact_path}"
fi

echo "docker context path is ${absolute_docker_context_path}"
echo "dockerfile path is ${absolute_dockerfile_path}"
echo "artifact path is ${absolute_artifact_path}"

app_name=`mvn -q -N exec:exec -Dexec.executable="echo" -Dexec.args='${project.artifactId}'`
version=`mvn -q -N exec:exec -Dexec.executable="echo" -Dexec.args='${project.version}'`

image_name=eulerproject/${app_name}:${version}

args=$@;
echo "exec mvn -U clean package -Dmaven.test.skip ${args}"
mvn -U clean package -Dmaven.test.skip ${args}

if test ! -d ${absolute_artifact_path}
then
    echo "dir '${absolute_artifact_path}' not exits";
    exit 1;
fi

for file in `ls ${absolute_artifact_path}/*.war`
do
    war_file_path=${file}
done

if test -z ${war_file_path}
then
    echo "artifact not exits";
    exit 1;
fi

docker_img_build_cmd="docker build -f ${absolute_dockerfile_path} -t ${image_name} ${absolute_docker_context_path}"
docker_img_push_cmd="docker push ${image_name}"
docker_img_rm__cmd="docker rmi ${image_name}"

echo "exec ${docker_img_build_cmd}"
${docker_img_build_cmd}
echo "exec ${docker_img_push_cmd}"
${docker_img_push_cmd}
echo "exec ${docker_img_rm__cmd}"
${docker_img_rm__cmd}
