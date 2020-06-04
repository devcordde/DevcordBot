FROM adoptopenjdk/openjdk13-openj9

WORKDIR /usr/app
COPY build/libs/*-all.jar ./bot.jar

ENTRYPOINT ["java","-jar","./bot.jar"]