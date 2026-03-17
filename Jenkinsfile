pipeline {
  agent any
  tools { maven 'Maven_3.9.13' }

  environment {
    // Repo name in Harbor will be the same as APP_NAME
    APP_NAME = 'java-jenkins-app'

    // Your Harbor values (from screenshot)
    HARBOR_REGISTRY = 'occ-harbor.oraclecorp.com'
    HARBOR_PROJECT  = 'dhivyg'

    // Must match what your Java prints
    EXPECTED_LOG = 'Hello from Java app running in Docker via Jenkins!'
  }

  stages {
    stage('Checkout') {
      steps { checkout scm }
    }

    stage('Build JAR') {
      steps {
        bat '''
          mvn -v
          mvn -U -DskipTests clean package
          dir target
        '''
      }
    }

    stage('Build Docker Image') {
      steps {
        script {
          // Local image name (on Jenkins machine)
          env.IMG_LOCAL  = "${env.APP_NAME}:${env.BUILD_NUMBER}"

          // Remote image name (Harbor)
          env.IMG_REMOTE = "${env.HARBOR_REGISTRY}/${env.HARBOR_PROJECT}/${env.APP_NAME}:${env.BUILD_NUMBER}"
        }
        bat """
          docker version
          docker build -t %IMG_LOCAL% .
          docker images | findstr %APP_NAME%
        """
      }
    }

    stage('Run & Verify Java Output') {
      steps {
        bat """
          docker rm -f %APP_NAME% 2>nul

          docker run -d --name %APP_NAME% %IMG_LOCAL%

          timeout /t 3 /nobreak >nul

          docker logs --tail 200 %APP_NAME%
          docker logs --tail 200 %APP_NAME% | findstr /C:"%EXPECTED_LOG%"
          if errorlevel 1 (
            echo Expected output not found. Failing build; will NOT push to Harbor.
            exit /b 1
          )
        """
      }
    }

    stage('Push to Harbor') {
      steps {
        withCredentials([usernamePassword(
          credentialsId: 'harbor-dhivyg-robot',   // <-- must match your Jenkins credential ID
          usernameVariable: 'H_USER',
          passwordVariable: 'H_PASS'
        )]) {
          bat """
            echo %H_PASS% | docker login %HARBOR_REGISTRY% -u %H_USER% --password-stdin

            docker tag %IMG_LOCAL% %IMG_REMOTE%
            docker push %IMG_REMOTE%

            docker logout %HARBOR_REGISTRY%
          """
        }
      }
    }
  }

  post {
    always {
      bat """
        docker rm -f %APP_NAME% 2>nul
      """
    }
  }
}
