#!/bin/bash

ACCOUNT=evansye
TAG=${1:-`date +%s`}
SUFFIX=${1:-hadoop}

ln -s site.yaml.template.$SUFFIX site.yaml.template
rm -rf bigtop-puppet
cp -r ../../bigtop-deploy/puppet ./bigtop-puppet
docker build --force-rm --no-cache -t "$ACCOUNT/hadoocker:$TAG" .
rm -rf bigtop-puppet
