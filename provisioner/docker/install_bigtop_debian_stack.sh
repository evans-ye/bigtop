#!/bin/bash -x
#for com in $install_list; do
#    case $com in
#    hadoop)
#        apt-get install -yq hadoop hadoop-hdfs ;;
#    yarn)
#        apt-get install -yq hadoop-yarn ;;
#    *)
#        apt-get install -yq $com ;;
#    esac
#done
apt-get update
while [ $# -gt 0 ]; do
    case "$1" in
    hadoop)
        apt-get install -yq hadoop hadoop-client hadoop-conf-pseudo hadoop-debuginfo hadoop-doc hadoop-hdfs hadoop-hdfs-datanode hadoop-hdfs-fuse hadoop-hdfs-journalnode hadoop-hdfs-namenode hadoop-hdfs-secondarynamenode hadoop-hdfs-zkfc hadoop-httpfs hadoop-libhdfs hadoop-libhdfs-devel 
        shift;;
    yarn)
        apt-get install -yq hadoop-mapreduce hadoop-mapreduce-historyserver hadoop-yarn hadoop-yarn-nodemanager hadoop-yarn-proxyserver hadoop-yarn-resourcemanager hadoop-yarn-timelineserver
        shift;;
    *)
        apt-get install -yq $1
        shift;;
    esac
done
