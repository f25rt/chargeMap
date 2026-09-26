# ---- Frontend: build static assets, serve + reverse-proxy via Nginx ----
# Stage 1: build the Vite app.
FROM node:20-alpine AS build
WORKDIR /app

COPY package.json package-lock.json* ./
RUN npm ci

COPY . .
# VITE_MAPBOX_TOKEN is baked in at build time (Vite inlines VITE_* env vars).
ARG VITE_MAPBOX_TOKEN
ENV VITE_MAPBOX_TOKEN=$VITE_MAPBOX_TOKEN
RUN npm run build

# Stage 2: Nginx serving the static build + proxying /api and /actuator to the backend.
FROM nginx:1.27-alpine AS runtime
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /usr/share/nginx/html
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
