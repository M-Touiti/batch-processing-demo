FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app
COPY . .
RUN ./mvnw clean package -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=builder /app/exposition/target/*.jar app.jar
RUN mkdir -p /batch-input /batch-output
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
