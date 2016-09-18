#!/bin/bash

rm -rfv bigtop-puppet
cp -r ../../bigtop-deploy/puppet ./bigtop-puppet
docker build -t "test`date +%s`" .
rm -rfv bigtop-puppet
