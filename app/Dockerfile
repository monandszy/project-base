FROM eclipse-temurin:21-jdk-alpine
VOLUME /tmp
ARG EXTRACTED=build/dependencies
COPY ${EXTRACTED}/dependencies/ ./
COPY ${EXTRACTED}/spring-boot-loader/ ./
COPY ${EXTRACTED}/snapshot-dependencies/ ./
COPY ${EXTRACTED}/application/ ./

RUN addgroup -S spring && adduser -S spring -G spring
USER spring
ENTRYPOINT ["java","org.springframework.boot.loader.launch.JarLauncher"]