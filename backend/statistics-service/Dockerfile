FROM eclipse-temurin:21-jdk-alpine

WORKDIR /app

COPY build/libs/statistics-service-0.0.1-SNAPSHOT.jar /app/statistics-service.jar

EXPOSE 8083

CMD ["java", "-jar", "/app/statistics-service.jar"]
