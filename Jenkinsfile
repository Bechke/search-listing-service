node {
    def repourl = "${REGISTRY_URL}/${PROJECT_ID}/${ARTIFACT_REGISTRY}"
    def targetVmIp = "35.200.196.46"
    def targetVmUser = "bechkedekho"
    def containerName = "search-listing-service"
    def containerPort = "9191"

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

            // Build and push using Jib to GCR
            sh """
                ./gradlew clean jib \
                -Djib.to.image=${repourl}/search-listing-service
            """
        }
    }

    stage('Deploy to VM') {
        withCredentials([sshUserPrivateKey(credentialsId: 'vm-ssh-key', keyFileVariable: 'SSH_KEY')]) {
            // SSH into target VM and deploy container
            sh """
                ssh -o StrictHostKeyChecking=no -i \$SSH_KEY ${targetVmUser}@${targetVmIp} << 'EOF'

                echo "=== Pulling latest image ==="
                docker pull ${repourl}/search-listing-service

                echo "=== Stopping and removing old container if exists ==="
                docker stop ${containerName} || true
                docker rm ${containerName} || true

                echo "=== Running new container ==="
                docker run -d --name ${containerName} \\
                    --restart unless-stopped \\
                    -p ${containerPort}:${containerPort} \\
                    -e SPRING_PROFILES_ACTIVE=dev \\
                    -e SPRING_DATASOURCE_URL=jdbc:mysql://localhost:3306/search_listing_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC \\
                    -e SPRING_DATASOURCE_USERNAME=search_listing_user \\
                    -e SPRING_DATASOURCE_PASSWORD=search_listing_db_pass \\
                    -e SPRING_KAFKA_BOOTSTRAP_SERVERS=localhost:9092 \\
                    -e SPRING_ZIPKIN_BASE_URL=http://localhost:9411 \\
                    ${repourl}/search-listing-service

                echo "=== Deployment complete. Container running on port ${containerPort} ==="

                docker ps --filter "name=${containerName}"

                EOF
            """
        }
    }
}
