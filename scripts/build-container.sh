#!/bin/bash
./gradlew clean build
docker build --build-arg JAR_FILE=build/libs/\*.jar -t jcedeno/skin-tool-ipfs:latest .
