FROM eclipse-temurin:21-jdk-alpine

WORKDIR /app

COPY build/libs/ticket-service-0.0.1-SNAPSHOT.jar /app/ticket-service.jar

EXPOSE 8081

CMD ["java", "-jar", "/app/ticket-service.jar"]
