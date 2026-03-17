pipeline {
  agent any
  tools { maven 'Maven_3.9.13' }

  environment {
    APP_NAME     = 'java-jenkins-app'
    EXPECTED_LOG = 'Hello from Java app running in Docker via Jenkins!'
  }

  stages {
    stage('Checkout') {
      steps { checkout scm }
    }

    stage('Build JAR') {
      steps {
        bat '''
          @echo on
          mvn -v
          mvn -U -DskipTests clean package

          echo ===== List target folder =====
          if not exist target (
            echo ERROR: target folder not created. Maven build likely failed.
            exit /b 1
          )
          dir target

          echo ===== Ensure target\\app.jar exists (normalize jar name) =====
          if not exist target\\app.jar (
            echo target\\app.jar not found. Copying the built jar to target\\app.jar ...
            copy /Y target\\*.jar target\\app.jar
          )

          echo ===== Final target folder =====
          dir target

          if not exist target\\app.jar (
            echo ERROR: Still no target\\app.jar. Cannot continue.
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
          @echo on
          docker version

          echo ===== Workspace files =====
          dir
          echo ===== Target files =====
          dir target

          docker build -t %IMG_LOCAL% .
          docker images | findstr %APP_NAME%
        """
      }
    }

    stage('Run & Verify Java Output') {
      steps {
        bat """
          @echo on
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
      // Cleanup should not fail the whole build
      bat """
        @echo on
        docker rm -f %APP_NAME% 2>nul || exit /b 0
      """
    }
  }
}
