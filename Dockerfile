# Use an official OpenJDK 21 image
FROM openjdk:21-jdk-slim

WORKDIR /app

COPY build/libs/search-listing-services*.jar /app/search-listing-services.jar

# Expose the port the app should run on
EXPOSE 9191

# Run the WAR with both profile and port forced
ENTRYPOINT ["java", \
    "-Dspring.profiles.active=gcp", \
    "-Dserver.port=9191", \
    "-Dserver.address=0.0.0.0", \
    "-jar", "/app/search-listing-services.jar"]
