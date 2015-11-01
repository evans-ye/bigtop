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

FROM bigtop/puppet:centos-6

COPY bigtop-deploy/puppet/hieradata /etc/puppet/hieradata
COPY bigtop-deploy/puppet/hiera.yaml /etc/puppet/hiera.yaml
#COPY bigtop-deploy/puppet /bigtop-home/bigtop-deploy/puppet
COPY docker/bigtop-deploy/centos-6/bootstrap.sh /bootstrap.sh

RUN mkdir -p /data/{1,2}

CMD /bootstrap.sh
