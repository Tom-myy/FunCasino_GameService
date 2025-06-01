# ---------- Step 1: Build .jar ----------
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app
COPY . .

RUN chmod +x mvnw

# 🛠️ Отключаем MAVEN_CONFIG, если вдруг Render или образ его подставляет
ENV MAVEN_CONFIG=""

RUN ./mvnw clean package -DskipTests

# ---------- Step 2: Launch ----------
FROM eclipse-temurin:21
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
