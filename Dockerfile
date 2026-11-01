# syntax=docker/dockerfile:1
FROM maven:3.9.11-eclipse-temurin-21 AS build
WORKDIR /build
COPY pom.xml .
COPY src src
RUN --mount=type=secret,id=maven_settings \
    --mount=type=secret,id=java_truststore \
    if [ -f /run/secrets/maven_settings ]; then \
      mvn -B -ntp -s /run/secrets/maven_settings \
        -Djavax.net.ssl.trustStore=/run/secrets/java_truststore \
        -Djavax.net.ssl.trustStorePassword=changeit -DskipTests package; \
    else mvn -B -ntp -DskipTests package; fi

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /build/target/teza-backend-*.jar app.jar
USER 10001:10001
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
