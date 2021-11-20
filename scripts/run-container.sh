#!/bin/bash

# name for container
NAME=skin_tool
# public tcp port for the rest api
PORT=42069
echo "$REDIS_URI is the uri"
# run the container
docker run -it -d --name $NAME -e REDIS_URI=$REDIS_URI -e SKIN_TOOL_PYTHON_URI=$SKIN_TOOL_PYTHON_URI -p $PORT:8080 jcedeno/skin-tool-ipfs:latest
