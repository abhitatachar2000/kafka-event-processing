FROM eclipse-temurin:25-jre-alpine

ARG JAR_FILE=target/*.jar
COPY ${JAR_FILE} app.jar

EXPOSE 8005
ENTRYPOINT ["java","-jar","/app.jar"]
