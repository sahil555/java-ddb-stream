FROM amazoncorretto:17-alpine

WORKDIR /usr/src/app

COPY ./target/*.jar ./service.jar

ENTRYPOINT exec java $JAVA_OPTS  -jar service.jar

#CMD ["java",  "-jar","./service.jar", "${JAVA_OPTS}"]
