#!/bin/bash

rm -rfv bigtop-deploy
cp -r ../../bigtop-deploy .
docker build -t "test`date +%s`" .
