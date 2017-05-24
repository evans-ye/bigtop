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

# Prepare default repo by detecting the environment automatically
case $operatingsystem {
    # Use CentOS 7 repo for other CentOS compatible OSs
    /(OracleLinux|Amazon|RedHat)/: {
      $default_repo = "http://bigtop-repos.s3.amazonaws.com/releases/1.0.0/centos/7/x86_64"
    }
    # Detect env to pick up default repo for other Bigtop supported OSs
    default: {
      include stdlib

      $lower_os = downcase($operatingsystem)
      # We use code name such as trusty for Ubuntu instead of release version in bigtop's binary convenience repos
      if ($operatingsystem == "Ubuntu") { $release = $lsbdistcodename } else { $release = $operatingsystemmajrelease }
      $default_repo = "http://bigtop-repos.s3.amazonaws.com/releases/1.0.0/${lower_os}/${release}/x86_64"
    }
}

$jdk_preinstalled = hiera("bigtop::jdk_preinstalled", false)
if ( ! $jdk_preinstalled ) {
   require jdk
}

$provision_repo = hiera("bigtop::provision_repo", true)

if ($provision_repo) {
   require bigtop_repo
}

node default {

  $roles_enabled = hiera("bigtop::roles_enabled", false)

  if (!is_bool($roles_enabled)) {
    fail("bigtop::roles hiera conf is not of type boolean. It should be set to either true or false")
  }

  if ($roles_enabled) {
    include node_with_roles
  } else {
    include node_with_components
  }
}

if versioncmp($::puppetversion,'3.6.1') >= 0 {
  $allow_virtual_packages = hiera('bigtop::allow_virtual_packages',false)
  Package {
    allow_virtual => $allow_virtual_packages,
  }
}
