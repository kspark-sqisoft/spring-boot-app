# --- Build ---
FROM eclipse-temurin:17-jdk AS builder
WORKDIR /app
COPY gradlew build.gradle settings.gradle ./
COPY gradle gradle
COPY src src
RUN chmod +x gradlew && ./gradlew bootJar --no-daemon -x test

# --- Run ---
FROM eclipse-temurin:17-jre
WORKDIR /app
RUN apt-get update \
	&& apt-get install -y --no-install-recommends curl \
	&& rm -rf /var/lib/apt/lists/* \
	&& groupadd --system spring \
	&& useradd --system --gid spring --no-create-home spring
COPY --from=builder /app/build/libs/*.jar app.jar
USER spring
EXPOSE 3000
ENV SERVER_PORT=3000
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
