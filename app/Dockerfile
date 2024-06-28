FROM eclipse-temurin:21.0.3_9-jdk-alpine as build
VOLUME /tmp
ARG EXTRACTED=build/dependencies
COPY ${EXTRACTED}/dependencies/ ./
COPY ${EXTRACTED}/spring-boot-loader/ ./
COPY ${EXTRACTED}/snapshot-dependencies/ ./
COPY ${EXTRACTED}/application/ ./

RUN addgroup -S spring && adduser -S spring -G spring
USER spring
ENTRYPOINT ["java","org.springframework.boot.loader.launch.JarLauncher"]

#FROM ghcr.io/graalvm/graalvm-ce:21.3.2 as builder
#RUN gu install native-image
#WORKDIR build
#ARG JAR
#COPY build/libs/${JAR} app.jar
#RUN native-image --static -jar app.jar -H:+ReportExceptionStackTraces -H:Name=output
#
#FROM scratch
#COPY --from=builder /build/output /opt/output
#CMD ["/opt/output"]