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

usage() {
    echo "usage: $PROG [-C file ] args"
    echo "       -C file                                   Use alternate file for config.yaml"
    echo "  commands:"
    echo "       -c NUM_INSTANCES, --create=NUM_INSTANCES  Create a Docker based Bigtop Hadoop cluster"
    echo "       -d, --destroy                             Destroy the cluster"
    echo "       -e, --exec INSTANCE_NO|INSTANCE_NAME      Execute command on a specific instance. Instance can be specified by name or number."
    echo "                                                 For example: $PROG --exec 1 bash"
    echo "                                                              $PROG --exec docker_bigtop_1 bash"
    echo "       -E, --env-check                           Check whether required tools has been installed"
    echo "       -l, --list                                List out container status for the cluster"
    echo "       -w, --swarm                               Create a swarm cluster to run docker containers on top of it"
    echo "       -p, --provision                           Deploy configuration changes"
    echo "       -s, --smoke-tests                         Run Bigtop smoke tests"
    echo "       -h, --help"
    exit 1
}

create() {
    if [ -e .provision_id ]; then
        echo "Cluster already exist! Run ./$PROG -d to destroy the cluster or delete .provision_id file and containers manually."
        exit 1;
    fi
    echo "`date +'%Y%m%d%H%M%S'`rdm$RANDOM" > .provision_id
    PROVISION_ID=`cat .provision_id`
    # Create a shared /etc/hosts and hiera.yaml that will be both mounted to each container soon
    mkdir -p config/hieradata 2> /dev/null
    cat /dev/null > ./config/hiera.yaml
    cat /dev/null > ./config/hosts

    # Startup instances
    docker-compose -p $PROVISION_ID scale $COMPOSE_INSTANCE=$1
    if [ $? -ne 0 ]; then
        echo "Docker container(s) startup failed!";
        exit 1;
    fi
    NODES=(`docker-compose -p $PROVISION_ID ps -q`)

    # Get the headnode FQDN
    hadoop_head_node=`docker inspect --format {{.Config.Hostname}}.{{.Config.Domainname}} ${NODES[0]}`
    # Fetch configurations form specificed yaml config file
    repo=$(get-yaml-config repo)
    components="[`echo $(get-yaml-config components) | sed 's/ /, /g'`]"
    jdk=$(get-yaml-config jdk)
    generate-config "$hadoop_head_node" "$repo" "$components" "$jdk"
    prepare-nodes
    provision
    echo "Create complete."
}

generate-hosts() {
    if [ -e .swarm_enabled ]; then
        for node in ${NODES[*]}; do
            docker exec $node bash -c "cat /dev/null > /etc/hosts"
        done
        for node in ${NODES[*]}; do
            entry=`docker inspect --format "{{.NetworkSettings.Networks.$OVERLAY_NETWORK.IPAddress}} {{.Config.Hostname}}.{{.Config.Domainname}}" $node`
            for inode in ${NODES[*]}; do
                docker exec $inode bash -c "echo $entry >> /etc/hosts"
            done
        done
    else 
        for node in ${NODES[*]}; do
            entry=`docker inspect --format "{{.NetworkSettings.IPAddress}} {{.Config.Hostname}}.{{.Config.Domainname}}" $node`
            docker exec ${NODES[0]} bash -c "echo $entry >> /etc/hosts"
        done
    fi
    wait
}

generate-config() {
    echo "Bigtop Puppet configurations are shared between instances, and can be modified under config/hieradata"
    cat $BIGTOP_PUPPET_DIR/hiera.yaml > ./config/hiera.yaml
    yes | cp -vr $BIGTOP_PUPPET_DIR/hieradata ./config/
    cat > ./config/hieradata/site.yaml << EOF
bigtop::hadoop_head_node: $1
hadoop::hadoop_storage_dirs: [/data/1, /data/2]
bigtop::bigtop_repo_uri: $2
hadoop_cluster_node::cluster_components: $3
bigtop::jdk_package_name: $4
EOF
}

prepare-nodes() {
    generate-hosts
    if [ -e .swarm_enabled ]; then
        for inode in ${NODES[*]}; do
            docker exec $inode bash -c "mkdir /bigtop-home"
        done
        copy-to-instances ../../bigtop-deploy /bigtop-home/bigtop-deploy
        copy-to-instances ../../bigtop_toolchain /bigtop-home/bigtop_toolchain
        copy-to-instances ./config/hiera.yaml /etc/puppet/hiera.yaml
        copy-to-instances ./config/hieradata /etc/puppet/hieradata
    fi
    distro=$(get-yaml-config distro)
    enable_local_repo=$(get-yaml-config enable_local_repo)
    bootstrap $distro $enable_local_repo
}

copy-to-instances() {
    for node in ${NODES[*]}; do
        docker cp  $1 $node:$2 &
    done
    wait
}

bootstrap() {
    for node in ${NODES[*]}; do
        docker cp  $1 $node:$2 &
        docker exec $node bash -c "/bigtop-home/bigtop-deploy/vm/utils/setup-env-$1.sh $2" &
    done
    wait
}

provision() {
    for node in ${NODES[*]}; do
        bigtop-puppet $node &
    done
    wait
}

scale() {
    docker-compose -p $PROVISION_ID scale $COMPOSE_INSTANCE=$1
    NODES=(`docker-compose -p $PROVISION_ID ps -q`)
    prepare-nodes
    provision
}

swarm() {
    kvstore
    swarm-master
    swarm-slave
    touch .swarm_enabled
    eval $(docker-machine env --swarm swarm-master)
    docker network create --driver overlay $OVERLAY_NETWORK
    echo "Swarm environment setup complete."
}

