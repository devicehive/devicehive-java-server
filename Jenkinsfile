properties([
  buildDiscarder(logRotator(artifactDaysToKeepStr: '', artifactNumToKeepStr: '', daysToKeepStr: '7', numToKeepStr: '7'))
])

def publishable_branches = ["development", "master"]
def deployable_branches = ["development"]

node('docker') {
  stage('Build jars') {
    echo 'Building jars ...'
    def maven = docker.image('maven:3.5.2-jdk-8')
    maven.pull()
    maven.inside {
      checkout scm
      sh 'mvn clean package -DskipTests'
      sh 'mvn test'
      archiveArtifacts artifacts: 'devicehive-backend/target/devicehive-backend-*-boot.jar, devicehive-auth/target/devicehive-auth-*-boot.jar, devicehive-plugin/target/devicehive-plugin-*-boot.jar, devicehive-frontend/target/devicehive-frontend-*-boot.jar, devicehive-common/target/devicehive-common-*-shade.jar', fingerprint: true, onlyIfSuccessful: true

      stash includes:'devicehive-backend/target/devicehive-backend-*-boot.jar, devicehive-auth/target/devicehive-auth-*-boot.jar, devicehive-plugin/target/devicehive-plugin-*-boot.jar, devicehive-frontend/target/devicehive-frontend-*-boot.jar, devicehive-common/target/devicehive-common-*-shade.jar', name: 'jars'
    }
  }

  stage('Build and publish Docker images in CI repository') {
    echo 'Building images ...'
    unstash 'jars'
    def auth = docker.build('devicehiveci/devicehive-auth-rdbms:${BRANCH_NAME}', '-f dockerfiles/devicehive-auth-rdbms.Dockerfile .')
    def plugin = docker.build('devicehiveci/devicehive-plugin-rdbms:${BRANCH_NAME}', '-f dockerfiles/devicehive-plugin-rdbms.Dockerfile .')
    def frontend = docker.build('devicehiveci/devicehive-frontend-rdbms:${BRANCH_NAME}', '-f dockerfiles/devicehive-frontend-rdbms.Dockerfile .')
    def backend = docker.build('devicehiveci/devicehive-backend-rdbms:${BRANCH_NAME}', '-f dockerfiles/devicehive-backend-rdbms.Dockerfile .')
    def hazelcast = docker.build('devicehiveci/devicehive-hazelcast:${BRANCH_NAME}', '-f dockerfiles/devicehive-hazelcast.Dockerfile .')

    echo 'Pushing images to CI repository ...'
    docker.withRegistry('https://registry.hub.docker.com', 'devicehiveci_dockerhub'){
      auth.push()
      plugin.push()
      frontend.push()
      backend.push()
      hazelcast.push()
    }
  }
}

if (publishable_branches.contains(env.BRANCH_NAME)) {
  stage('Run regression tests'){
    node('tests-runner'){
      try {
        dir('devicehive-docker'){
          echo("Clone Docker Compose files")
          git branch: 'development', url: 'https://github.com/devicehive/devicehive-docker.git', depth: 1
        }

        dir('devicehive-docker/rdbms-image'){
          writeFile file: '.env', text: """COMPOSE_FILE=docker-compose.yml:ci-images.yml
          DH_TAG=${BRANCH_NAME}
          JWT_SECRET=devicehive
          """

          echo("Start DeviceHive")
          sh '''
            sudo docker-compose pull
            sudo docker-compose up -d
          '''
        }

        echo("Wait for devicehive")
        timeout(time:2, unit: 'MINUTES') {
          waitUntil{
            def fe_status = sh script: 'curl --output /dev/null --silent --head --fail "http://127.0.0.1:8080/api/rest/info"', returnStatus: true
            return (fe_status == 0)
          }
        }

        dir('devicehive-tests') {
          echo("Clone regression tests")
          git branch: 'development', url: 'https://github.com/devicehive/devicehive-tests.git', depth: 1

          echo("Install dependencies with npm")
          sh '''
            sudo npm install -g mocha@3.5.3 mochawesome
            sudo npm i
          '''

          echo("Configure tests")
          sh '''
            cp config.json config.json.orig
            cat config.json.orig | \\
            jq ".server.wsUrl = \\"ws://127.0.0.1:8080/api/websocket\\"" | \\
            jq ".server.ip = \\"127.0.0.1\\"" | \\
            jq ".server.port = \\"8080\\"" | \\
            jq ".server.restUrl = \\"http://127.0.0.1:8080/api/rest\\"" | \\
            jq ".server.authRestUrl = \\"http://127.0.0.1:8090/api/rest\\"" > config.json
          '''

          timeout(time:10, unit: 'MINUTES') {
            echo("Run integration tests")
            sh 'mocha -R mochawesome integration-tests'
          }
        }
      } finally {
        zip archive: true, dir: 'devicehive-tests', glob: 'mochawesome-report/**', zipFile: 'mochawesome-report.zip'
        dir('devicehive-docker/rdbms-image') {
          sh '''
            sudo docker-compose kill
            sudo docker-compose down
            sudo docker volume ls -qf dangling=true | xargs -r sudo docker volume rm
          '''
        }
        cleanWs()
      }
    }
  }

  node('docker') {
    stage('Publish image in main repository') {
      // Builds from 'master' branch will have 'latest' tag
      def IMAGE_TAG = (env.BRANCH_NAME == 'master') ? 'latest' : env.BRANCH_NAME

      docker.withRegistry('https://registry.hub.docker.com', 'devicehiveci_dockerhub'){
        sh """
          docker tag devicehiveci/devicehive-auth-rdbms:${BRANCH_NAME} registry.hub.docker.com/devicehive/devicehive-auth-rdbms:${IMAGE_TAG}
          docker tag devicehiveci/devicehive-frontend-rdbms:${BRANCH_NAME} registry.hub.docker.com/devicehive/devicehive-frontend-rdbms:${IMAGE_TAG}
          docker tag devicehiveci/devicehive-backend-rdbms:${BRANCH_NAME} registry.hub.docker.com/devicehive/devicehive-backend-rdbms:${IMAGE_TAG}
          docker tag devicehiveci/devicehive-hazelcast:${BRANCH_NAME} registry.hub.docker.com/devicehive/devicehive-hazelcast:${IMAGE_TAG}
          docker tag devicehiveci/devicehive-plugin-rdbms:${BRANCH_NAME} registry.hub.docker.com/devicehive/devicehive-plugin-rdbms:${IMAGE_TAG}

          docker push registry.hub.docker.com/devicehive/devicehive-auth-rdbms:${IMAGE_TAG}
          docker push registry.hub.docker.com/devicehive/devicehive-frontend-rdbms:${IMAGE_TAG}
          docker push registry.hub.docker.com/devicehive/devicehive-backend-rdbms:${IMAGE_TAG}
          docker push registry.hub.docker.com/devicehive/devicehive-hazelcast:${IMAGE_TAG}
          docker push registry.hub.docker.com/devicehive/devicehive-plugin-rdbms:${IMAGE_TAG}
        """
      }
    }
  }
}

if (deployable_branches.contains(env.BRANCH_NAME)) {
  stage('Deploy build to dev server'){
    node('dev-server') {
      dir('/home/centos/devicehive-docker/rdbms-image'){
        sh '''
          sed -i -e "s/DH_TAG=.*/DH_TAG=${BRANCH_NAME}/g" .env
          sudo docker-compose pull
          sudo docker-compose up -d
          echo "$(date): Deployed build from ${BRANCH_NAME} to dev server" > ./jenkins-cd.timestamp
        '''
      }
    }
  }
}
