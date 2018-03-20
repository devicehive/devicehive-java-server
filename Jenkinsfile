properties([
  buildDiscarder(logRotator(artifactDaysToKeepStr: '', artifactNumToKeepStr: '32', daysToKeepStr: '', numToKeepStr: '100'))
])

def test_branches = ["development", "master"]
def publish_branches = ["development", "master"]
def deploy_branches = ["development"]
def test_rpc = true

stage('Build jars') {
  node('docker') {
    echo 'Building jars ...'
    def maven = docker.image('maven:3.5.3-jdk-8')
    maven.pull()
    maven.inside {
      checkout scm
      sh 'mvn clean package -DskipTests'
      try {
        sh 'mvn test'
      }
      finally {
        junit '**/target/surefire-reports/TEST-*.xml'
      }
      def artifacts = 'devicehive-backend/target/devicehive-backend-*-boot.jar, devicehive-auth/target/devicehive-auth-*-boot.jar, devicehive-plugin/target/devicehive-plugin-*-boot.jar, devicehive-frontend/target/devicehive-frontend-*-boot.jar, devicehive-common/target/devicehive-common-*-shade.jar'
      archiveArtifacts artifacts: artifacts, fingerprint: true, onlyIfSuccessful: true
      stash includes: artifacts, name: 'jars'
    }
  }
}

