node('docker') {
  stage('Build jars') {
    echo 'Building jars ...'
    def maven = docker.image('maven:3.3.9-jdk-8')
    maven.pull()
    maven.inside {
      checkout scm
      sh 'mvn clean package -DskipTests'
      archiveArtifacts artifacts: 'devicehive-backend/target/devicehive-backend-*-boot.jar, devicehive-frontend/target/devicehive-frontend-*-boot.jar, devicehive-common/target/devicehive-common-*-shade.jar', fingerprint: true, onlyIfSuccessful: true
    }
  }
}
