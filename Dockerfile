# 1. Usa uma imagem base com o Java 17
FROM openjdk:17-jdk-slim

# 2. Define a pasta de trabalho dentro do container
WORKDIR /app

# 3. Copia o arquivo que o Maven gera para dentro do container
# O "target/*.jar" pega qualquer jar gerado
COPY target/*.jar app.jar

# 4. Comando para rodar o app
ENTRYPOINT ["java", "-jar", "app.jar"]