FROM eclipse-temurin:17-jdk

WORKDIR /usr/src/app

COPY . .

RUN javac -cp lib/sqlite-jdbc.jar -d out src/*.java src/model/*.java src/dao/*.java src/db/*.java
RUN jar --create --file MercadoApp.jar -C out .

EXPOSE 8080

CMD ["java", "-cp", "MercadoApp.jar:lib/sqlite-jdbc.jar", "WebServer"]
