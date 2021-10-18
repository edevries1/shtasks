FROM openjdk:11

WORKDIR /app/

ADD ./target/scala-2.13/assignment-assembly-0.1.jar /app/

ENTRYPOINT ["java", "-jar", "assignment-assembly-0.1.jar"]
