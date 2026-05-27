FROM maven:3.9-eclipse-temurin-17 AS build

WORKDIR /app

COPY pom.xml .
RUN mvn dependency:go-offline -B

COPY src src
RUN mvn package -DskipTests

FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Устанавливаем wget и создаем директорию для сертификатов
RUN apk add --no-cache wget

# Скачиваем сертификат Render
RUN wget -O /etc/ssl/certs/render.crt https://render.com/ssl/render.crt

COPY --from=build /app/target/RideShare-0.0.1-SNAPSHOT.jar app.jar

RUN mkdir -p /app/logs

EXPOSE 8080

ENV SPRING_PROFILES_ACTIVE=prod \
    JAVA_OPTS="-Xmx512m -Xms256m"

# Добавляем сертификат в Java truststore
RUN keytool -keystore /opt/java/openjdk/lib/security/cacerts -storepass changeit -noprompt -trustcacerts -importcert -alias render -file /etc/ssl/certs/render.crt

HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD wget --quiet --tries=1 --spider http://localhost:8080/swagger-ui.html || exit 1

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]