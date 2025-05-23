pipeline {
    agent any

    environment {
        GRADLE_OPTS = '-Dorg.gradle.jvmargs="-Xmx2g -XX:+HeapDumpOnOutOfMemoryError"'

        GH_CREDENTIALS  = credentials('GITHUB_PACKAGES')
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }
        stage('Fix permissions') {
            steps {
                sh 'chmod +x gradlew'
            }
        }
        stage('Build') {
            steps {
                script {
                    setStatus('build','NEUTRAL','Building the project...')
                    try {
                        sh "./gradlew clean assemble --info --no-daemon"
                        setStatus('build','SUCCESS','Build succeeded')
                    } catch (Exception e) {
                        setStatus('build','FAILURE','Build failed')
                        throw e
                    }
                }
            }
        }
        stage('Spotless') {
            steps {
                script {
                    setStatus('spotless','NEUTRAL','Checking code format...')
                    try {
                        sh './gradlew check -x test --no-daemon'
                        setStatus('spotless','SUCCESS','Spotless passed')
                    } catch (Exception e) {
                        setStatus('spotless','FAILURE','Spotless failed')
                    }
                }
            }
        }
        stage('Test') {
            steps {
                script {
                    setStatus('test','NEUTRAL','Running tests...')
                    try {
                        sh './gradlew :kustomtrace:test --no-daemon'
                        setStatus('test','SUCCESS','Tests passed')
                    } catch (Exception e) {
                        setStatus('test','FAILURE','Tests failed')
                    }
                }
            }
        }
        stage('Functional tests') {
            steps {
                script {
                    setStatus('functionalTest','NEUTRAL','Running functional tests...')
                    try {
                        sh './gradlew :functional-test:test --no-daemon --info'
                        setStatus('functionalTest','SUCCESS','Functional test passed')
                    } catch (Exception e) {
                        setStatus('functionalTest','FAILURE','Functional test failed')
                    }
                }
            }
        }
        stage('Publish library') {
            when {
                anyOf {
                    branch 'main'
                    expression { sh (script: "git log -1 --pretty=%B | grep 'publishLib'", returnStatus: true) == 0 } // Matches a commit message including publishLib
                    expression { sh (script: "git log -1 --pretty=%B | grep 'publishAll'", returnStatus: true) == 0 } // Matches a commit message including publishAll
                }
            }
            environment {
                GPG_KEY_ID    = credentials('GPG_KEY_ID')
                GPG_KEY_PASS  = credentials('GPG_KEY_PASS')
                OSSRH_CREDENTIALS  = credentials('OSSRH_CREDENTIALS')
            }
            steps {
                withCredentials([
                    file(credentialsId: 'GPG_SECRET_KEY', variable: 'GPG_KEY_PATH')
                ]) {
                    sh '''#!/bin/bash
                        set -euo pipefail

                        export GPG_ASC_ARMOR="$(cat $GPG_KEY_PATH)"

                        ./gradlew :kustomtrace:publish --info --no-daemon \
                            -Psigning.keyId=$GPG_KEY_ID \
                            -Psigning.password=$GPG_KEY_PASS \
                            -Psigning.secretKeyRingFile=$GPG_KEY_PATH \
                            -PgithubPackagesUsername=$GH_CREDENTIALS_USR \
                            -PgithubPackagesPassword=$GH_CREDENTIALS_PSW \
                            -PmavenCentralUsername=$OSSRH_CREDENTIALS_USR \
                            -PmavenCentralPassword=$OSSRH_CREDENTIALS_PSW
                    '''
                }
            }
        }
        stage('Publish cli') {
            when {
                anyOf {
                    branch 'main'
                    expression { sh (script: "git log -1 --pretty=%B | grep 'publishCli'", returnStatus: true) == 0 } // Matches a commit message including publishCli
                    expression { sh (script: "git log -1 --pretty=%B | grep 'publishAll'", returnStatus: true) == 0 } // Matches a commit message including publishAll
                }
            }
            environment {
                GPG_KEY_ID    = credentials('GPG_KEY_ID')
                GPG_KEY_PASS  = credentials('GPG_KEY_PASS')
                OSSRH_CREDENTIALS  = credentials('OSSRH_CREDENTIALS')
            }
            steps {
                withCredentials([
                    file(credentialsId: 'GPG_SECRET_KEY', variable: 'GPG_KEY_PATH')
                ]) {
                    sh '''#!/bin/bash
                        set -euo pipefail

                        export GPG_ASC_ARMOR="$(cat $GPG_KEY_PATH)"

                        ./gradlew :kustomtrace-cli:publish --info --no-daemon \
                            -Psigning.keyId=$GPG_KEY_ID \
                            -Psigning.password=$GPG_KEY_PASS \
                            -Psigning.secretKeyRingFile=$GPG_KEY_PATH \
                            -PgithubPackagesUsername=$GH_CREDENTIALS_USR \
                            -PgithubPackagesPassword=$GH_CREDENTIALS_PSW \
                    '''
                }
            }
        }
        /*
        stage('Git Tag') {
            when {
                branch 'main'
            }
            steps {
                sh './gradlew tagRelease --no-daemon'
            }
        }
        */
    }
}

def setStatus(context, status, message) {
    publishChecks name: context, conclusion: status, title: 'Jenkins CI', summary: message
}