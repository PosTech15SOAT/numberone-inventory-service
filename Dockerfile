FROM maven:3.9.11-eclipse-temurin-21 AS build
WORKDIR /app
ENV MAVEN_OPTS="-Xmx768m -XX:ActiveProcessorCount=2"
COPY pom.xml mvnw ./
COPY .mvn .mvn
COPY src src
RUN chmod +x mvnw && ./mvnw -B -Dmaven.test.skip=true package

FROM eclipse-temurin:21-jre AS runtime
WORKDIR /app
ENV DD_HTTP_SERVER_TAG_QUERY_STRING=false DD_HTTP_CLIENT_TAG_QUERY_STRING=false
ADD --chmod=0444 --checksum=sha256:bdd2f818138b683d0c266637fbda4bccf01e940d55bdeb2ea43aaa6d43218ddf https://repo.maven.apache.org/maven2/com/datadoghq/dd-java-agent/1.64.2/dd-java-agent-1.64.2.jar /app/dd-java-agent.jar
COPY --chmod=755 entrypoint.sh /app/entrypoint.sh
USER 10001:10001
EXPOSE 8080
ENTRYPOINT ["/app/entrypoint.sh"]

FROM runtime AS verified
COPY target/inventory-service-*.jar app.jar

FROM runtime AS local
COPY --from=build /app/target/inventory-service-*.jar app.jar
