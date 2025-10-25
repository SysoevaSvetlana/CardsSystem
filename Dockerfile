# Dockerfile для разработки
FROM maven:3.9.5-eclipse-temurin-17

WORKDIR /app

# Копируем pom.xml для кэширования зависимостей
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Копируем исходный код
COPY src ./src

# Порт приложения
EXPOSE 8080

# Запуск через Maven
CMD ["mvn", "spring-boot:run"]

