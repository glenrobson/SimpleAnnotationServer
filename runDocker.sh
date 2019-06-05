#!/bin/bash

docker build --rm -t sas:latest . && docker run -v /tmp/data:/usr/src/app/data --rm -p 8888:8080 --name sas sas:latest
