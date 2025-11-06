# 1단계 빌드
FROM eclipse-temurin:17-jdk AS stage1
WORKDIR /app

COPY gradle gradle
COPY src src
COPY build.gradle .
COPY settings.gradle .
COPY gradlew .

RUN sed -i 's/\r$//' gradlew && chmod +x gradlew
RUN ./gradlew clean bootJar -x test --no-daemon

# 2단계 (경량 런타임)
FROM eclipse-temurin:17-jre AS stage2
WORKDIR /app
COPY --from=stage1 /app/build/libs/*.jar app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]
