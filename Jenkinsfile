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
            sh "gcloud auth activate-service-account --key-file=${GC_KEY}"
            sh "gcloud auth configure-docker asia-south1-docker.pkg.dev"

            sh """
                export REPO_URL=${repourl}
                ./gradlew clean bootJar -x test jib
            """
        }
    }

    stage('Deploy to VM') {
        withCredentials([sshUserPrivateKey(credentialsId: 'vm-ssh-key', keyFileVariable: 'SSH_KEY_PATH')]) {
            sh """
                ssh -o StrictHostKeyChecking=no -i "$SSH_KEY_PATH" "$vmUser@$targetVmIp" bash -s <<'ENDSSH'
                    echo "=== Stopping and removing existing container (if any) ==="
                    docker stop search-listing-service || true
                    docker rm search-listing-service || true

                    echo "=== Pulling latest image ==="
                    docker pull ${repourl}/search-listing-service

                    echo "=== Running new container ==="
                    docker run -d --name search-listing-service --restart unless-stopped -p ${remoteAppPort}:9191 ${repourl}/search-listing-service

                    echo "=== Deployment complete. Container running on port ${remoteAppPort} ==="
                    docker ps --filter "name=search-listing-service"
ENDSSH
            """
        }
    }
}
