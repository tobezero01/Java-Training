FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY . .
RUN mvn -q -DskipTests -pl shipping-service -am package

FROM eclipse-temurin:17-jre
WORKDIR /opt/app
COPY --from=build /app/shipping-service/target/shipping-service-*.jar app.jar
EXPOSE 9006
ENTRYPOINT ["java","-jar","/opt/app/app.jar","--spring.profiles.active=docker","--server.port=9006"]
