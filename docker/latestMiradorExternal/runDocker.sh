#!/bin/bash

docker build --rm -t sas:latest . && docker run -ti --cap-add SYS_ADMIN -v /sys/fs/cgroup:/sys/fs/cgroup:ro --rm --name sas -p 9001:80 sas:latest 


