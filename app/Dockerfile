FROM eclipse-temurin:21
RUN addgroup -system spring
RUN adduser -system -group spring
USER spring:spring
ARG DEPENDENCIES=build/dependencies
COPY ${DEPENDENCIES}/BOOT-INF/lib /app/lib
COPY ${DEPENDENCIES}/META-INF /app/META-INF
COPY ${DEPENDENCIES}/BOOT-INF/classes /app
ENTRYPOINT ["java","-cp","app:app/lib/*","code.App"]