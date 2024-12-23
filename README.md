#prerequisites installations
1. Java 23
2. Docker desktop
3. Minikube
4. kubectl

#Start app locally with MySql image
docker-compose up --build

#Stop App and Mysql image
docker-compose stop

#Removes App containers, networks and  the volumes will also be removed (all data stored in them will be lost).
docker-compose down -v


#minukube for futue k8s
minikube status
minikube start --driver=docker
minikube image load vyapari-services:latest
minikube service vyapari-services
minikube dashboard

#kubectl for future k8s
kubectl apply -f k8s-deployment.yaml
kubectl get pods
kubectl get deployments
kubectl get svc
kubectl get nodes -o wide
kubectl logs <image-name>


#gradle
./gradlew bootRun -Dspring.profiles.active=dev