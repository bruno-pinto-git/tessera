FROM eclipse-temurin:21-jdk-alpine

WORKDIR /app

COPY build/libs/match-service-0.0.1-SNAPSHOT.jar /app/match-service.jar

EXPOSE 8082

CMD ["java", "-jar", "/app/match-service.jar"]
