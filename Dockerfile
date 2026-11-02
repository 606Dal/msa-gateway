FROM eclipse-temurin:17-jdk

# 이미지 안에서 시작할 경로
WORKDIR /app

COPY . .

# 실행 권한 부여
RUN chmod +x gradlew && ./gradlew bootJar -x test

EXPOSE 8000

CMD ["java", "-jar", "build/libs/Gateway-0.0.1-SNAPSHOT.jar"]