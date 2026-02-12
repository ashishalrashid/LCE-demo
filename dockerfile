# ---------- BUILD STAGE ----------
FROM maven:3.9.6-eclipse-temurin-21 AS build

WORKDIR /app
COPY . .

# Build shaded executable jar
RUN mvn clean package -DskipTests


# ---------- RUNTIME STAGE ----------
FROM eclipse-temurin:21-jre

WORKDIR /app

# Copy executable jar
COPY --from=build /app/target/lexcorpus-engine-0.1.0.jar app.jar

# Copy index and demo documents
COPY data data
COPY src/main/resources/static src/main/resources/static

EXPOSE 8080

CMD ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]

# JAVA_OPTS=-Xms128m -Xmx320m -XX:+UseSerialGC

