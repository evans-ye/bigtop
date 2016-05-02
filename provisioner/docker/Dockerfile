# Creates Bigtop base docker image
#
# 

FROM PREBUILD_SRC_IMAGE
MAINTAINER Amir Sanjar
USER root

ADD install_bigtop_stack.sh /etc/install_bigtop_stack.sh
RUN chown root:root /etc/install_bigtop_stack.sh
RUN chmod 700 /etc/install_bigtop_stack.sh

ADD setup-env.sh /etc/setup-env.sh
RUN chown root:root /etc/setup-env.sh
RUN chmod 700 /etc/setup-env.sh

ARG COMPONENTS
RUN /etc/install_bigtop_stack.sh $COMPONENTS
ARG REPO
RUN /etc/setup-env.sh $REPO

CMD ["bash"]
