FROM openjdk:17.0.2
VOLUME /tmp
ADD target/campushub-support-*.jar app.jar
ENTRYPOINT ["java","-jar","/app.jar"]
