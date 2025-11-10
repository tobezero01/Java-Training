FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY . .
RUN mvn -q -DskipTests -pl ecom-eureka-server -am package

FROM eclipse-temurin:17-jre
WORKDIR /opt/app
COPY --from=build /app/ecom-eureka-server/target/ecom-eureka-server-*.jar app.jar
EXPOSE 8761
ENTRYPOINT ["java","-jar","/opt/app/app.jar","--spring.profiles.active=docker","--server.port=8761"]