kvstore() {
    echo "Creating kvstore node..."
    DRIVER='virtualbox'
    DRIVER_OPTS='--virtualbox-memory 256'
    #docker-machine create -d $DRIVER $DRIVER_OPTS kvstore 
    docker-machine create -d $DRIVER kvstore 
    eval $(docker-machine env kvstore)
    docker run -d -p 8500:8500 --name=consul progrium/consul -server -bootstrap
}

swarm-master() {
    echo "Creating swarm-master node..."
    DRIVER='virtualbox'
    DRIVER_OPTS='--virtualbox-memory 2048'
    docker-machine create -d $DRIVER $DRIVER_OPTS --swarm --swarm-master --swarm-discovery="consul://$(docker-machine ip kvstore):8500" --engine-opt="cluster-store=consul://$(docker-machine ip kvstore):8500" --engine-opt="cluster-advertise=eth1:2376" swarm-master
}

swarm-slave() {
    echo "Creating swarm-slave node..."
    DRIVER='virtualbox'
    DRIVER_OPTS='--virtualbox-memory 2048'
    docker-machine create -d $DRIVER $DRIVER_OPTS --swarm --swarm-discovery="consul://$(docker-machine ip kvstore):8500" --engine-opt="cluster-store=consul://$(docker-machine ip kvstore):8500" --engine-opt="cluster-advertise=eth1:2376" swarm-slave
}

smoke-tests() {
    hadoop_head_node=${NODES:0:12}
    smoke_test_components="`echo $(get-yaml-config smoke_test_components) | sed 's/ /,/g'`"
    docker exec $hadoop_head_node bash -c "bash -x /bigtop-home/bigtop-deploy/vm/utils/smoke-tests.sh $smoke_test_components"
}

destroy() {
    if [ -n "$PROVISION_ID" ]; then
        docker-compose -p $PROVISION_ID stop
        docker-compose -p $PROVISION_ID rm -f
    fi
    rm -rvf ./config .provision_id
    echo "Destroy complete."
}

destroy-swarm() {
    docker-machine rm kvstore
    docker-machine rm swarm-master
    docker-machine rm swarm-slave
}

bigtop-puppet() {
    docker exec $1 bash -c 'puppet apply -d --modulepath=/bigtop-home/bigtop-deploy/puppet/modules:/etc/puppet/modules /bigtop-home/bigtop-deploy/puppet/manifests/site.pp'
}

get-yaml-config() {
    RUBY_EXE=ruby
    if [ $# -eq 1 ]; then
        RUBY_SCRIPT="data = YAML::load(STDIN.read); puts data['$1'];"
    elif [ $# -eq 2 ]; then
        RUBY_SCRIPT="data = YAML::load(STDIN.read); puts data['$1']['$2'];"
    else
        echo "The yaml config retrieval function can only take 1 or 2 parameters.";
        exit 1;
    fi
    cat ${yamlconf} | $RUBY_EXE -ryaml -e "$RUBY_SCRIPT" | tr -d '\r'
}

execute() {
    re='^[0-9]+$'
    if [[ $1 =~ $re ]] ; then
        no=$1
        shift
        docker exec -ti ${NODES[$((no-1))]} $@
    else
        name=$1
        shift
        docker exec -ti $name $@
    fi
}

env-check() {
    echo "Environment check..."
    echo "Check docker:"
    docker -v || exit 1
    echo "Check docker-compose:"
    docker-compose -v || exit 1
    echo "Check ruby:"
    ruby -v || exit 1
}

list() {
    docker-compose -p $PROVISION_ID ps
}

PROG=`basename $0`

if [ $# -eq 0 ]; then
    usage
fi

yamlconf="config.yaml"

export DOCKER_IMAGE=$(get-yaml-config docker image)
BIGTOP_HOME_DIR=../../
BIGTOP_PUPPET_DIR=../../bigtop-deploy/puppet
COMPOSE_INSTANCE=bigtop_local
OVERLAY_NETWORK=bigtop
if [ -e .swarm_enabled ]; then
    echo "Swarm enabled. (enable_local_repo is not available in this mode)"
    eval $(docker-machine env --swarm swarm-master)
    COMPOSE_INSTANCE=bigtop_swarm
fi
if [ -e .provision_id ]; then
    PROVISION_ID=`cat .provision_id`
fi
if [ -n "$PROVISION_ID" ]; then
    NODES=(`docker-compose -p $PROVISION_ID ps -q`)
fi

while [ $# -gt 0 ]; do
    case "$1" in
    -c|--create)
        if [ $# -lt 2 ]; then
          echo "Create requires a number" 1>&2
          usage
        fi
        env-check
        create $2
        shift 2;;
    -C|--conf)
        if [ $# -lt 2 ]; then
          echo "Alternative config file for config.yaml" 1>&2
          usage
        fi
	yamlconf=$2
        shift 2;;
    -d|--destroy)
        destroy
        shift;;
    -D|--destroy-all)
        destroy
        destroy-swarm
        shift;;
    -e|--exec)
        if [ $# -lt 3 ]; then
          echo "exec command takes 2 parameters: 1) instance no 2) command to be executed" 1>&2
          usage
        fi
        shift
        execute $@
        shift $#;;
    -E|--env-check)
        env-check
        shift;;
    -l|--list)
        list
        shift;;
    -p|--provision)
        provision
        shift;;
    -w|--swarm)
        swarm
        shift;;
    -s|--smoke-tests)
        smoke-tests
        shift;;
    -S|--scale)
        if [ $# -lt 2 ]; then
          echo "Scale requires a number" 1>&2
          usage
        fi
        scale $2
        shift 2;;
    -h|--help)
        usage
        shift;;
    *)
        echo "Unknown argument: '$1'" 1>&2
        usage;;
    esac
done
