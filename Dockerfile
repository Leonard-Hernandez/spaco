FROM amazoncorretto:25-alpine AS builder

WORKDIR /app/spaco

COPY gradle ./gradle
COPY gradlew .
COPY build.gradle settings.gradle ./
RUN chmod +x gradlew && sed -i 's/\r$//' gradlew
RUN ./gradlew dependencies --no-daemon || true

COPY ./src ./src

RUN ./gradlew clean bootJar -x test --no-daemon

FROM eclipse-temurin:25-jre-alpine

RUN addgroup -S spaco && adduser -S -G spaco -H -s /sbin/nologin spaco \
    && mkdir -p /data/uploads && chown spaco:spaco /data/uploads

WORKDIR /app

COPY --from=builder --chown=spaco:spaco /app/spaco/build/libs/app.jar .

USER spaco

# Photos: public catalogue and private clinical ones (mount a volume here)
VOLUME /data/uploads

EXPOSE 8092

ENTRYPOINT [ "java", "-jar", "app.jar" ]
