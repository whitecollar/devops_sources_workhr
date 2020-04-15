#!groovy
@Library("shared-libraries")
import io.bit.JenkinsJobs
import io.bit.Consul
import io.bit.Utils
import io.bit.ProjectHelpers
import io.bit.Notification
import io.bit.JIRAIntegration

def jenkinsJobs = new JenkinsJobs()
def consul = new Consul()
def utils = new Utils()
def projectHelpers = new ProjectHelpers()
def notification = new Notification()
def jIRAIntegration = new JIRAIntegration()

pipeline {
    agent {
        label "service_NewProject"
    }

    options {
        buildDiscarder(logRotator(daysToKeepStr:'30'))
        timeout(time: 28800, unit: 'SECONDS') 
    }

    parameters {
        string(description: 'Project key in jira', name: 'projectKey')
        string(description: 'User', name: 'user')
        string(defaultValue: "", description: 'Optional. System parameter with issue key from SD', name: 'issueKey')
        string(defaultValue: "", description: 'Optional. Issue author from SD', name: 'jiraReporter')
        string(defaultValue: "", description: 'Obsolete', name: 'isTestJenkinsJob')
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
        stage("Подготовка параметров") {
            steps {
                timestamps {
                    script {
                        projectHelpers.beforeStartJob()
                        projectKey = projectKey.toLowerCase().replaceAll("\\s","")
                        user = user.toLowerCase().replaceAll("\\s","")
                        slackChannel = consul.queryVal("${projectKey}/slack/${projectKey}_build_log")
                        taskType = jIRAIntegration.getIssueSummary(issueKey)
                    }
                }
            }
        }
        stage('Отправка уведомления в slack') {
            steps {
                timestamps {
                    script {
                        threadId = notification.sendSlackStartBuild(slackChannel, taskType, jiraReporter, issueKey)
                        currentBuild.result = "SUCCESS"
                    }
                }
            }
        }
        stage("Заполнение настроек в Consul") {
            steps {
                timestamps {
                    script {
                        jenkinsJobs.fillConsulUser(projectKey, user, issueKey, jiraReporter, isTestJenkinsJob)
                    }
                }
            }
        }
        stage("Добавление пользователя в rmq") {
            steps {
                timestamps {
                    script {
                        jenkinsJobs.addUserRabbitMQ(projectKey, user, issueKey, jiraReporter, isTestJenkinsJob)
                    }
                }
            }
        }
        stage("Добавление пользователя в хранилище 1С") {
            steps {
                timestamps {
                    script {
                        devAgent = consul.queryVal("${projectKey}/project_server")
                        if (!utils.pingServer(devAgent, true)) {
                            utils.raiseError("Ошибка. Не найден сервер ${devAgent}")
                        }
                        jenkinsJobs.addUserStorage1c(devAgent, projectKey, user, issueKey, jiraReporter, isTestJenkinsJob)
                    }
                }
            }
        }
    }
    post {
        always {
            script {
                notification.sendSlackFinishBuild(
                    slackChannel,
                    taskType,
                    currentBuild.result,
                    null,
                    jiraReporter,
                    threadId,
                    "",
                    issueKey
                )
                projectHelpers.beforeEndJob()
            }
        }
    }
}
