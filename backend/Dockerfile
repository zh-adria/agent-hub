FROM eclipse-temurin:21-jdk

WORKDIR /app

COPY target/agent-hub-backend-1.0.0-SNAPSHOT.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
