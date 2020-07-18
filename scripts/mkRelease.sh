#!/bin/bash

mvn clean package

cwd=`pwd`

build_dir=/tmp/sas-release/sas

if [ -d "$build_dir" ]; then
    echo "Removing previous files from $build_dir"
    rm -rf $build_dir/*
else
    echo "Creating $build_dir"
    mkdir -p $build_dir
fi    

cd $build_dir

echo "Copying files"
cp -r $cwd/target/dependency $cwd/target/simpleAnnotationStore.war .

cd ..
zip_dir=`pwd`

echo "Building zip"
zip -r sas.zip sas

echo "Release zip file in: $zip_dir/sas.zip"
