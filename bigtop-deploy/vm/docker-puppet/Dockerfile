# Bring up sshd
FROM bigtop/seed:centos-6.4

# ssh
RUN yum -y install openssh-server openssh-clients sudo
RUN sed -i.bak s/UsePAM\ yes/UsePAM\ no/g /etc/ssh/sshd_config
RUN ssh-keygen -q -N "" -t dsa -f /etc/ssh/ssh_host_dsa_key
RUN ssh-keygen -q -N "" -t rsa -f /etc/ssh/ssh_host_rsa_key

# setup vagrant account
RUN mkdir ~/.ssh/
RUN chmod 0755 ~/.ssh
RUN wget http://github.com/mitchellh/vagrant/raw/master/keys/vagrant.pub --no-check-certificate -O ~/.ssh/authorized_keys
RUN chmod 0644 ~/.ssh/authorized_keys

CMD /usr/sbin/sshd -D 
