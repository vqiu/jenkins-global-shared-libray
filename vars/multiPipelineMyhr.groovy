def call(body) {
  def config = [:]
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = config
    body()

  def  ENV_INFO
  if (env.BRANCH_NAME == 'master') {
    ENV_INFO         = 'development'
    IMAGE_TAG        = "${BUILD_NUMBER}-alpha"
    KUBE_CRED        = 'kubeconfig-new-dev'
  
  } else if (env.TAG_NAME) {
    ENV_INFO         = 'production'
    IMAGE_TAG        = TAG_NAME.replace('release-','')
    KUBE_CRED        = 'kubeconfig-new-prod'
  } 

  pipeline {
  
    options {
      timeout(time: 15, unit: 'MINUTES')
      timestamps ()
      disableConcurrentBuilds()
      quietPeriod(3)
      buildDiscarder(logRotator(numToKeepStr:'3'))
    }
  
    environment {

      REGISTRY_URL     = 'registry.haid.com.cn'
      NAMESPACE        = "myhr-system"
      PROJECT_NAME     = "myhr"
      APP_NAME         = "${config.appName}"
      EMAIL_RECIPIENTS = "tanyg@haid.com.cn,qiush01@haid.com.cn"
    }
  
    agent {
      kubernetes {
        yaml '''
apiVersion: v1
kind: Pod
metadata:
  labels:
    label: jenkins-slave
spec:
  nodeSelector:
    jnlp: true
  containers:
  - name: dind
    image: registry.haid.com.cn/library/docker-kubectl:2.0.1
    imagePullPolicy: IfNotPresent
    command:
    - cat
    tty: true
    volumeMounts:
    - name: docker-socket
      mountPath: '/var/run/docker.sock'
    - name: docker-config
      mountPath: /root/.docker/
  volumes:
  - name: docker-socket
    hostPath:
      path: '/var/run/docker.sock'
  - name: docker-config
    configMap:
      name: docker-config
  securityContext:
    runAsUser: 0
'''
      }
    }
  
    stages {
  
      stage('Build and Push') {
        steps {
          container('dind') {
            sh """
              docker build -t ${REGISTRY_URL}/${PROJECT_NAME}/${APP_NAME}:${IMAGE_TAG} .
              docker push ${REGISTRY_URL}/${PROJECT_NAME}/${APP_NAME}:${IMAGE_TAG}
              docker rmi ${REGISTRY_URL}/${PROJECT_NAME}/${APP_NAME}:${IMAGE_TAG}
            """
          }
        }
      }
  
      stage('Deploy - Dev') {
        when {
          expression { ENV_INFO == 'development' }
        }
        steps {
          withCredentials([file(credentialsId: "${KUBE_CRED}", variable: 'KUBECONFIG')]) {
            container('dind') {
              sh """
                sed -i 's@#IMAGE_TAG#@${IMAGE_TAG}@' manifests/${APP_NAME}/${ENV_INFO}/deployment.yaml
                kubectl -n ${NAMESPACE} apply -f manifests/${APP_NAME}/${ENV_INFO}
              """
            }
          }
        }
      }
  
      stage('Deploy - Prod') {
        when {
          expression { ENV_INFO == 'production' }
        }
        steps {
          withCredentials([file(credentialsId: "${KUBE_CRED}", variable: 'KUBECONFIG')]) {
            container('dind') {
              sh """
                sed -i 's@#IMAGE_TAG#@${IMAGE_TAG}@' manifests/${APP_NAME}/${ENV_INFO}/deployment.yaml
                kubectl -n ${NAMESPACE} apply -f manifests/${APP_NAME}/${ENV_INFO}
              """
            }
          }
        }
      }
 
      stage('Generate Report') {
        steps {
          sh """
            sed -i -e 's^#ENV^${ENV_INFO}^' \
                   -e 's^#BUILD_URL^${BUILD_URL}^' \
                   -e 's^#PROJECT^${PROJECT_NAME}^' \
                   -e 's^#IMAGE^${REGISTRY_URL}/${PROJECT_NAME}/${APP_NAME}:${IMAGE_TAG}^' \
                   -e 's^#GIT_URL^${GIT_URL}^' \
                   -e 's^#BRANCH^${BRANCH_NAME}^' \
                   -e "s^#COMMITTER_NAME^`git --no-pager show -s --format='%an' $GIT_COMMIT`^" \
                   -e "s^#COMMITTER_NOTES^`git show --format='format:%s' -s $GIT_COMMIT`^" manifests/report.html
          """
        }
      }
    }
  
    post{
      always {
         emailext body: '${FILE, path="manifests/report.html"}',
         attachLog: true,
         compressLog: true,
         mimeType: 'text/html',
         subject: "任务 \'${env.JOB_NAME}:${env.BUILD_NUMBER}\' - 应用构建报告[ ${currentBuild.result?:'SUCCESS'} ]",
         from: "haid-gitlab@haid.com.cn",
         to: "${EMAIL_RECIPIENTS}"
      }
    }
  }
}

return this

