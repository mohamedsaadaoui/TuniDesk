# ---- Build stage ----
FROM maven:3.9-eclipse-temurin-26 AS build
WORKDIR /app

COPY pom.xml .
RUN mvn -B -q dependency:go-offline

COPY src ./src
RUN mvn -B -q package -DskipTests

# ---- Runtime stage ----
FROM eclipse-temurin:21-jre
WORKDIR /app

RUN useradd -r -u 1001 tunidesk \
    && mkdir -p /data/uploads \
    && chown -R tunidesk:tunidesk /data /app

COPY --from=build /app/target/*.jar app.jar

USER tunidesk
EXPOSE 8080

ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
