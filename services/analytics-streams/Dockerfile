FROM eclipse-temurin:25-jre

ARG JAR_FILE=target/*.jar
COPY ${JAR_FILE} app.jar

EXPOSE 8008
ENTRYPOINT ["java","-jar","/app.jar"]
