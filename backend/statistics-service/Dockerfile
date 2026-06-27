# Multi-stage: build the bootable jar with Gradle, then run it on a slim JRE.
# Self-contained so `docker compose build` works anywhere (e.g. the deploy VM)
# without a locally pre-built jar.
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app
COPY . .
# Invoke the Gradle wrapper via its jar (not ./gradlew) so Windows CRLF line
# endings on the gradlew script don't break the build.
RUN java -cp gradle/wrapper/gradle-wrapper.jar org.gradle.wrapper.GradleWrapperMain bootJar -x test --no-daemon

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/build/libs/statistics-service-0.0.1-SNAPSHOT.jar /app/app.jar
EXPOSE 8083
# JAVA_OPTS lets the compose file cap the heap so the whole stack fits on a small VM.
ENV JAVA_OPTS="-XX:MaxRAMPercentage=70"
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
