FROM amazoncorretto:21-alpine

WORKDIR /app

COPY . .

RUN chmod +x mvnw && ./mvnw clean install -N -DskipTests

RUN ./mvnw clean package -am -pl payment-service -DskipTests

ENTRYPOINT ["java", "-jar", "payment-service/target/payment-service-0.0.1-SNAPSHOT.jar"]