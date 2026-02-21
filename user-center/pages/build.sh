#!/usr/bin/env bash

npm run build

rm -rf ../src/main/resources/static/*
rm -rf ../src/main/resources/templates/admin/*

cp -rp ./dist/assets ../src/main/resources/static
cp -p ./dist/index.html ../src/main/resources/templates/admin