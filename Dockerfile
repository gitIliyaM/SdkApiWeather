FROM openjdk:26-ea-21-jdk-slim
VOLUME /app
COPY build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app.jar"]