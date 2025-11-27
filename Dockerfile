# Use a base image with Java 17
FROM openjdk:17-jdk-slim

# Set the working directory in the container
WORKDIR /app

# Copy the Maven wrapper files to the working directory
COPY mvnw .
COPY .mvn .mvn

# Copy the pom.xml file to download dependencies
COPY pom.xml .

# Download dependencies - this step will be cached unless pom.xml changes
RUN ./mvnw dependency:go-offline -B

# Copy the source code
COPY src src

# Build the application
RUN ./mvnw install -DskipTests

# Run the application
# We assume the JAR will be in target/
# Replace 'campushub-support-service-0.0.1-SNAPSHOT.jar' with your actual JAR name if different
CMD ["java", "-jar", "target/campushub-support-service-0.0.1-SNAPSHOT.jar"]
