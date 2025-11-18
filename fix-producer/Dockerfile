FROM eclipse-temurin:21-jdk
WORKDIR /app
COPY target/fix-producer-1.0.0-SNAPSHOT.jar /app/fix-producer.jar
ENTRYPOINT ["java", "-cp", "/app/fix-producer.jar", "com.example.fixproducer.FixProducer"]

