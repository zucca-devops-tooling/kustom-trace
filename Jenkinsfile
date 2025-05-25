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

                echo "-----------------------------------------------------"
                echo "DEBUGGING FILE EXISTENCE AFTER GRADLE BUILD:"
                echo "-----------------------------------------------------"

                echo "Current working directory (pwd):"
                sh "pwd"
                echo "-----------------------------------------------------"

                echo "Checking if 'cli/build/libs' directory exists:"
                sh "if [ -d 'cli/build/libs' ]; then echo 'cli/build/libs directory EXISTS.'; else echo 'cli/build/libs directory DOES NOT EXIST!'; fi"
                echo "-----------------------------------------------------"

                echo "Listing contents of 'cli/build/libs' (if it exists):"
                sh "ls -Al cli/build/libs/ || echo 'Could not list cli/build/libs - does it exist or is it empty?'"
                echo "-----------------------------------------------------"

                echo "Full recursive listing of 'cli/build' (to see where libs might be):"
                sh "ls -AlR cli/build || echo 'Could not list cli/build'"
                echo "-----------------------------------------------------"

                // Now, let's try to find the file
                // This uses the shell's globbing first, which is very simple
                echo "Attempting to find JAR using shell globbing (ls):"
                // We use set +e to prevent the script from exiting if ls finds no files
                // and set -e to re-enable exiting on error afterwards.
                def lsOutput = sh(script: "set +e; ls cli/build/libs/kustomtrace-cli-*-all.jar; set -e", returnStdout: true).trim()

                if (lsOutput && !lsOutput.contains("No such file or directory")) {
                    echo "SUCCESS (ls): Found JAR(s): ${lsOutput}"
                    // If you are stashing, you'd typically want just one.
                    // If lsOutput might contain multiple lines if there are multiple matches (unlikely for -all.jar)
                    // We can take the first line for simplicity if needed:
                    def firstJarFound = lsOutput.tokenize('\n')[0]
                    echo "Using first JAR found by ls: ${firstJarFound}"
                    // stash name: 'cliShadowJar', includes: firstJarFound.substring(firstJarFound.indexOf("cli/")), allowEmpty: false
                    // The substring might be needed if ls gives a full path from somewhere unexpected
                } else {
                    echo "FAILURE (ls): Shell globbing (ls) did not find the JAR."
                    echo "Output of ls command was: '${lsOutput}'"

                    echo "-----------------------------------------------------"
                    echo "Attempting to find JAR using 'find' command again for diagnostics:"
                    def findJarScript = "find cli/build/libs -name 'kustomtrace-cli-*-all.jar' -print" // Removed -quit for more info
                    echo "Executing find script: ${findJarScript}"
                    def findOutput = sh(script: findJarScript, returnStdout: true, returnStatus: true)

                    if (findOutput.status == 0 && findOutput.stdout.trim()) {
                        echo "DEBUG (find): Found JAR(s): ${findOutput.stdout.trim()}"
                         error("CLI Shadow JAR found by find but NOT by ls. This is strange. Please check logs.")
                    } else {
                        echo "DEBUG (find): 'find' command status: ${findOutput.status}"
                        echo "DEBUG (find): 'find' command output: ${findOutput.stdout.trim()}"
                        error("CLI Shadow JAR not found by 'ls' or 'find' in Build stage! Check all 'ls' outputs above to see file structure and names.")
                    }
                }
                echo "-----------------------------------------------------"
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