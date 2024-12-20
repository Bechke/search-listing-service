# Use an official OpenJDK 23 image
FROM openjdk:23-jdk-slim

# Set the working directory
WORKDIR /app

# Copy the WAR file from the build context
COPY build/libs/vyapari-services*.war /app/vyapari-services.war

# Expose the port the app runs on
EXPOSE 8080

# Run the WAR file using embedded Tomcat
ENTRYPOINT ["java", "-jar", "/app/vyapari-services.war"]
