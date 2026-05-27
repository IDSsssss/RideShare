FROM node:20-alpine AS builder

WORKDIR /app

COPY package*.json ./
COPY package-lock.json* ./

RUN npm ci && npm cache clean --force

COPY . .

RUN npm run build

FROM nginx:alpine

# Устанавливаем wget (он легче curl)
RUN apk add --no-cache wget

COPY --from=builder /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80

# Healthcheck с wget
HEALTHCHECK --interval=10s --timeout=5s --start-period=10s --retries=5 \
    CMD wget --no-verbose --tries=1 --spider http://localhost/ || exit 1

CMD ["nginx", "-g", "daemon off;"]