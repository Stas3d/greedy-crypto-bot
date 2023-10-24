FROM eclipse-temurin:17-jdk-focal
VOLUME /tmp
EXPOSE 8080
COPY target/*.jar bot.jar
ENTRYPOINT ["java","-jar","/bot.jar"]
