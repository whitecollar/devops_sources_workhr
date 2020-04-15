#!groovy
@Library("shared-libraries")
import io.bit.Consul
import io.bit.ProjectHelpers
import io.bit.Utils
import io.bit.SqlUtils
import io.bit.BITConvJava
import io.bit.Notification
import io.bit.JIRAIntegration

def consul = new Consul()
def projectHelpers = new ProjectHelpers()
def utils = new Utils()
def bitConvJava = new BITConvJava()
def notification = new Notification()
def jIRAIntegration = new JIRAIntegration()
def sqlUtils = new SqlUtils()

pipeline {

    parameters {
        string(description: 'Project key in jira', name: 'projectKey')
        string(description: 'Configuration 1c type', name: 'confType')
        string(description: 'Issue author in SD', name: 'jiraReporter')
        string(defaultValue: "", description: 'Optional. Infobase postfix', name: 'postfix')
        string(defaultValue: "", description: 'Optional. Full filepath with *bak, *cf or *dt extension or basename to create infobase from', name: 'cfdtpath')
        string(defaultValue: "false", description: 'Optional. If true - make connection to storage 1c.', name: 'need1cStorage')
        string(defaultValue: "", description: 'Optional. System parameter with issue key from SD', name: 'issueKey')
        string(defaultValue: "", description: 'Obsolete', name: 'isTestJenkinsJob')
    }

    agent { 
        label "${bitConvJava.combine1cProjectServerName(env.projectKey)}" 
    }

    options {
        timeout(time: 60000, unit: 'SECONDS') 
        buildDiscarder(logRotator(numToKeepStr:'10'))
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
                        projectKey = projectKey.toLowerCase()
                        baseId = bitConvJava.combineBaseId(confType, postfix);
                        
                        projectServerPlatform = consul.queryVal("${projectKey}/project_server_platform")
                        projectServer = consul.queryVal("${projectKey}/project_server")
                        storage1cUser = consul.queryVal("${projectKey}/storage_1c_pipeline_user")
                        storage1cPassword  = consul.queryVal("${projectKey}/storage_1c_pipeline_password")
                        storage1cTCP = consul.queryVal("${projectKey}/templatebases/${baseId}/storage1C_tcp")
                        templateBase = consul.queryVal("${projectKey}/templatebases/${baseId}/base")
                        userbaseServer = consul.queryVal("${projectKey}/users/${jiraReporter}/devbases/${baseId}/server")
                        userBaseConnString = consul.queryVal("${projectKey}/users/${jiraReporter}/devbases/${baseId}/connection_string")
                        userBase = consul.queryVal("${projectKey}/users/${jiraReporter}/devbases/${baseId}/base")
                        admin1cUser = consul.queryVal("${projectKey}/users/${jiraReporter}/devbases//${baseId}/admin_1c_user")
                        admin1cPassword = consul.queryVal("${projectKey}/users/${jiraReporter}/devbases//${baseId}/admin_1c_password")
                        slackChannel = consul.queryVal("${projectKey}/slack/${projectKey}_build_log")
                        backupPath = consul.queryVal("${projectKey}/backup_path")
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
        stage("Создание базы") {
            steps {
                timestamps {
                    script {
                        if (projectHelpers.testInfobaseConnectionRAS(userbaseServer, userBase, projectServerPlatform, admin1cUser, admin1cPassword)) {
                            projectHelpers.dropDb(utils, projectServerPlatform, userbaseServer, userBase, admin1cUser, admin1cPassword, true)
                        }
                        if (cfdtpath == confType) {
                            // Считаем, что в cfdtpath передано имя эталонной базы на проектном сервере,
                            // которую нужно скопировать.
                            sqlUtils.copyBase(userbaseServer, templateBase, userBase, backupPath)
                            projectHelpers.createDb(utils, projectServerPlatform, userbaseServer, userBase, null, true)
                        } else {
                            projectHelpers.createLoadDb(projectServerPlatform, userbaseServer, userBase, userBaseConnString, "", "",  cfdtpath)
                        }
                        
                    }
                }
            }
        }
        stage("Подключение базы к хранилищу")  {
            steps {
                timestamps {
                    script {
                        if (need1cStorage == "true") {
                            connected = projectHelpers.testInfobaseConnectionRAS(userbaseServer, userBase, projectServerPlatform)
                            if (connected) {
                                // Вероятно пустая база либо баз с пустыми пользователями.
                                // Будем подключаться к хранилищу с пустой авторизацией.
                                admin1cUser = ""
                                admin1cPassword = ""
                            }
                            else {
                                echo "It seems that we cannont connect to infobase with empty user. That's okay - we should use Administrator credentials instead"
                            }

                            projectHelpers.unbindRepo(utils, userbaseServer, userBase, projectServerPlatform, admin1cUser, admin1cPassword)
                            projectHelpers.bindRepo(utils, storage1cTCP, jiraReporter, storage1cPassword, userbaseServer, userBase, projectServerPlatform, admin1cUser, admin1cPassword)
                            projectHelpers.updateInfobase(utils, userBaseConnString, admin1cUser, admin1cPassword, projectServerPlatform)
                        }
                    }
                }
            }
        }
        stage("Добавление базы в список пользователю") {
            steps {
                timestamps {
                    script {
                        projectHelpers.addTolistTask(
                            bitConvJava.userServiceAgent(jiraReporter, projectKey),
                            projectServer,
                            userBase,
                            jiraReporter
                        )
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
