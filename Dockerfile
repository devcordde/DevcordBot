FROM adoptopenjdk/openjdk15-openj9

WORKDIR /usr/app
COPY build/libs/install ./

ENTRYPOINT ["bin/devcordbot"]