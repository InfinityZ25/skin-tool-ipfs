FROM openjdk:17.0.1-oraclelinux8

ARG JAR_FILE=build/libs/skin-0.0.1-SNAPSHOT.jar
COPY ${JAR_FILE} app.jar

ENTRYPOINT ["java","-jar","/app.jar"]
