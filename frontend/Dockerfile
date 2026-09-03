# 构建上下文必须为仓库根目录：docker build -f frontend/Dockerfile .
FROM node:22.14-alpine AS build
WORKDIR /workspace/frontend

COPY frontend/package.json frontend/package-lock.json ./
RUN npm ci

COPY frontend ./
ARG VITE_API_BASE_URL=/api
ENV VITE_API_BASE_URL=$VITE_API_BASE_URL
RUN npm run build

FROM nginx:1.27-alpine
# Compose 保持连接单体后端；Kubernetes 构建时通过参数改为服务网关。
ARG API_UPSTREAM=backend:8080
ENV API_UPSTREAM=$API_UPSTREAM
COPY frontend/nginx.conf /etc/nginx/templates/default.conf.template
COPY --from=build /workspace/frontend/dist /usr/share/nginx/html
EXPOSE 80
HEALTHCHECK --interval=15s --timeout=5s --retries=6 \
  CMD wget -qO- http://127.0.0.1/ >/dev/null || exit 1
