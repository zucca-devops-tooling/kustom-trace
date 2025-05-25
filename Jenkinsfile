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
                        def shadowJarPath = sh(script: "find cli/build/libs -name 'kustomtrace-cli-*-all.jar' -print -quit", returnStdout: true).trim()
                            if (shadowJarPath) {
                                stash name: 'cliShadowJar', includes: shadowJarPath, allowEmpty: false
                                echo "Stashed ${shadowJarPath}"
                            } else {
                                error "CLI Shadow JAR not found after build in Build stage!"
                            }
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
                        sh "./gradlew check -x test --no-daemon"
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
                        sh "./gradlew :kustomtrace:test --no-daemon"
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
                        sh "./gradlew :functional-test:test --no-daemon --info"
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
            steps {
                withCredentials([
                    file(credentialsId: 'GPG_SECRET_KEY', variable: 'GPG_KEY_PATH')
                ]) {
                    sh '''#!/bin/bash
                        set -euo pipefail

                        export GPG_ASC_ARMOR="$(cat $GPG_KEY_PATH)"

                        ./gradlew :kustomtrace-cli:publish --info --no-daemon \
                            -PgithubPackagesUsername=$GH_CREDENTIALS_USR \
                            -PgithubPackagesPassword=$GH_CREDENTIALS_PSW \
                    '''
                }
            }
        }
        stage('Release') {
            when {
                allOf{
                    expression {
                        def commitMessage = sh(script: "git log -1 --pretty=%B", returnStdout: true).trim()
                        return commitMessage.contains('[release]')
                    }
                    branch 'PR-17'
                }
            }
            steps {
                script {
                    releaseVersion = sh(script: "./gradlew properties -q -Pquiet | grep '^version:' | awk '{print \$2}'", returnStdout: true).trim()
                    echo "Read project version for release: ${releaseVersion}"

                    def changelogNotes = sh(script: """
                        awk '/^## \\[${releaseVersion}\\]/{flag=1; next} /^## \\[/{flag=0} flag' CHANGELOG.md
                    """, returnStdout: true).trim()

                    if (changelogNotes.isEmpty()) {
                        changelogNotes = "No specific changelog notes found for this version."
                    }

                    def shadowJarPath = sh(script: "find cli/build/libs -name 'kustomtrace-cli-*-all.jar' -print -quit", returnStdout: true).trim()
                    if (!shadowJarPath) {
                        error("CLI Shadow JAR not found after build!")
                    }

                    def tagName = "v${releaseVersion}"
                    sh "git push https://$GH_CREDENTIALS_USR:$GH_CREDENTIALS_PSW@github.com/zucca-devops-tooling/kustom-trace.git ${tagName}"


                    sh """
                        export GH_TOKEN="$GH_CREDENTIALS_PSW"
                        gh release create ${tagName} \\
                            "${shadowJarPath}" \\
                            --title "Release ${tagName}" \\
                            --notes "${changelogNotes}"
                    """
                    echo "GitHub Release ${tagName} created."
                }
            }
        }
    }
}

def setStatus(context, status, message) {
    publishChecks name: context, conclusion: status, title: 'Jenkins CI', summary: message
}