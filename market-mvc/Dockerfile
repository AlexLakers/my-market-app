FROM amazoncorretto:21-alpine

WORKDIR /app

COPY . .

RUN chmod +x mvnw && ./mvnw clean install -N -DskipTests

RUN ./mvnw clean package -am -pl market-mvc -DskipTests

ENTRYPOINT ["java", "-jar", "market-mvc/target/market-mvc-0.0.1-SNAPSHOT.jar"]