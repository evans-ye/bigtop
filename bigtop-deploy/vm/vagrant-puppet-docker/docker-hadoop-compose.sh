#!/bin/bash

# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

BIGTOP_PUPPET_DIR=../../puppet

usage() {
    echo "usage: $PROG [-C file ] args"
    echo "       -C file                                   Use alternate file for vagrantconfig.yaml"
    echo "  commands:"
    echo "       -c NUM_INSTANCES, --create=NUM_INSTANCES  Create a Docker based Bigtop Hadoop cluster"
    echo "       -p, --provision                           Deploy configuration changes"
    echo "       -s, --smoke-tests                         Run Bigtop smoke tests"
    echo "       -d, --destroy                             Destroy the cluster"
    echo "       -h, --help"
    exit 1
}

create() {
    # Startup instances
    docker-compose scale bigtop=$1
    if [ $? -ne 0 ]; then
        echo "Docker container(s) startup failed!";
	exit 1;
    fi

    # Get the headnode FQDN
    nodes=(`docker-compose ps -q`)
    hadoop_head_node=`docker inspect --format {{.Config.Hostname}}.{{.Config.Domainname}}` ${nodes[0]}

    # Fetch configurations form specificed yaml config file
    repo=$(get-yaml-config repo)
    components="[`echo $(get-yaml-config components) | sed 's/ /, /g'`]"
    jdk=$(get-yaml-config jdk)
    distro=$(get-yaml-config distro)
    enable_local_repo=$(get-yaml-config enable_local_repo)

    # Start provisioning
    auto-gen-config "$hadoop_head_node" "$repo" "$components" "$jdk"
    copy-to-instances $BIGTOP_PUPPET_DIR/hiera.yaml /etc/puppet/hiera.yaml
    bootstrap $distro $enable_local_repo
    provision
}

auto-gen-config() {
    echo "Bigtop Puppet configurations are shared between instances, and can be modified under config/hieradata"
    mkdir config 2> /dev/null
    yes | cp -vr $BIGTOP_PUPPET_DIR/hieradata config/
    cat > config/hieradata/site.yaml << EOF
bigtop::hadoop_head_node: $1
hadoop::hadoop_storage_dirs: [/data/1, /data/2]
bigtop::bigtop_repo_uri: $2
hadoop_cluster_node::cluster_components: $3
bigtop::jdk_package_name: $4
EOF
}

copy-to-instances() {
    nodes=(`docker-compose ps -q`)
    for node in ${nodes[*]}; do
        docker cp  $1 $node:$2 &
    done
    wait
}

bootstrap() {
    nodes=(`docker-compose ps -q`)
    for node in ${nodes[*]}; do
        docker exec $node bash -c "/bigtop-home/bigtop-deploy/vm/utils/setup-env-$1.sh $2" &
    done
    wait
}

provision() {
    nodes=(`docker-compose ps -q`)
    for node in ${nodes[*]}; do
        bigtop-puppet $node &
    done
    wait
}

smoke-tests() {
    nodes=(`docker-compose ps -q`)
    hadoop_head_node=${nodes:0:12}
    smoke_test_components="`echo $(get-yaml-config smoke_test_components) | sed 's/ /,/g'`"
    docker exec $hadoop_head_node bash -c "bash -x /bigtop-home/bigtop-deploy/vm/utils/smoke-tests.sh $smoke_test_components"
}

destroy() {
    docker-compose stop
    docker-compose rm -f
    rm -rvf ./config
}

bigtop-puppet() {
    docker exec $1 bash -c 'puppet apply -d --modulepath=/bigtop-home/bigtop-deploy/puppet/modules:/etc/puppet/modules /bigtop-home/bigtop-deploy/puppet/manifests/site.pp'
}

get-yaml-config() {
    RUBY_EXE=ruby
    which ruby > /dev/null 2>&1
    if [ $? -ne 0 ]; then
	# use vagrant embedded ruby on Windows
        RUBY_EXE=$(dirname $(which vagrant))/../embedded/bin/ruby
    fi
    RUBY_SCRIPT="data = YAML::load(STDIN.read); puts data['$1'];"
    cat ${vagrantyamlconf} | $RUBY_EXE -ryaml -e "$RUBY_SCRIPT" | tr -d '\r'
}

PROG=`basename $0`

if [ $# -eq 0 ]; then
    usage
fi

vagrantyamlconf="vagrantconfig.yaml"
while [ $# -gt 0 ]; do
    case "$1" in
    -c|--create)
        if [ $# -lt 2 ]; then
          echo "Create requires a number" 1>&2
          usage
        fi
        create $2
        shift 2;;
    -C|--conf)
        if [ $# -lt 2 ]; then
          echo "Alternative config file for vagrantconfig.yaml" 1>&2
          usage
        fi
	vagrantyamlconf=$2
        shift 2;;
    -p|--provision)
        provision
        shift;;
    -s|--smoke-tests)
        smoke-tests
        shift;;
    -d|--destroy)
        destroy
        shift;;
    -h|--help)
        usage
        shift;;
    *)
        echo "Unknown argument: '$1'" 1>&2
        usage;;
    esac
done
