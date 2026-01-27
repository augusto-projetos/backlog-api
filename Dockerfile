# --- Etapa 1: Construir o projeto (Build) ---
# Usamos uma imagem Maven baseada no Eclipse Temurin (Java 17)
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY . .
RUN mvn clean package -DskipTests

# --- Etapa 2: Rodar o projeto (Run) ---
# Usamos uma imagem leve do Linux com Java 17 (Eclipse Temurin)
FROM eclipse-temurin:17-jdk-jammy
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]