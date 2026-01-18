FROM amazoncorretto:21-alpine

WORKDIR /app

COPY ../.mvn .mvn

COPY ../pom.xml .
COPY ../mvnw .
COPY src ./src

RUN chmod +x mvnw && ./mvnw clean package -DskipTests

ENTRYPOINT ["java", "-jar", "target/my-market-app-0.0.1-SNAPSHOT.jar"]