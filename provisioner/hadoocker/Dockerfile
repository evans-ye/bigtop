FROM bigtop/deploy:centos-6
ADD startup.sh /startup.sh
ADD shutdown.sh /shutdown.sh
ADD bigtop-puppet /bigtop-puppet
ADD bigtop-puppet/hiera.yaml /etc/puppet/hiera.yaml
ADD bigtop-puppet/hieradata /etc/puppet/hieradata
ADD site.yaml.template /etc/puppet/hieradata/site.yaml.template
RUN /startup.sh -i 
RUN /shutdown.sh
CMD /startup.sh -f
