@Library("shared-libraries")
import io.bit.ProjectHelpers
import io.bit.Utils

ProjectHelpers projectHelpers = new ProjectHelpers()
Utils utils = new Utils()

pipeline {

    parameters {
        string(description: 'Project key in jira', name: 'projectKey')
        string(defaultValue: "", description: 'Optional. System parameter with issue key from SD', name: 'issueKey')
        string(defaultValue: "", description: 'Optional. Issue author from SD', name: 'jiraReporter')
    }

    agent {
        label "shareservice"
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
                        withCredentials([[$class: 'UsernamePasswordMultiBinding', credentialsId: 'jira-app',
                                          usernameVariable: 'USERNAME', passwordVariable: 'PASSWORD']]) {
                            returnCode = utils.cmd("powershell -file ${env.WORKSPACE}/servers-maintenance/new_project_folders_creation.ps1 ${projectKey.toUpperCase()}  ${USERNAME} ${PASSWORD}")
                        }
                        if (returnCode != 0) {
                            utils.raiseError("Папка для проекта не создалась")
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