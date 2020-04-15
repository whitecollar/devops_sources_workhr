#!groovy
@Library("shared-libraries")
import io.bit.Consul
import io.bit.ProjectHelpers
import io.bit.BITConvJava
import io.bit.JenkinsSteps
import io.bit.SqlUtils
import io.bit.Notification
import io.bit.Utils

def loadcfgTasks = [:]
def updatedbTasks = [:]
def dropUsersTasks = [:]
def backupTasks = [:]
def consul = new Consul()
def projectHelpers = new ProjectHelpers()
def bitConvJava = new BITConvJava()
def jenkinsSteps = new JenkinsSteps()
def notification = new Notification()
def utils = new Utils()

pipeline {

    options {
        timeout(time: 28800, unit: 'SECONDS') 
        buildDiscarder(logRotator(daysToKeepStr:'10'))
    }
	
    parameters {
        string(description: 'Project key in jira', name: 'projectKey')
        string(defaultValue: "", description: 'Optional. Base filter', name: 'templateBase')
        string(defaultValue: "", description: 'Optional. System parameter with issue key from SD', name: 'issueKey')
        string(defaultValue: "", description: 'Optional. Issue author in SD', name: 'jiraReporter')
    }

    agent { 
        label "${bitConvJava.combine1cProjectServerName(env.projectKey)}" 
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

        stage('Запрос параметров из консула') {
            steps {
                timestamps {
                    script {

                        // создаем пустые каталоги
                        dir ('build') {
                            writeFile file:'dummy', text:''
                        }
                          // создаем пустые каталоги
                        dir ('build/filebases') {
                            writeFile file:'dummy', text:''
                        }

                        projectKey = projectKey.toLowerCase()
                        mainExtension = bitConvJava.getMainExtension()

                        platform = consul.queryVal("${projectKey}/project_server_platform")
                        storageUser = consul.queryVal("${projectKey}/storage_1c_pipeline_user")
                        storagePwd = consul.queryVal("${projectKey}/storage_1c_pipeline_password")
                        slackChannel = consul.queryVal("${projectKey}/slack/${projectKey}_build_log")
                        bddType = "UPDATE_TEMPLATE_BASES"
                        
                        templateBases = consul.queryList("${projectKey}/templatebases")

                        currentBuild.result = "SUCCESS"
                    }
                }
            }
        }

        stage('Отправка уведомления в slack') {
            steps {
                timestamps {
                    script {
                        if (!jiraReporter.isEmpty()) {
                            threadId = notification.sendSlackStartBuild(slackChannel, bddType, jiraReporter, issueKey)
                        }
                        currentBuild.result = "SUCCESS"
                    }
                }
            }
        }

        stage('Подготовка параметров бекапов') {
            steps {
                timestamps {
                    script {
                        backupPathInfo = "Для эталонных баз созданы следующие бекапы:"
                        for (def baseId : templateBases) {
                            if (!templateBase.isEmpty() && baseId != templateBase) {
                                continue
                            }
                            
                            templateBase = consul.queryVal("${projectKey}/templatebases/${baseId}/base")
                            templateBaseServer  = consul.queryVal("${projectKey}/templatebases/${baseId}/server")
                            backupPath = "${consul.queryVal("${projectKey}/backup_path")}\\${templateBase}_${utils.currentDateStamp()}.bak"
                            backupPathInfo += "\n${backupPath}"

                            backupTasks["backupTask_${templateBase}"] = backuptask(templateBaseServer, templateBase, backupPath)
                        }
                        backupPathInfo += "\nБекапы автоматически удаляются через 24 часа"

                        currentBuild.result = "SUCCESS"
                    }
                }
            }
        }
        stage('Подготовка параметров обновления') {
            steps {
                timestamps {
                    script {
                        
                        singleCfgEDT = jenkinsSteps.applySingleStorageEDT(templateBases, platform, "master", null)
                        for (def baseId : templateBases) {

                            if (!templateBase.isEmpty() && baseId != templateBase) {
                                continue
                            }
                            
                            mergeSettings = consul.queryVal("${projectKey}/templatebases/${baseId}/mergesettings")
                            mergeType = consul.queryVal("${projectKey}/templatebases/${baseId}/mergetype")
                            storageTCP = consul.queryVal("${projectKey}/templatebases/${baseId}/storage1C_tcp")
                            storageEDT = consul.queryVal("${projectKey}/templatebases/${baseId}/storage_edt")
                            admin1cUser = consul.queryVal("${projectKey}/templatebases/${baseId}/admin_1c_user")
                            admin1cPassword = consul.queryVal("${projectKey}/templatebases/${baseId}/admin_1c_password")
                            templateBaseServer  = consul.queryVal("${projectKey}/templatebases/${baseId}/server")
                            templateBase = consul.queryVal("${projectKey}/templatebases/${baseId}/base")
                            templateBaseConnString = consul.queryVal("${projectKey}/templatebases/${baseId}/connection_string")
                            storageExtTCP = consul.queryVal("${projectKey}/templatebases/${baseId}/main_extension_storage1C_tcp")

                            projectDir = "${env.WORKSPACE}/build/edt/${baseId}"
                            sourcesPath = "${env.WORKSPACE}/build/exported/${baseId}";
                            cfgfileName = "${env.WORKSPACE}/build/${baseId}_temp.cf"
                            cfgExtfileName = "${env.WORKSPACE}/build/${baseId}_temp.cfe"
                            cfgTempFileBase = "${env.WORKSPACE}/build/filebases/${baseId}"
                            distibFolder = "${env.WORKSPACE}/build/distib/${baseId}_distrib"
                            mergeSettingsPath = "${projectDir}/${projectKey}/${mergeSettings}"
    
                            loadcfgTasks["loadcfgTask_${templateBase}"] = jenkinsSteps.loadTestBaseTask(
                                platform, 
                                storageTCP, 
                                storageEDT, 
                                templateBaseConnString, 
                                templateBaseServer, 
                                templateBase, 
                                admin1cUser, 
                                admin1cPassword, 
                                storageUser, 
                                storagePwd, 
                                mergeType, 
                                baseId, 
                                projectDir, 
                                sourcesPath, 
                                cfgfileName, 
                                mergeSettingsPath, 
                                singleCfgEDT, 
                                cfgTempFileBase, 
                                distibFolder, 
                                storageExtTCP, 
                                mainExtension,
                                "master"
                            )
                            dropUsersTasks["dropUsersTask_${templateBase}"] =  jenkinsSteps.dropUsersTask(
                                templateBaseServer,
                                platform,
                                admin1cUser,
                                admin1cPassword,
                                templateBase
                            )
                            updatedbTasks["updatedbTask_${templateBase}"] = jenkinsSteps.updatedbTask(
                                templateBaseConnString, 
                                templateBaseServer, 
                                templateBase, 
                                admin1cUser, 
                                admin1cPassword, 
                                platform, 
                                mainExtension
                            )
                        }
                        currentBuild.result = "SUCCESS"
                    }
                }
            }
        }
        stage('Запуск обновления баз') {
            steps {
                timestamps {
                    script {
                        parallel backupTasks
                        parallel loadcfgTasks
                        parallel dropUsersTasks
                        parallel updatedbTasks

                        currentBuild.result = "SUCCESS"
                    }
                }
            }
        }
    }
    
    post {
        always {
            script {
                if (!jiraReporter.isEmpty()) {
                    notification.sendSlackFinishBuild(
                        slackChannel,
                        bddType,
                        currentBuild.result, 
                        null, 
                        jiraReporter, 
                        threadId,
                        currentBuild.result == "SUCCESS" ? backupPathInfo : "",
                        issueKey
                    )
                }

                projectHelpers.beforeEndJob()
            }
        }
    }
}

def backuptask(projectServer, templateBase, backupPath) {
    return {
        stage("Резервное копирование базы ${projectServer}\\${templateBase}") {
            timestamps {
                sqlUtils = new SqlUtils()
                sqlUtils.backupDb(projectServer, templateBase, backupPath)
            }
        }
    }
}