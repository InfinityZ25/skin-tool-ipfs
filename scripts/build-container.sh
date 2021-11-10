#!/bin/bash
docker build --build-arg JAR_FILE=build/libs/\*.jar -t jcedeno/skin-tool-ipfs:latest .
