FROM maven:3.9.11-eclipse-temurin-21 AS build
WORKDIR /app
ENV MAVEN_OPTS="-Xmx768m -XX:ActiveProcessorCount=2"
COPY pom.xml mvnw ./
COPY .mvn .mvn
COPY src src
RUN chmod +x mvnw && ./mvnw -B -Dmaven.test.skip=true package

FROM eclipse-temurin:21-jre AS runtime
WORKDIR /app
USER 10001:10001
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]

FROM runtime AS verified
COPY target/inventory-service-*.jar app.jar

FROM runtime AS local
COPY --from=build /app/target/inventory-service-*.jar app.jar
