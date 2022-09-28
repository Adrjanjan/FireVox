FROM openjdk:17

EXPOSE 8080

RUN mkdir /app

COPY target/Voxel-0.0.1-SNAPSHOT-exec.jar /app/Voxel.jar
RUN chown 777 /app/Voxel.jar

ENTRYPOINT ["java", "-XX:+UnlockExperimentalVMOptions", "-jar","/app/Voxel.jar"]
