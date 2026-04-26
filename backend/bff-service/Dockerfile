FROM eclipse-temurin:21-jdk-alpine

WORKDIR /app

COPY build/libs/bff-service-0.0.1-SNAPSHOT.jar /app/bff-service.jar

EXPOSE 8080

CMD ["java", "-jar", "/app/bff-service.jar"]
