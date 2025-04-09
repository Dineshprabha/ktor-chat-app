FROM gradle:7.6.1-jdk17 as builder
WORKDIR /app
COPY . .
RUN gradle build -x test

FROM openjdk:17
WORKDIR /app
COPY --from=builder /app/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
