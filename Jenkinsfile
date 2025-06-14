node {
    def repourl = "${REGISTRY_URL}/${PROJECT_ID}/${ARTIFACT_REGISTRY}"
    def targetVmIp = "35.200.196.46"
    def vmUser = "bechkedekho"
    def remoteAppPort = "9191"

    stage('Checkout') {
        checkout([$class: 'GitSCM',
            branches: [[name: '*/main']],
            extensions: [],
            userRemoteConfigs: [[
                credentialsId: 'git',
                url: 'https://github.com/Bechke/search-listing-service.git'
            ]]
        ])
    }

    stage('Set Permissions') {
        sh 'chmod +x ./gradlew'
    }

    stage('Build and Push Image') {
        withCredentials([file(credentialsId: 'gcp', variable: 'GC_KEY')]) {
            sh """
                echo "=== Authenticating with GCP ==="
                gcloud auth activate-service-account --key-file=${GC_KEY}
                gcloud auth configure-docker asia-south1-docker.pkg.dev

                echo "=== Building Spring Boot JAR ==="
                ./gradlew clean bootJar -x test

                echo "=== Building Docker Image ==="
                docker build -t ${repourl}/search-listing-service .

                echo "=== Pushing Docker Image to Artifact Registry ==="
                docker push ${repourl}/search-listing-service
            """
        }
    }

    stage('Deploy to VM') {
        withCredentials([sshUserPrivateKey(credentialsId: 'vm-ssh-key', keyFileVariable: 'SSH_KEY_PATH')]) {
            sh """
                ssh -o StrictHostKeyChecking=no -i "$SSH_KEY_PATH" "$vmUser@$targetVmIp" bash -s <<'ENDSSH'
                    echo "=== Creating Docker network if it doesn't exist ==="
                    docker network inspect bechke-network >/dev/null 2>&1 || docker network create bechke-network

                    echo "=== Stopping and removing existing container (if any) ==="
                    docker stop search-listing-service >/dev/null 2>&1 || true
                    docker rm search-listing-service >/dev/null 2>&1 || true

                    echo "=== Pulling latest image ==="
                    docker pull ${repourl}/search-listing-service

                    echo "=== Running new container on bechke-network ==="
                    docker run -d \
                        --name search-listing-service \
                        --restart unless-stopped \
                        --network bechke-network \
                        -p ${remoteAppPort}:9191 \
                        ${repourl}/search-listing-service

                    echo "=== Deployment complete. Running containers: ==="
                    docker ps --filter "name=search-listing-service"
ENDSSH
            """
        }
    }
}
