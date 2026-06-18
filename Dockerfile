# ── Stage 1: сборка ────────────────────────────────────────────────────────
FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /app

# Сначала только pom.xml — кэшируем слой с зависимостями отдельно от кода.
# Это значит что при изменении только .java файлов maven не будет
# заново скачивать все зависимости из интернета.
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Теперь копируем код и собираем jar
COPY src ./src
RUN mvn clean package -DskipTests -B

# ── Stage 2: финальный образ ─────────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Непривилегированный пользователь — не запускаем приложение от root
RUN addgroup -S spring && adduser -S spring -G spring

COPY --from=build /app/target/*.jar app.jar

RUN chown spring:spring app.jar
USER spring

EXPOSE 8080

# Проверка что приложение живо — Spring Boot Actuator должен быть в pom.xml
HEALTHCHECK --interval=30s --timeout=5s --start-period=40s --retries=3 \
    CMD wget -qO- http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]