stage('Build and publish Docker images in CI repository') {
  node('docker') {
    echo 'Building images ...'
    unstash 'jars'
    def auth = docker.build('devicehiveci/devicehive-auth:${BRANCH_NAME}', '--pull -f dockerfiles/devicehive-auth.Dockerfile .')
    def plugin = docker.build('devicehiveci/devicehive-plugin:${BRANCH_NAME}', '-f dockerfiles/devicehive-plugin.Dockerfile .')
    def frontend = docker.build('devicehiveci/devicehive-frontend:${BRANCH_NAME}', '-f dockerfiles/devicehive-frontend.Dockerfile .')
    def backend = docker.build('devicehiveci/devicehive-backend:${BRANCH_NAME}', '-f dockerfiles/devicehive-backend.Dockerfile .')
    def hazelcast = docker.build('devicehiveci/devicehive-hazelcast:${BRANCH_NAME}', '--pull -f dockerfiles/devicehive-hazelcast.Dockerfile .')

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

if (test_branches.contains(env.BRANCH_NAME)) {
  stage('Run integration tests'){
    parallel rpc: {
      if (test_rpc) {
        stage('Run integration tests with rpc'){
          node('tests-runner'){
            try {
              clone_devicehive_docker()
              dir('devicehive-docker/rdbms-image'){
                writeFile file: '.env', text: """COMPOSE_PROJECT_NAME=rpc
                COMPOSE_FILE=docker-compose.yml:ci-images.yml:dh_proxy_custom_certificate.yml:dh_plugin.yml:dh_plugin-ci-image.yml
                DH_TAG=${BRANCH_NAME}
                JWT_SECRET=devicehive
                DH_FE_SPRING_PROFILES_ACTIVE=rpc-client
                DH_BE_SPRING_PROFILES_ACTIVE=rpc-server
                """

                start_devicehive()
              }
              wait_for_devicehive_is_up()
              run_devicehive_tests()
            } finally {
              archive_container_logs('rpc')
              zip archive: true, dir: 'devicehive-tests', glob: 'mochawesome-report/**', zipFile: 'mochawesome-report-rpc.zip'
              shutdown_devicehive()
              cleanWs()
            }
          }
        }
      }
    },
    wsproxy: {
      stage('Run integration tests with ws-proxy'){
        node('tests-runner'){
          try {
            clone_devicehive_docker()
            dir('devicehive-docker/rdbms-image'){
              writeFile file: '.env', text: """COMPOSE_PROJECT_NAME=wsproxy
              COMPOSE_FILE=docker-compose.yml:ci-images.yml:dh_proxy_custom_certificate.yml:dh_plugin.yml:dh_plugin-ci-image.yml
              DH_TAG=${BRANCH_NAME}
              JWT_SECRET=devicehive
              """

              start_devicehive()
            }
            wait_for_devicehive_is_up()
            run_devicehive_tests()
          } finally {
            archive_container_logs('wsproxy')
            zip archive: true, dir: 'devicehive-tests', glob: 'mochawesome-report/**', zipFile: 'mochawesome-report-wsproxy.zip'
            shutdown_devicehive()
            cleanWs()
          }
        }
      }
    }
  }
}

if (publish_branches.contains(env.BRANCH_NAME)) {
  stage('Publish image in main repository') {
    node('docker') {
      // Builds from 'master' branch will have 'latest' tag
      def IMAGE_TAG = (env.BRANCH_NAME == 'master') ? 'latest' : env.BRANCH_NAME

      docker.withRegistry('https://registry.hub.docker.com', 'devicehiveci_dockerhub'){
        sh """
          docker tag devicehiveci/devicehive-auth:${BRANCH_NAME} registry.hub.docker.com/devicehive/devicehive-auth:${IMAGE_TAG}
          docker tag devicehiveci/devicehive-frontend:${BRANCH_NAME} registry.hub.docker.com/devicehive/devicehive-frontend:${IMAGE_TAG}
          docker tag devicehiveci/devicehive-backend:${BRANCH_NAME} registry.hub.docker.com/devicehive/devicehive-backend:${IMAGE_TAG}
          docker tag devicehiveci/devicehive-hazelcast:${BRANCH_NAME} registry.hub.docker.com/devicehive/devicehive-hazelcast:${IMAGE_TAG}
          docker tag devicehiveci/devicehive-plugin:${BRANCH_NAME} registry.hub.docker.com/devicehive/devicehive-plugin:${IMAGE_TAG}

          docker push registry.hub.docker.com/devicehive/devicehive-auth:${IMAGE_TAG}
          docker push registry.hub.docker.com/devicehive/devicehive-frontend:${IMAGE_TAG}
          docker push registry.hub.docker.com/devicehive/devicehive-backend:${IMAGE_TAG}
          docker push registry.hub.docker.com/devicehive/devicehive-hazelcast:${IMAGE_TAG}
          docker push registry.hub.docker.com/devicehive/devicehive-plugin:${IMAGE_TAG}
        """
      }
    }
  }
}

if (deploy_branches.contains(env.BRANCH_NAME)) {
  stage('Deploy build to dev server'){
    node('dev-server') {
      dir('/home/centos/devicehive-docker/rdbms-image'){
        sh '''
          echo "$(date): Deploying build #${BUILD_NUMBER} from ${BRANCH_NAME} branch..." > ./jenkins-cd.timestamp
          sed -i -e "s/DH_TAG=.*/DH_TAG=${BRANCH_NAME}/g" .env
          sudo docker-compose pull
          sudo docker-compose up -d
          echo "$(date): Successfully deployed build #${BUILD_NUMBER} from ${BRANCH_NAME} branch" > ./jenkins-cd.timestamp
        '''
      }
    }
  }
}

def clone_devicehive_docker(){
  dir('devicehive-docker'){
    echo("Clone Docker Compose files")
    git branch: 'development', url: 'https://github.com/devicehive/devicehive-docker.git', depth: 1
    sh '''
      mkdir ../ssl || :
      tar -xakf rdbms-image/tests/localhost_cert.tar.gz -C ../ssl
    '''
  }
}

def start_devicehive() {
  echo("Start DeviceHive")
  sh '''
    sudo docker-compose pull
    sudo docker-compose up -d
  '''
}

def wait_for_devicehive_is_up() {
  echo("Wait for devicehive")
  timeout(time:5, unit: 'MINUTES') {
    waitUntil{
      def is_up = sh script: 'curl --output /dev/null --silent --head --fail "http://127.0.0.1/api/rest/info"', returnStatus: true
      return (is_up == 0)
    }
  }
}

def run_devicehive_tests() {
  dir('devicehive-tests') {
    echo("Clone integration tests")
    git branch: 'development', url: 'https://github.com/devicehive/devicehive-tests.git', depth: 1

    echo("Install dependencies with npm")
    sh '''
      sudo npm install -g mocha mochawesome
      sudo npm i
    '''

    echo("Configure tests")
    sh '''
      cp config.json config.json.orig
      cat config.json.orig | \\
      jq ".server.wsUrl = \\"ws://127.0.0.1/api/websocket\\"" | \\
      jq ".server.ip = \\"127.0.0.1\\"" | \\
      jq ".server.port = \\"80\\"" | \\
      jq ".server.restUrl = \\"http://127.0.0.1/api/rest\\"" | \\
      jq ".server.authRestUrl = \\"http://127.0.0.1/auth/rest\\"" | \\
      jq ".server.pluginRestUrl = \\"http://127.0.0.1/plugin/rest\\"" > config.json
    '''

    timeout(time:10, unit: 'MINUTES') {
      echo("Run integration tests")
      sh 'mocha --exit -R mochawesome integration-tests'
    }
  }
}

def archive_container_logs(flavour) {
  def logsdir = "container-logs"
  sh """
    mkdir ${logsdir} || :
    sudo docker logs ${flavour}_dh_auth_1 > ${logsdir}/${flavour}_auth.log 2>&1
    sudo docker logs ${flavour}_dh_backend_1 > ${logsdir}/${flavour}_backend.log 2>&1
    sudo docker logs ${flavour}_dh_frontend_1 > ${logsdir}/${flavour}_frontend.log 2>&1
    sudo docker logs ${flavour}_dh_plugin_1 > ${logsdir}/${flavour}_plugin.log 2>&1
    sudo docker logs ${flavour}_dh_proxy_1 > ${logsdir}/${flavour}_proxy.log 2>&1
    sudo docker logs ${flavour}_hazelcast_1 > ${logsdir}/${flavour}_hazelcast.log 2>&1
    sudo docker logs ${flavour}_kafka_1 > ${logsdir}/${flavour}_kafka.log 2>&1
    sudo docker logs ${flavour}_wsproxy_1 > ${logsdir}/${flavour}_wsproxy.log 2>&1
    sudo docker logs ${flavour}_postgres_1 > ${logsdir}/${flavour}_postgres.log 2>&1
    sudo docker logs ${flavour}_zookeeper_1 > ${logsdir}/${flavour}_zookeeper.log 2>&1
  """
  def logs = "${logsdir}/${flavour}_auth.log, ${logsdir}/${flavour}_backend.log, ${logsdir}/${flavour}_frontend.log, ${logsdir}/${flavour}_plugin.log, ${logsdir}/${flavour}_proxy.log, ${logsdir}/${flavour}_hazelcast.log, ${logsdir}/${flavour}_kafka.log, ${logsdir}/${flavour}_wsproxy.log, ${logsdir}/${flavour}_postgres.log, ${logsdir}/${flavour}_zookeeper.log"
  archiveArtifacts artifacts: logs, fingerprint: true
}

def shutdown_devicehive() {
  echo("Shutting down DeviceHive instance and cleaning up")
  dir('devicehive-docker/rdbms-image') {
    sh '''
      sudo docker-compose kill
      sudo docker-compose down
      sudo docker volume ls -qf dangling=true | xargs -r sudo docker volume rm
    '''
  }
}
