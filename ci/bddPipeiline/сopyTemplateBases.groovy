#!groovy
@Library("shared-libraries")
import io.bit.Consul
import io.bit.Utils
import io.bit.SqlUtils
import io.bit.ProjectHelpers
import io.bit.BITConvJava
import io.bit.GitUtils

def backuptasks = [:]
def restoretasks = [:]
def repotasks = [:]
def createWebPubTasks = [:]
def dropdbtasks = [:]
def addTolistTasks = [:]
def consul = new Consul()
def utils = new Utils()
def bitConvJava = new BITConvJava()
def projectHelpers = new ProjectHelpers()

pipeline {
	
    parameters {
        string(description: 'Jenkins agent', name: 'jenkinsAgent')
        string(description: 'Project key in jira', name: 'projectKey')
        string(description: 'User to copy bases for', name: 'user')
        string(description: 'Issue type in SD. Allowed values are COPY_TEMPLATE_BASE, COPY_DEV_BASE, UPDATE_FROM_STORAGE, UPDATE_FROM_DEVBASE', name: 'taskType')
        string(defaultValue: "", description: 'Optional. Base filter', name: 'baseIdFilter')
        string(defaultValue: "", description: 'Optional.Issue author in SD', name: 'jiraReporter')
        string(defaultValue: "", description: 'Optional. System parameter with issue key from SD', name: 'issueKey')
        string(defaultValue: "", description: 'Obsolete', name: 'isTestJenkinsJob')
    }

    options {
        timeout(time: 28800, unit: 'SECONDS') 
        buildDiscarder(logRotator(daysToKeepStr:'10'))
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
        stage('Подготовка параметров копирования баз') {
            steps {
                timestamps {
                    script {
                        userserviceAgent = bitConvJava.userServiceAgent(user, projectKey)
                        devserviceAgent = bitConvJava.userServiceAgent(bitConvJava.getUserJenkins(), projectKey);
                        
                        projectKey = projectKey.toLowerCase()
                        platform = consul.queryVal("${projectKey}/project_server_platform")
                        projectServer = consul.queryVal("${projectKey}/project_server")
                        mainExtension = bitConvJava.getMainExtension()
                        
                        templatebases = consul.queryList("${projectKey}/templatebases")
                        addSmokeBases(projectKey, templatebases, user)

                        for (def baseId : templatebases) {
                            if (!baseIdFilter.isEmpty() && baseId != baseIdFilter) {
                                continue
                            }
                            
                            isTemplateBase = (consul.queryVal("${projectKey}/templatebases/${baseId}/base", true) != null)
                            templateGroup = isTemplateBase ? "${projectKey}/templatebases" : "${projectKey}/smokebases"
                            testGroup = isTemplateBase ? "${projectKey}/users/${user}/testbases" : "${projectKey}/users/${user}/smokebases"

                            template_base = consul.queryVal("${templateGroup}/${baseId}/base")
                            template_connection_string = consul.queryVal("${templateGroup}/${baseId}/connection_string")
                            admin1cUser = consul.queryVal("${templateGroup}/${baseId}/admin_1c_user")
                            admin1cPassword = consul.queryVal("${templateGroup}/${baseId}/admin_1c_password")
                            storage1cTCP = consul.queryVal("${templateGroup}/${baseId}/storage1C_tcp", true)

                            storage1cUser = consul.queryVal("${projectKey}/storage_1c_pipeline_user")
                            storage1cPassword = consul.queryVal("${projectKey}/storage_1c_pipeline_password")
                            userBase = consul.queryVal("${testGroup}/${baseId}/base")
                            if (taskType == "COPY_DEV_BASE") {
                                userbase_server = consul.queryVal("${projectKey}/users/${user}/devbases/${baseId}/server")
                                userbase_connstring = consul.queryVal("${projectKey}/users/${user}/devbases/${baseId}/connection_string")
                                userBase = consul.queryVal("${projectKey}/users/${user}/devbases/${baseId}/base")
                            } else {
                                userbase_server = consul.queryVal("${testGroup}/${baseId}/server")
                                userbase_connstring = consul.queryVal("${testGroup}/${baseId}/connection_string")
                                userBase = consul.queryVal("${testGroup}/${baseId}/base")
                            } 
                            backupPath = "${consul.queryVal("${projectKey}/backup_path")}/${userBase}_${utils.currentDateStamp()}.bak"
                            publicpath = bitConvJava.combineWebPubFolder(userBase)

                            // Удаление тестовой базы
                            dropdbtasks["dropdbtask_${userBase}"] = dropdbtask (
                                projectServer,
                                platform,
                                admin1cUser,
                                admin1cPassword,
                                userBase
                            )
                            // Добавление тестовой базы в список
                            if (utils.pingJenkinsAgent(userserviceAgent)) {
                                // Если выбрано тестировать на временных базах, то добавление баз в список пропускается, т.к. userserviceAgent не существует
                                addTolistTasks["addTolistTask_${userBase}"] = addTolistTask(
                                    userserviceAgent,
                                    projectServer,
                                    userBase,
                                    user
                                )
                            } else {
                                echo "Adding bases to ibases.v8i skipped since user ${user} does not have jenkins node ${userserviceAgent}"
                            }
                            // Создание бекапа базы
                            backuptasks["backuptask_${userBase}"] = backuptask(
                                projectServer,
                                template_base,
                                backupPath
                            )
                            // Восстановление базы из бекапа
                            restoretasks["restoretask_${userBase}"] = restoretask(
                                platform,
                                projectServer,
                                userBase,
                                backupPath
                            )
                            // Подключение базы к хранилищу 1С
                            if (taskType == "COPY_DEV_BASE") {
                                repotasks["repotask_${userBase}"] =  repotask(platform,
                                    userbase_connstring,
                                    projectServer, 
                                    userBase, 
                                    storage1cTCP,
                                    user,
                                    storage1cPassword, 
                                    admin1cUser, 
                                    admin1cPassword
                                )
                            }
                            // Публикация веб-клиента и сервисов
                            createWebPubTasks["createWebPubTask_${userBase}"] = createWebPubTask(
                                devserviceAgent, 
                                projectKey,
                                platform, 
                                projectServer, 
                                userBase, 
                                userbase_connstring,
                                admin1cUser, 
                                admin1cPassword, 
                                publicpath,
                                user
                            )
                        }
                    }
                }
            }
        }
        stage('Запуск копирования баз') {
            steps {
                timestamps {
                    script {
                        parallel dropdbtasks
                        parallel addTolistTasks
                        parallel backuptasks
                        parallel restoretasks
                        parallel repotasks
                        parallel createWebPubTasks
                    }
                }
            }
        }
    }
}

