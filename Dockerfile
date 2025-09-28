# build-стейдж
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app

COPY pom.xml .
RUN --mount=type=secret,id=maven_settings,target=/root/.m2/settings.xml \
    --mount=type=cache,target=/root/.m2 \
    mvn -q -DskipTests dependency:go-offline

COPY src ./src
RUN --mount=type=secret,id=maven_settings,target=/root/.m2/settings.xml \
    --mount=type=cache,target=/root/.m2 \
    mvn -q -DskipTests clean package \
 && cp target/*.jar /app/app.jar   # <-- добавили

# runtime-стейдж
FROM eclipse-temurin:21-jre
WORKDIR /app
RUN apt-get update && apt-get install -y --no-install-recommends curl && rm -rf /var/lib/apt/lists/*
COPY --from=build /app/app.jar ./app.jar
HEALTHCHECK --interval=10s --timeout=3s --start-period=20s --retries=10 \
  CMD curl -fsS --max-time 1 http://127.0.0.1:8080/actuator/health/readiness | grep -q '"status":"UP"' || exit 1
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/app.jar"]
