# Multi-stage build: a full JDK + Maven to compile, then a small JRE-only image to run.
# The final image has no compiler, no source and no Maven cache.

# ---- Stage 1: build ----
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app

# Copy only the build files first and download dependencies. Docker caches this layer,
# so changing a .java file doesn't re-download the internet.
COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN ./mvnw -q -B dependency:go-offline

COPY src src
# Tests run in CI (they need Docker for Testcontainers), not inside the image build.
RUN ./mvnw -q -B package -DskipTests

# ---- Stage 2: run ----
FROM eclipse-temurin:21-jre
WORKDIR /app

# Don't run as root inside the container.
RUN useradd --system --uid 1001 clearance
COPY --from=build /app/target/clearance-*.jar app.jar
USER clearance

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
