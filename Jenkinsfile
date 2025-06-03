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

            // Build and push using Jib to GCR
            sh """
                ./gradlew clean jib \
                -Djib.to.image=${repourl}/search-listing-service
            """
        }
    }

    stage('Deploy') {
        // Replace IMAGE_URL with actual base URL in deployment.yaml
        sh "sed -i 's|IMAGE_URL|${repourl}|g' k8s/deployment.yaml"

        step([$class: 'KubernetesEngineBuilder',
            projectId: env.PROJECT_ID,
            clusterName: env.CLUSTER,
            location: env.REGION,
            manifestPattern: 'k8s/deployment.yaml',
            credentialsId: env.PROJECT_ID,
            verifyDeployments: true
        ])
    }
}
