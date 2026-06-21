pipeline {
    agent {
        kubernetes {
            yaml '''
apiVersion: v1
kind: Pod
spec:
  securityContext:
    runAsUser: 0
    runAsGroup: 0

  hostAliases:
    - ip: "127.0.0.1"
      hostnames:
        - "harbor.20211772.xyz"

  containers:
    - name: jdk
      image: eclipse-temurin:21-jdk-jammy
      command: ['cat']
      tty: true
      env:
        - name: DOCKER_HOST
          value: tcp://localhost:2375
        - name: DOCKER_TLS_CERTDIR
          value: ""

    - name: docker
      image: docker:24.0-dind
      args:
        - "--insecure-registry=harbor.20211772.xyz"
      securityContext:
        privileged: true
      env:
        - name: DOCKER_TLS_CERTDIR
          value: ""

    - name: gitops
      image: alpine/k8s:1.31.0
      command: ['cat']
      tty: true

    - name: proxy
      image: nginx:alpine
      command:
        - /bin/sh
        - -c
        - |
          apk add --no-cache openssl

          openssl req -x509 -nodes -days 365 \
            -newkey rsa:2048 \
            -keyout /etc/nginx/cert.key \
            -out /etc/nginx/cert.crt \
            -subj "/CN=harbor.20211772.xyz"

          cat <<'NGINX_EOF' > /etc/nginx/nginx.conf
          events {}

          http {
            client_max_body_size 0;

            server {
              listen 80;
              listen 443 ssl;

              ssl_certificate /etc/nginx/cert.crt;
              ssl_certificate_key /etc/nginx/cert.key;

              server_name harbor.20211772.xyz;

              location / {
                proxy_pass http://harbor.harbor.svc.cluster.local:80;
                proxy_set_header Host harbor.20211772.xyz;
                proxy_set_header X-Forwarded-Proto https;
                proxy_set_header X-Forwarded-Host $host;
              }
            }
          }
          NGINX_EOF

          nginx -g 'daemon off;'
'''
        }
    }

    environment {
        HARBOR_URL = 'harbor.20211772.xyz'
        HARBOR_PROJECT = 'tripio'
        IMAGE_NAME = 'tripio-backend'

        HARBOR_CREDENTIALS_ID = 'harbor-tripio-robot'

        GITOPS_REPO_URL = 'https://github.com/kth0213/k8s-homelab.git'
        GITOPS_CREDENTIALS_ID = 'github-token'
        GITOPS_PATH = 'k8s/apps/personal-apps/tripio/backend'
    }

    stages {
        stage('1. Wait for Docker daemon') {
            steps {
                container('docker') {
                    sh '''
                      until docker info > /dev/null 2>&1; do
                        echo "Waiting for Docker daemon..."
                        sleep 2
                      done
                    '''
                }
            }
        }

        stage('2. Test and build JAR') {
            steps {
                container('jdk') {
                    sh '''
                      chmod +x ./gradlew
                      ./gradlew clean test bootJar --no-daemon

                      JAR_FILE=$(find build/libs -maxdepth 1 -type f \
                        -name '*.jar' ! -name '*-plain.jar' | head -n 1)

                      test -n "$JAR_FILE"
                      cp "$JAR_FILE" build/libs/app.jar
                      ls -lh build/libs/app.jar
                    '''
                }
            }
        }

        stage('3. Create image tag') {
            steps {
                script {
                    if (!env.GIT_COMMIT) {
                        error 'GIT_COMMIT is not available after SCM checkout.'
                    }

                    env.IMAGE_TAG =
                        "dev-${env.BUILD_NUMBER}-${env.GIT_COMMIT.take(7)}"

                    env.IMAGE_FULL_NAME =
                        "${env.HARBOR_URL}/${env.HARBOR_PROJECT}/${env.IMAGE_NAME}:${env.IMAGE_TAG}"
                }
            }
        }

        stage('4. Docker build and push') {
            steps {
                container('docker') {
                    withCredentials([
                        usernamePassword(
                            credentialsId: "${HARBOR_CREDENTIALS_ID}",
                            usernameVariable: 'HARBOR_USER',
                            passwordVariable: 'HARBOR_PW'
                        )
                    ]) {
                        sh '''
                          test -f build/libs/app.jar

                          echo "$HARBOR_PW" \
                            | docker login "$HARBOR_URL" \
                              -u "$HARBOR_USER" \
                              --password-stdin

                          docker build \
                            -t "$IMAGE_FULL_NAME" \
                            -f Dockerfile .

                          docker push "$IMAGE_FULL_NAME"
                        '''
                    }
                }
            }
        }

        stage('5. Update GitOps image tag') {
            steps {
                container('gitops') {
                    withCredentials([
                        gitUsernamePassword(
                            credentialsId: "${GITOPS_CREDENTIALS_ID}",
                            gitToolName: 'Default'
                        )
                    ]) {
                        sh '''
                          apk add --no-cache git

                          rm -rf gitops-repo
                          git clone --depth 1 "$GITOPS_REPO_URL" gitops-repo

                          cd "gitops-repo/$GITOPS_PATH"

                          kustomize edit set image \
                            "$HARBOR_URL/$HARBOR_PROJECT/$IMAGE_NAME=$IMAGE_FULL_NAME"

                          if git diff --quiet -- kustomization.yaml; then
                            echo "No GitOps image-tag change."
                            exit 0
                          fi

                          git config user.email 'jenkins@20211772.xyz'
                          git config user.name 'Jenkins CI'

                          git add kustomization.yaml
                          git commit -m "chore(tripio): deploy backend $IMAGE_TAG"
                          git push origin main
                        '''
                    }
                }
            }
        }
    }
}
