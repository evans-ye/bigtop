#!/bin/bash

init() {
    cp /etc/puppet/hieradata/site.yaml.template /etc/puppet/hieradata/site.yaml
    sed -i -e 's/head.node.fqdn/`hostname`/g' /etc/puppet/hieradata/site.yaml
    puppet apply -d --modulepath=/bigtop-home/bigtop-deploy/puppet/modules:/etc/puppet/modules /bigtop-home/bigtop-deploy/puppet/manifests/site.pp
}

usage() {
    echo "usage: $PROG args"
    echo "       -f, --foreground                                   Running foreground."
    echo "       -i, --init                                         Bootstrap the stack."
    exit 1
}

while [ $# -gt 0 ]; do
    case "$1" in
    -f|--foreground)
        sleep infinity
        shift 1;;
    -i|--init)
        init
	shift 1;;
    *)
        echo "Unknown argument: '$1'" 1>&2
        usage;;
    esac
done
