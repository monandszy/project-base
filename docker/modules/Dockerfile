FROM eclipse-temurin:21.0.3_9-jdk-alpine as build
ARG JAR_PATH
COPY ${JAR_PATH} ./app.jar
ENTRYPOINT ["java","-jar","/app.jar"]