# Use official OpenJDK 21 slim image
FROM openjdk:21-jdk-slim

# Set working directory
WORKDIR /app

# Copy the fat JAR from Gradle build
COPY build/libs/search-listing-services*.jar /app/search-listing-services.jar

# Expose the application's port
EXPOSE 9191

# Run the Spring Boot application with the GCP profile
ENTRYPOINT ["java", \
    "-Dspring.profiles.active=gcp", \
    "-Dserver.port=9191", \
    "-Dserver.address=0.0.0.0", \
    "-jar", "/app/search-listing-services.jar"]
