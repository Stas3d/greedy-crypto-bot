FROM eclipse-temurin:17-jdk-focal
COPY target/greed-bot-1.0.0.jar greed-bot-1.0.0.jar
ENTRYPOINT ["java","-jar","/greed-bot-1.0.0.jar"]

