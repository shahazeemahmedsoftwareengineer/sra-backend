FROM amazoncorretto:17-alpine

WORKDIR /app

COPY gradlew .
COPY gradle gradle
COPY build.gradle.kts .
COPY settings.gradle.kts .
COPY src src

RUN chmod +x gradlew
RUN ./gradlew installDist --no-daemon

EXPOSE 8080

CMD ["./build/install/sra-backend/bin/sra-backend"]
