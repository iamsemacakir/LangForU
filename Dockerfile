# ---------- 1) Компилиране на приложението ----------
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
# Тестовете не се компилират тук (по-бързо и по-малко изтегляния); те се пускат в GitHub Actions.
RUN mvn -B -q -Dmaven.test.skip=true package

# ---------- 2) Малък образ само за стартиране ----------
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar

# Настройки за безплатния сървър в Render (512 MB RAM, малко CPU):
# ограничена памет за Java, по-бърз старт, часова зона България (за задачата в 08:00).
ENV JAVA_TOOL_OPTIONS="-Xmx256m -Xss512k -XX:ReservedCodeCacheSize=64m -XX:+UseSerialGC -XX:TieredStopAtLevel=1 -Duser.timezone=Europe/Sofia"
ENV SERVER_TOMCAT_THREADS_MAX=50

EXPOSE 8080
CMD ["java", "-jar", "app.jar"]
