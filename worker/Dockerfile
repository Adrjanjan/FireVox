FROM openjdk:17

EXPOSE 8081

RUN mkdir /app

COPY target/worker-*-exec.jar /app/worker.jar
RUN chmod 777 /app/worker.jar

ENTRYPOINT ["java", "-XX:+UnlockExperimentalVMOptions", "-jar","/app/worker.jar"]
