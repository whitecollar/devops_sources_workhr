@Library("shared-libraries")
import io.bit.Consul
import io.bit.BITConvJava
import io.bit.ProjectHelpers

def consul = new Consul()
def bitConvJava = new BITConvJava()
def projectHelpers = new ProjectHelpers()

pipeline {
    agent {
        label "service_NewProject"
    }

    options {
        timeout(time: 3600, unit: 'SECONDS') 
        buildDiscarder(logRotator(daysToKeepStr:'30'))
    }

    parameters {
        string(description: 'Project key in jira', name: 'projectKey')
        string(defaultValue: "", description: 'Optional. Platform 1С version. For example: 8.3.12.1529', name: 'platform1c')
        string(defaultValue: "false", description: 'Optional. Flag that server is needed. Allowed values are: true, false', name: 'needServer')
        string(defaultValue: "", description: 'Optional. Projects members via comma. For example username1,username2', name: 'projectParticipants')
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
        stage("Основной блок") {
            steps {
                timestamps {
                    script {
                        projectKey = projectKey.toLowerCase()

                        projectServer = bitConvJava.combine1cProjectServerName(projectKey)
                        slackChannel = bitConvJava.combineSlackChannel(projectKey)
                        featuresRepo = bitConvJava.combineFeaturesRepo(projectKey)
                        storage1cFolder = bitConvJava.getStorage1CFolder()
                        backupShare = bitConvJava.getBackupShare()
                        storage1cUser = bitConvJava.getStorage1cDefUserName()
                        storage1cPass = bitConvJava.getStorage1cDefUserPass()

                        if (platform1c.isEmpty()) {
                            consul.putValIfNotExists(BITConvJava.STORAGE_1C_SERVER_TCP, "${projectKey}/storage1с_server_tcp")
                            consul.putValIfNotExists(platform1c, "${projectKey}/project_server_platform")
                        } else {
                            storage1cTcp = consul.queryVal("${bitConvJava.combinePlatform1cConsulPath(platform1c)}/storage_1c_server_tcp")
                            consul.putVal(storage1cTcp, "${projectKey}/storage1с_server_tcp")
                            consul.putVal(platform1c, "${projectKey}/project_server_platform")
                        }

                        consul.putVal('', "${projectKey}/")
                        consul.putVal(projectServer, "${projectKey}/project_server")
                        consul.putVal('', "${projectKey}/templatebases/")
                        consul.putVal('', "${projectKey}/custombases/")
                        consul.putVal('', "${projectKey}/smokebases/")
                        consul.putVal(featuresRepo, "${projectKey}/features_repo")
                        consul.putVal(slackChannel, "${projectKey}/slack_channel")
                        consul.putValIfNotExists('', "${projectKey}/slack/${projectKey}_build_log")
                        consul.putVal(storage1cUser, "${projectKey}/storage_1c_pipeline_user")
                        consul.putVal(storage1cPass, "${projectKey}/storage_1c_pipeline_password")
                        consul.putVal(storage1cFolder, "${projectKey}/storage1с_server_file")
                        consul.putValIfNotExists(needServer, "${projectKey}/needServer")
                        consul.putValIfNotExists('', "${projectKey}/bddBase")
                        consul.putVal(backupShare, "${projectKey}/backup_path")
                        consul.putValIfNotExists("true", "${projectKey}/project_alive")

                        //Добавляем пользователей в проект
                        consul.putVal('', "${projectKey}/users/")

                        consul.putVal(bitConvJava.combineRabbitMQServerName(projectKey), "${projectKey}/servers/rabbitmq/dev${projectKey}rmq/server")
                        consul.putVal('/', "${projectKey}/servers/rabbitmq/dev${projectKey}rmq/vshost")
                        consul.putVal(bitConvJava.getRabbitmqDefPort(), "${projectKey}/servers/rabbitmq/dev${projectKey}rmq/port")
                        consul.putValIfNotExists('admin', "${projectKey}/servers/rabbitmq/dev${projectKey}rmq/login")
                        consul.putValIfNotExists('admin', "${projectKey}/servers/rabbitmq/dev${projectKey}rmq/password")
                    }
                }
            }
        }
    }
}