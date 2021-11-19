#!/bin/bash
./gradlew clean build
docker build -t jcedeno/skin-tool-ipfs:latest .
