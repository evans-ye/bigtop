class bigtop_repo {
  case $::operatingsystem {
    /(OracleLinux|Amazon|CentOS|Fedora|RedHat)/: {
       yumrepo { "Bigtop":
          baseurl => hiera("bigtop::bigtop_repo_uri", $default_repo),
          descr => "Bigtop packages",
          enabled => 1,
          gpgcheck => 0,
       }
       Yumrepo<||> -> Package<||>
    }
    /(Ubuntu|Debian)/: {
       include apt
       apt::conf { "disable_keys":
          content => "APT::Get::AllowUnauthenticated 1;",
          ensure => present
       } ->
       apt::source { "Bigtop":
          location => hiera("bigtop::bigtop_repo_uri", $default_repo),
          release => "bigtop",
          repos => "contrib",
          ensure => present,
       } ->
       exec {'bigtop-apt-update':
          command => '/usr/bin/apt-get update'
       }
    }
    default: {
      notify{"WARNING: running on a neither yum nor apt platform -- make sure Bigtop repo is setup": }
    }
  }
}
