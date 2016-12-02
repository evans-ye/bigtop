#!/bin/bash

init() {
    echo "`facter ipaddress` `facter fqdn`" >> /etc/hosts
    cp /etc/puppet/hieradata/site.yaml.template /etc/puppet/hieradata/site.yaml
    sed -i -e "s/head.node.fqdn/`facter fqdn`/g" /etc/puppet/hieradata/site.yaml
    puppet apply --modulepath=/bigtop-puppet/modules:/etc/puppet/modules /bigtop-puppet/manifests/site.pp
}

usage() {
    echo "usage: $PROG args"
    echo "       -f, --foreground                         Running foreground."
    echo "       -b, --build                              Bootstrap the stack."
    exit 1
}

while [ $# -gt 0 ]; do
    case "$1" in
    -f|--foreground)
        init
        sleep infinity
        shift;;
    -b|--bootstrap)
        init
	shift;;
    *)
        echo "Unknown argument: '$1'" 1>&2
        usage;;
    esac
done
