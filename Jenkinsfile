pipeline {
  agent any
  tools { maven 'Maven_3.9.13' }

  environment {
    APP_NAME      = 'java-jenkins-app'
    EXPECTED_LOG  = 'Hello from Java app running in Docker via Jenkins!'
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

      echo ===== Listing target folder =====
      dir target

      echo ===== Checking for target\\app.jar =====
      if not exist target\\app.jar (
        echo ERROR: target\\app.jar not found.
        echo If you see a different jar name above, we will copy/rename it.
        exit /b 1
      )
    '''
  }
}

    stage('Build Docker Image (local only)') {
      steps {
        script {
          env.IMG_LOCAL = "${env.APP_NAME}:${env.BUILD_NUMBER}"
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
            echo Expected output not found. Failing build.
            exit /b 1
          )
        """
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
