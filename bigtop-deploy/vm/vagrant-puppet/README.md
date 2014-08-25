    Licensed to the Apache Software Foundation (ASF) under one or more
    contributor license agreements. See the NOTICE file distributed with
    this work for additional information regarding copyright ownership.
    The ASF licenses this file to You under the Apache License, Version 2.0
    (the "License"); you may not use this file except in compliance with
    the License. You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

------------------------------------------------------------------------------------------------------------------------------------------------------

#BigTop VM provisioner

## Overview

The Vagrantfile definition creates a bigtop virtual hadoop cluster for you, by pulling from existing publishing bigtop repositories.
This cluster can be used:

- to test bigtop smoke tests
- to test bigtop puppet recipes

Eventually, we may also add ability to build AND provision bigtop in a vagrant recipe, which would essentially
give full validation of the BigTop stack.

## USAGE

1) Install [vagrant-hostmanager plugin](https://github.com/smdahlen/vagrant-hostmanager) to better manage `/etc/hosts`

```
vagrant plugin install vagrant-hostmanager
```

2) Install [vagrant-cachier plugin](https://github.com/fgrehm/vagrant-cachier) to cache packages at local

```
vagrant plugin install vagrant-cachier
```

3) To provision a 3 node Apache Hadoop cluster on top of vagrant boxes

```
vagrant up
```

4) You can specify number of nodes you'd like to provision by modifying `num_instances` in Vagrantfile

```
num_instances = 5
```

##Example:

5) Run hbase-test.sh to evaluate the deployment.

##Configure Apache Hadoop ecosystem components
* Choose the ecosystem you want to be deployed by modifying components in provision.sh.

```
     components,hadoop,hbase,yarn,mapred-app,...
```

By default, Apache Hadoop, YARN, and Apache HBase will be installed.
See `bigtop-deploy/puppet/config/site.csv.example` for more details.

##Note:

For bigtop 0.7.0 code base, you must change the value of the [yarn-site.xml](https://github.com/apache/bigtop/blob/master/bigtop-deploy/puppet/modules/hadoop/templates/yarn-site.xml) yarn.nodemanager.aux.services from "mapreduce_shuffle" to "mapreduce.shuffle" before `vagrant up`
