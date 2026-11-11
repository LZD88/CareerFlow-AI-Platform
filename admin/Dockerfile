FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

COPY target/YQ-AI-ADMIN-1.0-SNAPSHOT.jar app.jar

EXPOSE 8081

ENTRYPOINT ["java", "-jar", "app.jar", "--spring.profiles.active=prod"]
