#!/bin/bash

docker build --rm -t sas:latest . && docker run --rm -ti --tmpfs /tmp --tmpfs /run -v /sys/fs/cgroup:/sys/fs/cgroup:ro -p 9002:80 -p 8983:8983 -p 9080:8080 --privileged --name sas sas:latest
# docker run -ti --rm --name sas -p 9001:80 -e PORT=80 sas:latest 