def backuptask(projectServer, template_base, backupPath) {
    return {
        stage("Резервное копирование базы ${projectServer}\\${template_base}") {
            timestamps {
                utils = new Utils()
                prHelpers = new ProjectHelpers()
                sqlUtils = new SqlUtils()

                sqlUtils.checkDb(projectServer, template_base)
                sqlUtils.backupDb(projectServer, template_base, backupPath)
            }
        }
    }
}

def restoretask(platform, projectServer, userBase, backupPath) {
    return {
        stage("Восстановление базы ${projectServer}\\${userBase}") {
            timestamps {
                utils = new Utils()
                prHelpers = new ProjectHelpers()
                sqlUtils = new SqlUtils()
                sqlUtils.createEmptyDb(projectServer, userBase)
                sqlUtils.restoreDb(projectServer, userBase, backupPath)
                try {
                    prHelpers.createDb(utils, platform, projectServer, userBase, null, true)
                } catch (except) {
                    echo "Error happened when creating infobase with RAS equal true. Let's try again via plain command line mode"
                    prHelpers.createDb(utils, platform, projectServer, userBase, null, false)
                }
                prHelpers.clearBackups(utils, backupPath)
            }
        }
    }
}

def dropdbtask(projectServer, platform, admin1cUser, admin1cPassword, userBase) {
    return {
        stage("Удаление базы в кластере ${userBase}") {
            timestamps {
                def projectHelpers = new ProjectHelpers()
                def utils = new Utils()

                projectHelpers.dropDb(utils, platform, projectServer, userBase, admin1cUser, admin1cPassword)
            }
        }
    }
}

def addTolistTask(jenkinsAgent, server, base, user) {
    return {
        node (jenkinsAgent) {
            stage("Добавление в список ${base}") {
                timestamps {
                    def projectHelpers = new ProjectHelpers()
                    def utils = new Utils()

                    checkout scm

                    projectHelpers.clearClientCacheFilter(server, base)
                    projectHelpers.addInfobaseToList(utils, server, base, user)
                }
            }
        }
    }
}

def repotask(platform, userBaseConnString, server, infobase, storage1cTCP, storage1cUser, storage1cPassword, admin1cUser, admin1cPassword) {
    return {
        stage("Подключение базы к хранилищу ${server}\\${infobase}") {
            timestamps {
                utils = new Utils()
                prHelpers = new ProjectHelpers()
                prHelpers.unbindRepo(utils, server, infobase, platform, admin1cUser, admin1cPassword) 
                prHelpers.bindRepo(utils, storage1cTCP, storage1cUser, storage1cPassword, server, infobase, platform, admin1cUser, admin1cPassword)
                prHelpers.updateInfobase(utils, userBaseConnString, admin1cUser, admin1cPassword, platform)
            }
        }
    }
}

def createWebPubTask(jenkinsNode, projectKey, platform, projectServer, infobase, userbase_connstring, admin1c, passw1c, publicpath, user) {
    return {
        node (jenkinsNode) {
            stage("Создание web-публикации для ${infobase}") {
                timestamps {
                    prHelpers = new ProjectHelpers()
                    gitUtils = new GitUtils()

                    gitUtils.checkoutSCM(scm.userRemoteConfigs[0].url, "", "master")

                    prHelpers.createWebPublication(projectServer, platform, infobase, admin1c, passw1c, infobase, publicpath)
                    prHelpers.preparingVannessaAdnPublishWS(userbase_connstring, admin1c, passw1c, projectKey, user, "${publicpath}/default.vrd")
                }
            }
        }
    }
}

def addSmokeBases(projectKey, templatebases, user) {
    consul = new Consul()
    bitConvJava = new BITConvJava()

    if (user != bitConvJava.getUserJenkins()) {
        return
    }

    smokebases = consul.queryList("${projectKey}/smokebases")
    for (smokeBaseId in smokebases) {
        for (templBaseId in templatebases) {
            if (smokeBaseId != templBaseId) {
                templatebases.add(smokeBaseId)
            }
        }
    }
}