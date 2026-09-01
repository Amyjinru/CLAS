# Build with: docker build -f services/Dockerfile --build-arg SERVICE_MODULE=clas-iam services
FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /workspace

COPY pom.xml ./
COPY clas-common/pom.xml clas-common/pom.xml
COPY clas-iam/pom.xml clas-iam/pom.xml
COPY clas-catalog/pom.xml clas-catalog/pom.xml
COPY clas-order/pom.xml clas-order/pom.xml
COPY clas-compat/pom.xml clas-compat/pom.xml
COPY clas-common/src clas-common/src
COPY clas-iam/src clas-iam/src
COPY clas-catalog/src clas-catalog/src
COPY clas-order/src clas-order/src
COPY clas-compat/src clas-compat/src

ARG SERVICE_MODULE
RUN test -n "$SERVICE_MODULE" && mvn --batch-mode -pl "$SERVICE_MODULE" -am package -DskipTests

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
ARG SERVICE_MODULE
COPY --from=build /workspace/${SERVICE_MODULE}/target/${SERVICE_MODULE}-0.1.0.jar app.jar
EXPOSE 8081 8082 8083 8084
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
