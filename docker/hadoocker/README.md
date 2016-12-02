# Hadoocker

A big data service testing toolkit using Docker

## How to run

* Make sure you have Docker installed. We've tested this using [Docker for Mac](https://docs.docker.com/docker-for-mac/)

* Running Hadoop

```
docker run -p 50070:50070 bigtop/hadoocker:centos-6_hadoop
```

* Running Spark (Standalone mode)

```
docker run -p 8080:8080 bigtop/hadoocker:debian-8_spark
```

* Running Hadoop + HBase

```
docker run -p 50070:50070 -p 60010:60010 bigtop/hadoocker:ubuntu-14.04_hbase
```

## How to build

### Examples

* Build Hadoocker image that has Hadoop provisioned

```
./build.sh -a bigtop -o debian-8 -c hadoop 
```

* Build Hadoocker image that has Hadoop and Spark provisioned

```
./build.sh -a bigtop -o debian-8 -c "hadoop, yarn, spark"
```

* Build Hadoocker image that has Hadoop and HBase provisioned

```
./build.sh -a bigtop -o debian-8 -c "hadoop, hbase"
```

### Customize your Hadoop stack

* Edit *site.yaml.template.centos-6_hadoop* to create your own prefered stack

```
cp site.yaml.template.centos-6_hadoop site.yaml.template.centos-6_hadoop_ignite
vim site.yaml.template.centos-6_hadoop_ignite
```

* Add ignite in *hadoop_cluster_node::cluster_components* array and leave the other unchanged

```
...
hadoop_cluster_node::cluster_components: [hadoop, yarn, ignite]
...
```

* Build 

```
./build.sh -a bigtop -o centos-6 -f site.yaml.template.centos-6_hadoop_ignite -t my_ignite_stack
```
