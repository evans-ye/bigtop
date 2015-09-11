class bigtop_toolchain::zinc {

  include bigtop_toolchain::deps

  exec { '/bin/tar xvzf /usr/src/zinc-0.3.7.tgz':
    cwd         => '/usr/local',
    require     => Exec['/usr/bin/wget http://downloads.typesafe.com/zinc/0.3.7/zinc-0.3.7.tgz'],
    refreshonly => true,
    subscribe   => Exec['/usr/bin/wget http://downloads.typesafe.com/zinc/0.3.7/zinc-0.3.7.tgz'],
  }

  file { '/usr/local/zinc':
    ensure  => link,
    target  => '/usr/local/zinc-0.3.7',
    require => Exec['/bin/tar xvzf /usr/src/zinc-0.3.7.tgz'],
  }
}
