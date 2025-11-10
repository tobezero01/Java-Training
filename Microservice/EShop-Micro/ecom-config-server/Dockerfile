FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY . .
RUN mvn -q -DskipTests -pl ecom-config-server -am package

FROM eclipse-temurin:17-jre
WORKDIR /opt/app
COPY --from=build /app/ecom-config-server/target/ecom-config-server-*.jar app.jar
EXPOSE 8888
ENTRYPOINT ["java","-jar","/opt/app/app.jar","--spring.profiles.active=native,docker","--server.port=8888"]
