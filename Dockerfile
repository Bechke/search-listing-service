# Use an official OpenJDK 21 image
FROM openjdk:21-jdk-slim

# Set the working directory
WORKDIR /app

# Copy the WAR file from the build context
COPY build/libs/search-listing-services*.war /app/search-listing-services.war

# Expose the port the app runs on
EXPOSE 9191

# Run the WAR file using embedded Tomcat
ENTRYPOINT ["java", "-jar", "/app/search-listing-services.war", "--spring.profiles.active=gcp"]
