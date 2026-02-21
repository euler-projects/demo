#!/bin/bash
mvn -U clean package -D maven.test.skip=true
docker docker buildx build --platform linux/amd64,linux/arm64 --builder container-builder -t registry.cn-shenzhen.aliyuncs.com/euler-project/user-center:1.0.0 --push .
