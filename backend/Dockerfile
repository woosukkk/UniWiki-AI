FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace
COPY gradlew gradlew
COPY gradle gradle
COPY build.gradle ./
RUN sed -i 's/\r$//' gradlew && chmod +x gradlew
RUN ./gradlew dependencies --no-daemon || true
COPY src src
RUN ./gradlew clean bootJar --no-daemon

FROM eclipse-temurin:21-jre
WORKDIR /app
RUN useradd --system --uid 10001 uniwiki
COPY --from=build /workspace/build/libs/*.jar app.jar
USER uniwiki
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
