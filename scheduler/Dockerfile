FROM openjdk:17

EXPOSE 8083

RUN mkdir /app

COPY target/scheduler-0.0.1-SNAPSHOT-exec.jar /app/scheduler.jar
COPY kubernetes/firevox-8a69e6d70dcb.json /app/auth.json

RUN chown 777 /app/scheduler.jar

ENTRYPOINT ["java", "-XX:+UnlockExperimentalVMOptions", "-jar","/app/scheduler.jar"]
