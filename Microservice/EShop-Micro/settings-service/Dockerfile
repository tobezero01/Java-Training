FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY . .

# RUN mvn -q -DskipTests -pl settings-service -am package spring-boot:repackage
RUN mvn -q -DskipTests -pl settings-service -am package

FROM eclipse-temurin:17-jre
WORKDIR /opt/app
COPY --from=build /app/settings-service/target/settings-service-*.jar app.jar
EXPOSE 9005
ENTRYPOINT ["java","-jar","/opt/app/app.jar","--spring.profiles.active=docker","--server.port=9005"]
