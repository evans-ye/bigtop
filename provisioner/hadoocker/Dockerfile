FROM bigtop/deploy:centos-6
ADD startup.sh /startup.sh
ADD shutdown.sh /shutdown.sh
ADD bigtop-deploy /bigtop-deploy
ADD bigtop-deploy/puppet/hiera.yaml /etc/puppet/hiera.yaml
ADD bigtop-deploy/puppet/hieradata /etc/puppet/hieradata
ADD site.yaml.template /etc/puppet/hieradata/site.yaml.template
#ADD bigtop-home /bigtop-home
#ADD hiera.yaml /etc/puppet/hiera.yaml
#ADD hieradata /etc/puppet/hieradata
RUN /startup.sh -i 
RUN /shutdown.sh
CMD /startup.sh -f
