#!/bin/bash

http_code=100
while [ "$http_code" != "200" ]
do 
    sleep 5
    http_code=`curl --write-out %{http_code} --silent --output /dev/null "$1"`
    echo "$http_code";
done


