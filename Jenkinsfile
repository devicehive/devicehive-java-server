properties([
  buildDiscarder(logRotator(artifactDaysToKeepStr: '', artifactNumToKeepStr: '', daysToKeepStr: '7', numToKeepStr: '7'))
])

node('docker') {
  stage('Build jars') {
    echo 'Building jars ...'
    def maven = docker.image('maven:3.3.9-jdk-8')
    maven.pull()
    maven.inside {
      checkout scm
      sh 'mvn clean package -DskipTests'
      archiveArtifacts artifacts: 'devicehive-backend/target/devicehive-backend-*-boot.jar, devicehive-frontend/target/devicehive-frontend-*-boot.jar, devicehive-common/target/devicehive-common-*-shade.jar', fingerprint: true, onlyIfSuccessful: true

      stash includes:'devicehive-backend/target/devicehive-backend-*-boot.jar, devicehive-frontend/target/devicehive-frontend-*-boot.jar, devicehive-common/target/devicehive-common-*-shade.jar', name: 'jars'
    }
  }

  stage('Build Docker images') {
    echo 'Building Frontend image ...'
    checkout scm
    unstash 'jars'
    def frontend = docker.build('sitnik/devicehive-frontend-rdbms:${BRANCH_NAME}', '-f dockerfiles/devicehive-frontend-rdbms.Dockerfile.Jenkins .')
    def backend = docker.build('sitnik/devicehive-backend-rdbms:${BRANCH_NAME}', '-f dockerfiles/devicehive-backend-rdbms.Dockerfile.Jenkins .')
    def hazelcast = docker.build('sitnik/devicehive-hazelcast:${BRANCH_NAME}', '-f dockerfiles/devicehive-hazelcast.Dockerfile.Jenkins .')
    }
  }
}
