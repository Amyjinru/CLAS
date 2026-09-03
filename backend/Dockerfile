# 构建上下文必须为仓库根目录：docker build -f backend/Dockerfile .
FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /workspace/backend

COPY backend/pom.xml ./
# Maven Central 偶发限流时最多重试两次；最终失败仍使镜像构建失败，不能掩盖依赖问题。
RUN for attempt in 1 2 3; do \
      mvn --batch-mode dependency:go-offline && exit 0; \
      [ "$attempt" = 3 ] && exit 1; \
      sleep 5; \
    done

COPY backend/src ./src
RUN mvn --batch-mode -DskipTests package

FROM eclipse-temurin:17-jre-alpine
RUN apk add --no-cache bash mysql-client wget \
    && addgroup -S clas \
    && adduser -S clas -G clas
WORKDIR /opt/clas

COPY --from=build /workspace/backend/target/*.jar app.jar
COPY database ./database
RUN chown -R clas:clas /opt/clas

USER clas
EXPOSE 8080
HEALTHCHECK --interval=15s --timeout=5s --start-period=45s --retries=6 \
  CMD wget -qO- http://127.0.0.1:8080/api/health >/dev/null || exit 1
ENTRYPOINT ["java", "-jar", "/opt/clas/app.jar"]
