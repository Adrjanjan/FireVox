FROM openjdk:17

EXPOSE 8083

RUN mkdir /app

COPY target/main-*-exec.jar /app/main.jar

RUN chmod 777 /app/main.jar

ENTRYPOINT ["java", "-XX:+UnlockExperimentalVMOptions", "-jar","/app/main.jar"]
