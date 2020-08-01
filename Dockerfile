FROM adoptopenjdk/openjdk14-openj9 as builder
WORKDIR /usr/app
COPY . .
RUN ./gradlew installDist -Dorg.gradle.daemon=false

FROM adoptopenjdk/openjdk14-openj9

WORKDIR /usr/app
COPY build/install/devcordbot/ .

ENTRYPOINT ["/usr/app/bin/devcordbot"]