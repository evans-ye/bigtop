# Hadoocker

A big data service testing toolkit using Docker

## How to run

* Make sure you have Docker installed. I've tested this using [Docker for Mac](https://docs.docker.com/docker-for-mac/)

* Run Hadoop

```
docker run -d -p 50070:50070 evansye/hadoocker:hadoop
```

* Run Hadoop + Spark (Standalone mode)

```
docker run -d -p 50070:50070 -p 8080:8080 evansye/hadoocker:spark
```

* Run Hadoop + HBase

```
docker run -d -p 50070:50070 -p 60010:60010 evansye/hadoocker:hbase
```

## How to build

### Examples

* Build Hadoocker image that has Hadoop provisioned

```
./build.sh hadoop
```

* Build Hadoocker image that has Hadoop and Spark provisioned

```
./build.sh spark
```

* Build Hadoocker image that has Hadoop and HBase provisioned

```
./build.sh hbase
```

### Customize your Hadoop stack

* Edit *site.yaml.template.hadoop* to create your own prefered stack

```
cp site.yaml.template.hadoop site.yaml.template.ignite
vim site.yaml.template.ignite
```

* Add ignite in *hadoop_cluster_node::cluster_components* array and leave the other unchanged

```
...
hadoop_cluster_node::cluster_components: [hadoop, yarn, ignite]
...
```

* Build 

```
./build.sh ignite
```
