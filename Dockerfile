# Use Java 21 slim base image
FROM eclipse-temurin:21-jdk-alpine

# Set working directory
WORKDIR /app

# Copy the only jar file (assuming one jar in build/libs)
COPY build/libs/search-listing-service*.jar /app/search-listing-service.jar

# Expose port used by the Spring Boot app
EXPOSE 9191

# Run the app with the desired Spring profile and port
ENTRYPOINT ["java", \
    #"-Dspring.profiles.active=gcp", \
    "-Dserver.port=9191", \
    "-Dserver.address=0.0.0.0", \
    "-jar", "/app/search-listing-service.jar"]
