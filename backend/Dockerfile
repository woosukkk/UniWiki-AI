FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace
COPY backend/gradlew gradlew
COPY backend/gradle gradle
COPY backend/build.gradle ./
RUN sed -i 's/\r$//' gradlew && chmod +x gradlew
RUN ./gradlew dependencies --no-daemon || true
COPY backend/src src
RUN ./gradlew clean bootJar --no-daemon

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN apk add --no-cache chromium chromium-chromedriver \
    && addgroup -S uniwiki \
    && adduser -S -u 10001 -h /home/uniwiki -G uniwiki uniwiki \
    && mkdir -p /home/uniwiki/.cache/selenium \
    && chown -R uniwiki:uniwiki /home/uniwiki \
    && test -x /usr/bin/chromium \
    && test -x /usr/bin/chromedriver
COPY --from=build /workspace/build/libs/*.jar app.jar
COPY ai/data/normalized/sejong/software-course-schedules.json /app/data/software-course-schedules.json
USER uniwiki
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
