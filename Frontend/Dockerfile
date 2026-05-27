FROM node:20-alpine AS builder

WORKDIR /app

COPY package*.json ./
COPY package-lock.json* ./

RUN npm ci && npm cache clean --force

COPY . .

RUN npm run build

FROM nginx:alpine

# Копируем собранные файлы в правильную директорию
COPY --from=builder /app/dist /usr/share/nginx/html

# Копируем конфиг nginx
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]