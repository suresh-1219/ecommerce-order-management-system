# ---------- Stage 1: Build ----------
# Uses a Maven + JDK 21 image only to compile and package the app.
# This entire stage is discarded from the final image — it just produces the jar.
FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /app

# Copy the POM first and download dependencies separately from the source code.
# Docker caches layers: as long as pom.xml doesn't change, this layer (and its
# downloaded dependencies) is reused on rebuilds, so only source changes trigger
# a fresh dependency download.
COPY pom.xml .
RUN mvn dependency:go-offline -B

COPY src ./src
RUN mvn clean package -DskipTests -B

# ---------- Stage 2: Run ----------
# A minimal JRE (no Maven, no JDK, no build tools) — much smaller and safer
# than shipping the build toolchain in the runtime image.
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Copy only the built jar from the build stage
COPY --from=build /app/target/ecommerce-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8081

ENTRYPOINT ["java", "-jar", "app.jar"]
