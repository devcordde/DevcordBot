FROM adoptopenjdk/openjdk13-openj9 as builder
WORKDIR /usr/app
COPY . .
RUN ./gradlew build

FROM adoptopenjdk/openjdk13-openj9
WORKDIR /usr/app
COPY --from=builder /usr/app/build/libs/*-all.jar ./bot.jar
ENTRYPOINT ["java","-jar","./bot.jar"]