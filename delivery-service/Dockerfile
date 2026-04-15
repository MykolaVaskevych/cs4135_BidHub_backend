# Build context: backend/
# docker build -f delivery-service/Dockerfile -t bidhub-delivery .
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /workspace
COPY . .
RUN if [ -d backend ] && [ -f backend/mvnw ]; then cp -r backend/. . ; fi && \
    chmod +x mvnw && ./mvnw -pl delivery-service -am package -DskipTests -q

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /workspace/delivery-service/target/*.jar app.jar
EXPOSE 8088
ENTRYPOINT ["java", "-jar", "app.jar"]
