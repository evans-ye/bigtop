#!/bin/bash

rm -rfv bigtop-deploy
cp -r ../../bigtop-deploy .
cp bigtop-deploy/puppet/hieradata/site.yaml site.yaml.template
docker build -t "test`date +%s`" .
