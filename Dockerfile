FROM eclipse-temurin/17-jdk-alpine

WORKDIR /usr/app
COPY build/libs/install ./

ENTRYPOINT ["bin/devcordbot"]
