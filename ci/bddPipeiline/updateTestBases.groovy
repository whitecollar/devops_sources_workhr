#!groovy
@Library("shared-libraries")
import io.bit.Consul
import io.bit.Utils
import io.bit.ProjectHelpers
import io.bit.BITConvJava
import io.bit.JenkinsSteps

def loadcfgTasks = [:]
def updatedbTasks = [:]
def dropUsersTasks = [:]
def consul = new Consul()
def utils = new Utils()
def projectHelpers = new ProjectHelpers()
def bitConvJava = new BITConvJava()
def jenkinsSteps = new JenkinsSteps()

def UPDATE_FROM_STORAGE = "UPDATE_FROM_STORAGE"
def UPDATE_FROM_DEVBASE = "UPDATE_FROM_DEVBASE"

pipeline {

    options {
        timeout(time: 28800, unit: 'SECONDS') 
        buildDiscarder(logRotator(daysToKeepStr:'10'))
        skipDefaultCheckout true
    }
	
    parameters {
        string(description: 'Jenkins agent', name: 'jenkinsAgent')
        string(description: 'Project key in jira', name: 'projectKey')
        string(description: 'User which bases need to update', name: 'user')
        string(description: 'Issue type in SD. Allowed values are COPY_TEMPLATE_BASE, COPY_DEV_BASE, UPDATE_FROM_STORAGE, UPDATE_FROM_DEVBASE', name: 'taskType')
        string(defaultValue: "master", description: 'Optional. Branch of sources repository stored in EDT (currently only for adapter)', name: 'edtBranch')
        string(defaultValue: "", description: 'Optional. Base filter', name: 'baseIdFilter')
        string(defaultValue: "", description: 'Optional. System parameter with issue key from SD', name: 'issueKey')
        string(defaultValue: "", description: 'Optional. Issue author in SD', name: 'jiraReporter')
        string(defaultValue: "", description: 'Obsolete', name: 'isTestJenkinsJob')
    }

    agent { label "${env.jenkinsAgent}" }

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
        stage('Кастомный Checkout SCM') {
            steps {
                deleteDir()
                checkout scm
            }
        }
        stage('Подготовка параметров копирования баз') {
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

                        if  (!(taskType == UPDATE_FROM_STORAGE || taskType == UPDATE_FROM_DEVBASE)) {
                            utils.raiseError("Неправильный параметр taskType = " + taskType)
                        }

                        projectKey = projectKey.toLowerCase()
                        user = user.toLowerCase()
                        mainExtension = bitConvJava.getMainExtension()

                        platform = consul.queryVal("${projectKey}/project_server_platform")
                        storageUser = consul.queryVal("${projectKey}/storage_1c_pipeline_user")
                        storagePwd = consul.queryVal("${projectKey}/storage_1c_pipeline_password")
                        
                        templateBases = consul.queryList("${projectKey}/templatebases")
                        singleCfgEDT = jenkinsSteps.applySingleStorageEDT(templateBases, platform, edtBranch, user);
                        for (def baseId : templateBases) {

                            if (!baseIdFilter.isEmpty() && baseId != baseIdFilter) {
                                continue
                            }
                            
                            mergeSettings = consul.queryVal("${projectKey}/templatebases/${baseId}/mergesettings")
                            mergeType = consul.queryVal("${projectKey}/templatebases/${baseId}/mergetype")
                            storageTCP = consul.queryVal("${projectKey}/templatebases/${baseId}/storage1C_tcp")
                            storageEDT = consul.queryVal("${projectKey}/templatebases/${baseId}/storage_edt")
                            admin1cUser = consul.queryVal("${projectKey}/templatebases/${baseId}/admin_1c_user")
                            admin1cPassword = consul.queryVal("${projectKey}/templatebases/${baseId}/admin_1c_password")
                            userBaseServer  = consul.queryVal("${projectKey}/users/${user}/testbases/${baseId}/server")
                            userBase = consul.queryVal("${projectKey}/users/${user}/testbases/${baseId}/base")
                            userBaseConnString = consul.queryVal("${projectKey}/users/${user}/testbases/${baseId}/connection_string")
                            devBase = consul.queryVal("${projectKey}/users/${user}/devbases/${baseId}/base")
                            devBaseConnString = consul.queryVal("${projectKey}/users/${user}/devbases/${baseId}/connection_string")
                            storageExtTCP = consul.queryVal("${projectKey}/templatebases/${baseId}/main_extension_storage1C_tcp")

                            projectDir = "${env.WORKSPACE}/build/edt/${baseId}"
                            sourcesPath = "${env.WORKSPACE}/build/exported/${baseId}";
                            cfgfileName = "${env.WORKSPACE}/build/${baseId}_temp.cf"
                            cfgExtfileName = "${env.WORKSPACE}/build/${baseId}_temp.cfe"
                            cfgTempFileBase = "${env.WORKSPACE}/build/filebases/${baseId}"
                            distibFolder = "${env.WORKSPACE}/build/distib/${baseId}_distrib"
                            mergeSettingsPath = "${projectDir}/${projectKey}/${mergeSettings}"
    
                            if (taskType == UPDATE_FROM_DEVBASE
                                && projectHelpers.testInfobaseConnectionRAS(userBaseServer, devBase, platform, admin1cUser, admin1cPassword)) {

                                loadcfgTasks["loadcfgTask_${userBase}"] = jenkinsSteps.loadDevBaseTask(
                                    platform, 
                                    userBaseConnString, 
                                    devBaseConnString, 
                                    userBaseServer, 
                                    userBase, 
                                    admin1cUser, 
                                    admin1cPassword, 
                                    cfgfileName, 
                                    mainExtension, 
                                    cfgExtfileName, 
                                    devBase
                                )
                            } else {
                                loadcfgTasks["loadcfgTask_${userBase}"] = jenkinsSteps.loadTestBaseTask(
                                    platform, 
                                    storageTCP, 
                                    storageEDT, 
                                    userBaseConnString, 
                                    userBaseServer, 
                                    userBase, 
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
                                    edtBranch
                                )
                            }

                            dropUsersTasks["dropUsersTask_${userBase}"] =  jenkinsSteps.dropUsersTask(
                                userBaseServer,
                                platform,
                                admin1cUser,
                                admin1cPassword,
                                userBase
                            )

                            updatedbTasks["updatedbTask_${userBase}"] = jenkinsSteps.updatedbTask(
                                userBaseConnString, 
                                userBaseServer, 
                                userBase, 
                                admin1cUser, 
                                admin1cPassword, 
                                platform, 
                                mainExtension
                            )
                        }
                    }
                }
            }
        }
        stage('Запуск обновления баз') {
            steps {
                timestamps {
                    script {
                        parallel loadcfgTasks
                        parallel dropUsersTasks
                        parallel updatedbTasks
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
