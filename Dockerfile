FROM eclipse-temurin:21-jdk-alpine
WORKDIR /app
COPY build/libs/*.jar app.jar
EXPOSE 9191
ENTRYPOINT ["java", "-jar", "app.jar"]
