# Dockerfile
FROM eclipse-temurin:23-jdk-alpine

# Install tools for debugging
RUN apk add vim bash bash-doc curl htop netcat-openbsd

# Set working directory
WORKDIR /app

# Copy your jar file into the container
COPY virusDetector.jar /app/virusDetector.jar

# Set entrypoint
ENTRYPOINT ["java", "-jar", "-Dspring.profiles.active=${ENV_APP}", "/app/virusDetector.jar"]

