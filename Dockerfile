FROM node:22-alpine AS frontend
WORKDIR /app/src/main/frontend-realisasi
COPY src/main/frontend-realisasi/package.json src/main/frontend-realisasi/package-lock.json ./
RUN npm ci --no-audit --no-fund
COPY src/main/frontend-realisasi ./
# angular.json menulis hasil build ke ../resources/static = /app/src/main/resources/static
RUN npm run build

FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -q -B dependency:go-offline
COPY src src
COPY --from=frontend /app/src/main/resources/static src/main/resources/static
RUN mvn -q -B package -DskipTests -Dfrontend.skip=true

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/backend-realisasi-*.jar app.jar
ENV SPRING_PROFILES_ACTIVE=prod
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
