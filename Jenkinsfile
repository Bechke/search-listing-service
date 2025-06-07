node {
    def repourl = "${REGISTRY_URL}/${PROJECT_ID}/${ARTIFACT_REGISTRY}"

    stage('Checkout') {
        checkout([$class: 'GitSCM',
            branches: [[name: '*/main']],
            extensions: [],
            userRemoteConfigs: [[
                credentialsId: 'git',
                url: 'https://github.com/Bechke/search-listing-service.git' // update if needed
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

            // Build and push using Jib to GCR / Artifact Registry
            sh """
                ./gradlew clean jib \
                -Djib.to.image=${repourl}/search-listing-service
            """
        }
    }

    stage('Deploy to VM') {
        withCredentials([sshUserPrivateKey(credentialsId: 'vm-ssh-key', keyFileVariable: 'SSH_KEY'),
                         file(credentialsId: 'gcp', variable: 'GC_KEY')]) {

            // Copy the service account key to the target VM temporarily
            sh """
                scp -o StrictHostKeyChecking=no -i \$SSH_KEY \$GC_KEY bechkedekho@35.200.196.46:/tmp/service-account-key.json
            """

            // Now deploy
            sh """
                ssh -o StrictHostKeyChecking=no -i \$SSH_KEY bechkedekho@35.200.196.46 << 'EOF'

                echo "=== Authenticating gcloud & Artifact Registry ==="
                gcloud auth activate-service-account --key-file=/tmp/service-account-key.json
                gcloud auth configure-docker asia-south1-docker.pkg.dev

                echo "=== Pulling latest image ==="
                docker pull ${repourl}/search-listing-service

                echo "=== Stopping and removing old container if exists ==="
                docker stop search-listing-service || true
                docker rm search-listing-service || true

                echo "=== Running new container ==="
                docker run -d --name search-listing-service \\
                    --restart unless-stopped \\
                    -p 9191:9191 \\
                    -e SPRING_PROFILES_ACTIVE=dev \\
                    -e SPRING_DATASOURCE_URL="jdbc:mysql://localhost:3306/search_listing_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC" \\
                    -e SPRING_DATASOURCE_USERNAME="search_listing_user" \\
                    -e SPRING_DATASOURCE_PASSWORD="search_listing_db_pass" \\
                    -e SPRING_KAFKA_BOOTSTRAP_SERVERS="localhost:9092" \\
                    -e SPRING_ZIPKIN_BASE_URL="http://localhost:9411" \\
                    ${repourl}/search-listing-service

                echo "=== Deployment complete. Container running on port 9191 ==="
                docker ps --filter "name=search-listing-service"

                echo "=== Cleaning up temporary key ==="
                rm -f /tmp/service-account-key.json

EOF
            """
        }
    }
}
