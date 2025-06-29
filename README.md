# prerequisites installations
1. Java 21
2. Docker desktop
3. Minikube(optional)
4. kubectl(optional)

# gradle
./gradlew clean build --refresh-dependencies -x test
./gradlew build
./gradlew dependencies #check for dependencies conflict
./gradlew build --continuous #continuous build feature to automatically rebuild when files change.
./gradlew bootRun -Dspring.profiles.active=dev


# Docker
# docker build -t vyapari-services:latest .
docker-compose up --build
# optional
docker images
docker run -p 9191:9191 vyapari-services:latest

# Stop App and Mysql image
docker-compose stop

# Removes App containers, networks and  the volumes will also be removed (all data stored in them will be lost).
docker-compose down -v
