### Build stage
FROM maven:3.9.9-eclipse-temurin-17-alpine AS build
WORKDIR /workspace
COPY pom.xml .
COPY src ./src
# -DskipTests still compiles tests; use maven.test.skip to skip test compilation too.
RUN mvn -q -Dmaven.test.skip=true package

### Runtime stage (non-root)
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
RUN addgroup -S app && adduser -S app -G app
COPY --from=build /workspace/target/*.jar /app/app.jar
USER app

# Cloud Run will inject PORT; app also defaults to 8081 for local dev.
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "/app/app.jar"]