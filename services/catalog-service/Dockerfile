# Build from the repository root: docker build -f services/catalog-service/Dockerfile .
FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /workspace/catalog-service

COPY services/catalog-service/pom.xml ./
RUN mvn --batch-mode dependency:go-offline
COPY services/catalog-service/src ./src
RUN mvn --batch-mode -DskipTests package

FROM eclipse-temurin:17-jre-alpine
RUN addgroup -S clas && adduser -S clas -G clas
WORKDIR /opt/clas
COPY --from=build /workspace/catalog-service/target/catalog-service-*.jar app.jar
USER clas
EXPOSE 8081
HEALTHCHECK --interval=15s --timeout=5s --start-period=30s --retries=6 \
  CMD wget -qO- http://127.0.0.1:8081/actuator/health >/dev/null || exit 1
ENTRYPOINT ["java", "-jar", "/opt/clas/app.jar"]
