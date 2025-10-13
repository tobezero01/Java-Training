# Dockerfile

# ====== Stage 1: Build bằng Maven (JDK đầy đủ) ======
FROM maven:3.9.6-eclipse-temurin-17 AS builder
WORKDIR /workspace

# Copy pom và download dependencies sớm để cache layer
COPY pom.xml .
RUN mvn -q -e -DskipTests dependency:go-offline

# Copy source và build
COPY src ./src
RUN mvn -q -DskipTests clean package

# ====== Stage 2: Runtime (JRE) gọn nhẹ ======
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=builder /workspace/target/eshop-demo-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/app.jar"]
