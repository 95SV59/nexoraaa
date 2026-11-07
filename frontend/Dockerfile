# =============================================================================
# Stage 1: Build React/Vite Static Assets
# =============================================================================
FROM node:20-alpine AS builder
WORKDIR /app

COPY package*.json ./
RUN npm ci

COPY . .

# Build production assets (VITE_API_BASE_URL and VITE_WS_URL injected at build time)
ARG VITE_API_BASE_URL
ARG VITE_WS_URL
ENV VITE_API_BASE_URL=$VITE_API_BASE_URL
ENV VITE_WS_URL=$VITE_WS_URL

RUN npm run build

# =============================================================================
# Stage 2: Serve via Nginx
# =============================================================================
FROM nginx:alpine
COPY --from=builder /app/dist /usr/share/nginx/html

# SPA Routing: Fall back to index.html for client-side routing
RUN echo 'server { \
    listen 80; \
    server_name localhost; \
    location / { \
        root /usr/share/nginx/html; \
        index index.html index.htm; \
        try_files $uri $uri/ /index.html; \
    } \
}' > /etc/nginx/conf.d/default.conf

EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
