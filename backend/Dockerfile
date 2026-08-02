FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace
COPY backend/gradlew gradlew
COPY backend/gradle gradle
COPY backend/build.gradle ./
RUN sed -i 's/\r$//' gradlew && chmod +x gradlew
RUN ./gradlew dependencies --no-daemon || true
COPY backend/src src
RUN ./gradlew clean bootJar --no-daemon

FROM eclipse-temurin:21-jre
WORKDIR /app
RUN apt-get update \
    && apt-get install -y --no-install-recommends chromium chromium-driver \
    && rm -rf /var/lib/apt/lists/* \
    && useradd --system --uid 10001 uniwiki
COPY --from=build /workspace/build/libs/*.jar app.jar
COPY ai/data/normalized/sejong/software-course-schedules.json /app/data/software-course-schedules.json
USER uniwiki
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
