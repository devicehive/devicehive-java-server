properties([
  buildDiscarder(logRotator(artifactDaysToKeepStr: '', artifactNumToKeepStr: '', daysToKeepStr: '7', numToKeepStr: '7'))
])

def deployable_branches = ["development"]

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
    def frontend = docker.build('devicehiveci/devicehive-frontend-rdbms:${BRANCH_NAME}', '-f dockerfiles/devicehive-frontend-rdbms.Dockerfile.Jenkins .')
    def backend = docker.build('devicehiveci/devicehive-backend-rdbms:${BRANCH_NAME}', '-f dockerfiles/devicehive-backend-rdbms.Dockerfile.Jenkins .')
    def hazelcast = docker.build('devicehiveci/devicehive-hazelcast:${BRANCH_NAME}', '-f dockerfiles/devicehive-hazelcast.Dockerfile.Jenkins .')

    docker.withRegistry('https://registry.hub.docker.com', 'devicehiveci_dockerhub'){
      frontend.push()
      backend.push()
      hazelcast.push()
    }

    if (deployable_branches.contains(env.BRANCH_NAME)) {
      docker.withRegistry('https://registry.hub.docker.com', 'devicehiveci_dockerhub'){
        sh '''
          docker tag devicehiveci/devicehive-frontend-rdbms:${BRANCH_NAME} registry.hub.docker.com/devicehive/devicehive-frontend-rdbms:${BRANCH_NAME}
          docker tag devicehiveci/devicehive-backend-rdbms:${BRANCH_NAME} registry.hub.docker.com/devicehive/devicehive-backend-rdbms:${BRANCH_NAME}
          docker tag devicehiveci/devicehive-hazelcast:${BRANCH_NAME} registry.hub.docker.com/devicehive/devicehive-hazelcast:${BRANCH_NAME}

          docker push registry.hub.docker.com/devicehive/devicehive-frontend-rdbms:${BRANCH_NAME}
          docker push registry.hub.docker.com/devicehive/devicehive-backend-rdbms:${BRANCH_NAME}
          docker push registry.hub.docker.com/devicehive/devicehive-hazelcast:${BRANCH_NAME}
        '''
      }
    }
  }
}

node('dev-server') {
  if (deployable_branches.contains(env.BRANCH_NAME)) {
    stage('Deploy build to dev server'){
      sh '''
        cd ~/devicehive-docker/rdbms-image
        sed -i -e "s/DH_TAG=.*/DH_TAG=${BRANCH_NAME}/g" .env
        sudo docker-compose pull
        sudo docker-compose up -d
        echo "$(date): Deployed build from ${BRANCH_NAME} to dev server" > ./jenkins-cd.timestamp
      '''
    }
  }
}
