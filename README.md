#prerequisites installations
1. Java 23
2. Docker desktop
3. Minikube
4. kubectl

#Start app locally
docker-compose up --build

#Stop App
docker-compose stop

#Removes containers, networks and  the volumes will also be removed (all data stored in them will be lost).
docker-compose down -v


#minukube
minikube status
minikube start --driver=docker
minikube image load vyapari-services:latest
minikube service vyapari-services
minikube dashboard

#kubectl
kubectl apply -f k8s-deployment.yaml
kubectl get pods
kubectl get deployments
kubectl get svc
kubectl get nodes -o wide
kubectl logs <image-name>
