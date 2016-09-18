#!/bin/bash

for service in `ls /etc/init.d`; do
    service $service stop
done
