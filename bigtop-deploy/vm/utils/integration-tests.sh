#!/bin/bash -x

# Install Maven
puppet apply --modulepath=/bigtop-home -e "include bigtop_toolchain::maven"
puppet apply --modulepath=/bigtop-home -e "include bigtop_toolchain::env"
source /etc/profile.d/bigtop.sh

hadoop-integration-tests() {
    # Hadoop integration tests need to be run as HDFS to get rid of permission denied issue
    echo "hdfs    ALL=(ALL)       NOPASSWD: ALL" >> /etc/sudoers
    mkdir /bigtop-home/bigtop-tests/test-execution/smokes/hadoop/target
    chown -R hdfs:hdfs /bigtop-home/bigtop-tests/test-execution/smokes/hadoop/target
    
    su - hdfs <<'EOF'
export JAVA_HOME=/usr/lib/jvm/java-openjdk
export HADOOP_HOME=/usr/lib/hadoop 
export HADOOP_CONF_DIR=/etc/hadoop/conf
export HBASE_HOME=/usr/lib/hbase
export HBASE_CONF_DIR=/etc/hbase/conf
export ZOOKEEPER_HOME=/usr/lib/zookeeper
export HIVE_HOME=/usr/lib/hive
export PIG_HOME=/usr/lib/pig
export FLUME_HOME=/usr/lib/flume
export SQOOP_HOME=/usr/lib/sqoop
export HCAT_HOME=/usr/lib/hcatalog
export OOZIE_URL=http://localhost:11000/oozie
export HADOOP_MAPRED_HOME=/usr/lib/hadoop-mapreduce

cd /bigtop-home/bigtop-tests/test-execution/smokes/hadoop
mvn verify
EOF
}

integration-tests() {
    export JAVA_HOME=/usr/lib/jvm/java-openjdk
    export HADOOP_HOME=/usr/lib/hadoop 
    export HADOOP_CONF_DIR=/etc/hadoop/conf
    export HBASE_HOME=/usr/lib/hbase
    export HBASE_CONF_DIR=/etc/hbase/conf
    export ZOOKEEPER_HOME=/usr/lib/zookeeper
    export HIVE_HOME=/usr/lib/hive
    export PIG_HOME=/usr/lib/pig
    export FLUME_HOME=/usr/lib/flume
    export SQOOP_HOME=/usr/lib/sqoop
    export HCAT_HOME=/usr/lib/hcatalog
    export OOZIE_URL=http://localhost:11000/oozie
    export HADOOP_MAPRED_HOME=/usr/lib/hadoop-mapreduce
    
    cd /bigtop-home/bigtop-tests/test-execution/smokes/$1
    mvn verify
}

for conponent in `echo $1 | tr ' ' '\n'`; do
    if [ x"$conponent" == x"hadoop" ]; then
        hadoop-integration-tests
    else
        integration-tests $conponent
    fi
done
