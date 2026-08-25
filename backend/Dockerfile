FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /workspace
COPY pom.xml .
COPY src ./src
RUN mvn -B -DskipTests package

FROM eclipse-temurin:17-jre
WORKDIR /app
RUN apt-get update \
	&& apt-get install -y --no-install-recommends curl \
	&& rm -rf /var/lib/apt/lists/*
RUN useradd --system --uid 10001 clas
COPY --from=build /workspace/target/*.jar app.jar
RUN mkdir -p /app/uploads && chown -R clas:clas /app
USER clas
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
