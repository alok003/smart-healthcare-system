FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
COPY target/*.jar app.jar
EXPOSE 8012
ENTRYPOINT ["java", "-jar", "app.jar"]
