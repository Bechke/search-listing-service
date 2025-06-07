node {
    def repourl = "${REGISTRY_URL}/${PROJECT_ID}/${ARTIFACT_REGISTRY}"
    def VM_USER = "bechkedekho"
    def VM_IP = "35.200.196.46"
    def SSH_KEY_ID = "vm-ssh-key"

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

            // Build and push using Jib to GCR
            sh """
                ./gradlew clean jib \
                -Djib.to.image=${repourl}/search-listing-service
            """
        }
    }

    stage('Deploy to VM') {
        withCredentials([
            file(credentialsId: 'gcp', variable: 'GC_KEY'),
            sshUserPrivateKey(credentialsId: SSH_KEY_ID, keyFileVariable: 'SSH_KEY_PATH')
        ]) {

            // Copy service account key to target VM (safe path: /home/${VM_USER})
            sh "scp -o StrictHostKeyChecking=no -i ${SSH_KEY_PATH} ${GC_KEY} ${VM_USER}@${VM_IP}:/home/${VM_USER}/service-account-key.json"

            // SSH and deploy
            def remote = [
                name: 'target-vm',
                host: "${VM_IP}",
                user: "${VM_USER}",
                identityFile: "${SSH_KEY_PATH}",
                allowAnyHosts: true
            ]

            // Run remote deployment steps
            sshCommand remote: remote, command: """
                echo "=== Logging into Artifact Registry ==="
                docker login -u _json_key --password-stdin https://asia-south1-docker.pkg.dev < /home/${VM_USER}/service-account-key.json

                echo "=== Pulling latest image ==="
                docker pull ${repourl}/search-listing-service

                echo "=== Stopping and removing existing container if exists ==="
                docker stop search-listing-service || true
                docker rm search-listing-service || true

                echo "=== Running new container ==="
                docker run -d --name search-listing-service \\
                  --restart unless-stopped \\
                  -p 9191:9191 \\
                  -e SPRING_PROFILES_ACTIVE=dev \\
                  -e SPRING_DATASOURCE_URL=jdbc:mysql://localhost:3306/search_listing_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC \\
                  -e SPRING_DATASOURCE_USERNAME=search_listing_user \\
                  -e SPRING_DATASOURCE_PASSWORD=search_listing_db_pass \\
                  -e SPRING_KAFKA_BOOTSTRAP_SERVERS=localhost:9092 \\
                  -e SPRING_ZIPKIN_BASE_URL=http://localhost:9411 \\
                  ${repourl}/search-listing-service

                echo "=== Deployment complete. Container running on port 9191 ==="

                echo "=== Showing container logs (last 50 lines) ==="
                docker logs --tail 50 search-listing-service
            """
        }
    }
}
