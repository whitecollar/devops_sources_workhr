@Library("shared-libraries")
import io.bit.ProjectHelpers
import io.bit.Utils
import io.bit.BITConvJava

def projectHelpers = new ProjectHelpers()

pipeline {

    parameters {
        string(description: 'User', name: 'user')
        string(description: 'User origin E-mail', name: 'Email')
        string(defaultValue: "jenkins", description: 'Optional. System parameter with issue key from SD', name: 'issueKey')
        string(defaultValue: "jenkins", description: 'Optional. Issue author from SD', name: 'jiraReporter')
        string(defaultValue: "false", description: 'Optional. Obsolete parameter you should not use', name: 'isTestJenkinsJob')
    }

    agent {
        label "exchangeservice"
    }

    options {
        timeout(time: 3600, unit: 'SECONDS')
        buildDiscarder(logRotator(numToKeepStr: '10'))
    }

    stages {
        stage('Перед запуском сборки') {
            steps {
                timestamps {
                    script {
                        projectHelpers.beforeStartJob()
                    }
                }
            }
        }
        stage("Выполнение") {
            steps {
                timestamps {
                    script {
                        Utils utils = new Utils()
                        withCredentials([[$class: 'UsernamePasswordMultiBinding', credentialsId: 'exch-setup',
                                          usernameVariable: 'USERNAME', passwordVariable: 'PASSWORD']]) {
                            returnCode = utils.cmd("powershell -file ${env.WORKSPACE}/maintenance/exchangeDeleteMailboxAndDeleteContact.ps1 ${BITConvJava.EXCHANGE_URL}/powershell/ ${user} ${Email} ${USERNAME} ${PASSWORD}")
                        }
                        if (returnCode != 0) {
                            utils.raiseError("Почтовый ящик не удален")
                        }
                    }
                }
            }
        }
    }

    post {
        always {
            script {
                projectHelpers.beforeEndJob()
            }
        }
    }
}