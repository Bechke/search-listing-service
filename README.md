# prerequisites installations
1. Java 23
2. Docker desktop
3. Minikube
4. kubectl

# gradle
./gradlew clean build --refresh-dependencies -x test
./gradlew build
./gradlew dependencies #check for dependencies conflict
./gradlew build --continuous #continuous build feature to automatically rebuild when files change.
./gradlew bootRun -Dspring.profiles.active=dev

# For docker 
# Start app locally with MySql image
docker build -t vyapari-services:latest .
docker-compose up --build
# optional
docker images
docker run -p 8080:8080 vyapari-services:latest 

# Stop App and Mysql image
docker-compose stop

# Removes App containers, networks and  the volumes will also be removed (all data stored in them will be lost).
docker-compose down -v


# minukube for local k8s
minikube status
minikube start --driver=docker
eval $(minikube docker-env)  #Whenever we define image tag in k8s configuration it should pull from local system.
minikube -p minikube docker-env
minikube stop
minikube image load vyapari-services:latest
minikube service vyapari-services
minikube dashboard
minikube delete
minikube ip
minikube service vschart-k8schart --url
minikube addons enable ingress
minikube tunnel (after updating /etc/hosts config file)

# kubectl for future k8s
kubectl apply -f k8s-deployment.yaml
kubectl get pods
kubectl get deployments
kubectl get svc
kubectl get nodes -o wide
kubectl logs <image-name>
kubectl delete deployment vyapari-services --If you've made changes, delete and recreate the deployment:
kubectl exec -it mysql-7dc4647959-zlrvd -- /bin/sh
kubectl get pods -n ingress-nginx (after enabling ingress)

#kompose
kompose --file compose.yaml convert
kompose -f first.yaml -f second.yaml convert

#Access Db from cli
mysql -h mysql -u vyapari_user -p vyapari_pass

#Helm
helm create k8scharts (create a config for k8s)
helm install vschart k8scharts
helm upgrade vschart k8scharts (after changes made to chart)
helm template k8scharts (all config files information)
helm list (chart version defined in chart.yaml which is appVersion and version)