#!/bin/bash

# name for container
NAME=skin_tool
# public tcp port for the rest api
PORT=42069

# run the container
docker run -it -d --name $NAME -p $PORT:8080 jcedeno/skin-tool-ipfs:latest
