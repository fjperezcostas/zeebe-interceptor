FROM camunda/zeebe:8.5.0

ADD target/lib /tmp/lib
ADD target/zeebe-interceptor-1.0.0.jar /usr/local/zeebe/zeebe-interceptor-1.0.0.jar

RUN cp -r /tmp/lib/* /usr/local/zeebe/lib/
