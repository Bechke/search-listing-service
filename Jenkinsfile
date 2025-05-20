node {
    def repourl = "${REGISTRY_URL}/${PROJECT_ID}/${ARTIFACT_REGISTRY}"
    def gradleCMD = "chmod +x ./gradlew"

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

    stage('Build and Push Image') {
        withCredentials([file(credentialsId: 'gcp', variable: 'GC_KEY')]) {
            sh "gcloud auth activate-service-account --key-file=${GC_KEY}"
            sh 'gcloud auth configure-docker asia-south1-docker.pkg.dev'
            sh "${gradleCMD} clean jib --image=${repourl}"
        }
    }

    stage('Deploy') {
        sh "sed -i 's|IMAGE_URL|${repourl}|g' k8s/deployment.yaml"
        step([$class: 'KubernetesEngineBuilder',
            projectId: env.PROJECT_ID,
            clusterName: env.CLUSTER,
            location: env.REGION,
            manifestPattern: 'k8s/deployment.yaml',
            credentialsId: '6697f859-7094-4ea7-bdb4-833db844c250',
            verifyDeployments: true
        ])
    }
}
