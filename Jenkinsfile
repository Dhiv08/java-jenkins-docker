pipeline {
  agent any

tools {
  jdk   'jdk17'
  maven 'Maven_3.9.13'
}

  parameters {
    booleanParam(name: 'SKIP_PUSH', defaultValue: false, description: 'Skip pushing image to Harbor')
    booleanParam(name: 'SKIP_K8S_DEPLOY', defaultValue: false, description: 'Skip Kubernetes deploy')

    string(name: 'K8S_NAMESPACE',  defaultValue: 'default',          description: 'Kubernetes namespace')
    string(name: 'K8S_DEPLOYMENT', defaultValue: 'basic-java-hello', description: 'Deployment name')
    string(name: 'K8S_CONTAINER',  defaultValue: 'basic-java-hello', description: 'Container name in deployment')
  }

  environment {
    HARBOR_REGISTRY = 'occ-harbor.oraclecorp.com'
    HARBOR_PROJECT  = 'brm12ps8'          // <-- change if your Harbor project differs
    IMAGE_REPO      = 'basic-java-hello'  // <-- change if your Harbor repo differs
    IMAGE_TAG       = "${BUILD_NUMBER}"
    IMAGE_REF       = "${HARBOR_REGISTRY}/${HARBOR_PROJECT}/${IMAGE_REPO}:${IMAGE_TAG}"

    HARBOR_CREDS_ID = 'harbor-dhivyg-robot'
    LOCAL_CONTAINER_NAME = "smoke-${BUILD_NUMBER}"
  }

  options { timestamps() }

  stages {
    stage('Precheck (Docker + Kubectl)') {
      steps {
        bat """
          where docker
          docker version
          where kubectl
          kubectl config current-context
          kubectl get nodes
        """
      }
    }

    stage('Login to Oracle Harbor') {
      steps {
        withCredentials([usernamePassword(credentialsId: "${HARBOR_CREDS_ID}",
                                          usernameVariable: 'H_USER',
                                          passwordVariable: 'H_PASS')]) {
          bat """
            echo %H_PASS% | docker login %HARBOR_REGISTRY% -u %H_USER% --password-stdin
          """
        }
      }
    }

    stage('Build JAR') {
      steps {
        bat """
          mvn -v
          mvn -B -DskipTests clean package
          dir target
        """
      }
    }

    stage('Build Docker Image') {
      steps {
        bat """
          echo Building %IMAGE_REF%
          docker build -t %IMAGE_REF% .
        """
      }
    }

    stage('Smoke Test (local)') {
      steps {
        bat """
          docker rm -f %LOCAL_CONTAINER_NAME% 2>nul
          docker run --name %LOCAL_CONTAINER_NAME% --rm %IMAGE_REF%
        """
      }
    }

    stage('Push to Oracle Harbor') {
      when { expression { return !params.SKIP_PUSH } }
      steps {
        bat """
          docker push %IMAGE_REF%
        """
      }
    }

    stage('Deploy to K8s (Rancher Desktop)') {
      when {
        allOf {
          expression { return !params.SKIP_K8S_DEPLOY }
          expression { return !params.SKIP_PUSH }
        }
      }
      steps {
        bat """
          if exist k8s\\ (
            kubectl -n ${params.K8S_NAMESPACE} apply -f k8s\\
          ) else (
            echo No k8s\\ folder found. Create k8s\\deployment.yaml and k8s\\service.yaml
            exit /b 1
          )

          kubectl -n ${params.K8S_NAMESPACE} set image deployment/${params.K8S_DEPLOYMENT} ${params.K8S_CONTAINER}=%IMAGE_REF%
          kubectl -n ${params.K8S_NAMESPACE} rollout status deployment/${params.K8S_DEPLOYMENT} --timeout=180s
          kubectl -n ${params.K8S_NAMESPACE} get pods -o wide
        """
      }
    }
  }

  post {
    always {
      bat(returnStatus: true, script: """docker rm -f %LOCAL_CONTAINER_NAME% 2>nul""")
      bat(returnStatus: true, script: """docker logout %HARBOR_REGISTRY%""")
    }
  }
}
