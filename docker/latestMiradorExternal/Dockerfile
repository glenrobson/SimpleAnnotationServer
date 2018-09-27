FROM centos:7

# Setup centos:
ENV container docker
RUN (cd /lib/systemd/system/sysinit.target.wants/; for i in *; do [ $i == \
systemd-tmpfiles-setup.service ] || rm -f $i; done); \
rm -f /lib/systemd/system/multi-user.target.wants/*;\
rm -f /etc/systemd/system/*.wants/*;\
rm -f /lib/systemd/system/local-fs.target.wants/*; \
rm -f /lib/systemd/system/sockets.target.wants/*udev*; \
rm -f /lib/systemd/system/sockets.target.wants/*initctl*; \
rm -f /lib/systemd/system/basic.target.wants/*;\
rm -f /lib/systemd/system/anaconda.target.wants/*;
VOLUME [ "/sys/fs/cgroup" ]

RUN yum install -y tomcat unzip maven lsof httpd
#time taking tasks
RUN mkdir /opt/solr
WORKDIR /opt/solr
RUN useradd -ms /bin/bash solr
RUN chown -R solr /opt/solr
RUN curl -OL http://archive.apache.org/dist/lucene/solr/6.6.1/solr-6.6.1.tgz

# Setup SAS
RUN echo 'JAVA_OPTS="-DSAS.store=solr-cloud -DSAS.solr_connection=http://localhost:8983/solr,http://localhost:7574/solr -DSAS.solr_collection=test"' >> /etc/tomcat/tomcat.conf

WORKDIR /usr/local/src
#RUN curl -LO "https://github.com/glenrobson/SimpleAnnotationServer/archive/mirador-2.1.4.zip"
#RUN unzip mirador-2.1.4.zip
COPY sas.tar.gz /usr/local/src
RUN mkdir sas
WORKDIR /usr/local/src/sas
RUN tar zxvf ../sas.tar.gz
RUN mvn package
RUN cp target/simpleAnnotationStore.war /usr/share/tomcat/webapps/sas.war


# Setup SOLR
WORKDIR /opt/solr
RUN su solr -c "/usr/local/src/sas/travis/install_solrcloud.sh /usr/local/src/sas"
RUN su solr -c "/opt/solr/solr-6.6.1/bin/solr stop -all "
ADD solr.service /etc/systemd/system
ADD startCloud.sh /opt/solr/solr-6.6.1/bin/
RUN systemctl enable solr.service
EXPOSE 8983
EXPOSE 7574

RUN systemctl enable tomcat.service
EXPOSE 8080

# Setup httpd
WORKDIR /usr/local/src
RUN curl -OL https://github.com/ProjectMirador/mirador/releases/download/v2.6.0/build.zip
RUN unzip build.zip
RUN mv build/mirador /var/www/html/
ADD index.html /var/www/html/
ADD 3320651.json /var/www/html/3320651.json
ADD 4389767.json /var/www/html/4389767.json
ADD annotations.json /var/www/html/annotations.json
ADD sas.conf /etc/httpd/conf.d/


WORKDIR /var/www/html/mirador
#RUN curl -OL https://raw.githubusercontent.com/ProjectMirador/mirador/develop/js/src/annotations/simpleASEndpoint.js

RUN systemctl enable httpd.service

COPY testSetup.sh /usr/local/src
RUN chmod 755 /usr/local/src/testSetup.sh

COPY setup.service /etc/systemd/system
RUN systemctl enable setup.service

STOPSIGNAL SIGRTMIN+3
EXPOSE 80

CMD ["/sbin/init"]
