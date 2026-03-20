# Sử dụng JDK 21 làm base image (phù hợp với cấu hình pom.xml của bạn)
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app

# Copy wrapper và file cấu hình maven
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .

# Cấp quyền thực thi cho file wrapper
RUN chmod +x ./mvnw

# Copy toàn bộ mã nguồn
COPY src src

# Build project thành file JAR (bỏ qua chạy test để build nhanh hơn)
RUN ./mvnw clean package -DskipTests

# Run app
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
# Copy file JAR từ stage build
COPY --from=build /app/target/*.jar app.jar

# Cổng mặc định của Spring Boot
EXPOSE 8080

ENTRYPOINT ["java","-jar","app.jar"]
