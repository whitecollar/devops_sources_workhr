@Library("shared-libraries")
import io.bit.Consul
import io.bit.ProjectHelpers
import io.bit.Utils
import io.bit.GitUtils
import io.bit.JenkinsJobs
import io.bit.BITConvJava
import io.bit.Notification
import io.bit.JIRAIntegration

def consul = new Consul()
def projectHelpers = new ProjectHelpers()
def utils = new Utils()
def gitUtils = new GitUtils()
def bitConvJava = new BITConvJava()
def jenkinsJobs = new JenkinsJobs()
def notification = new Notification()
def jIRAIntegration = new JIRAIntegration()

pipeline {

    parameters {
        string(description: 'Project key in jira', name: 'projectKey')
        string(description: 'Configuration 1c type', name: 'confType')
        string(description: 'Update type for infobase. Allowed values are: LOAD, MERGE_CFG, MERGE_DISTRIBUTION', name: 'mergeType')
        string(defaultValue: "", description: 'Optional. Infobase postfix', name: 'postfix')
        string(defaultValue: "", description: 'Optional. Full filepath with *cf or *dt extension to create infobase from', name: 'cfdtpath')
        string(defaultValue: "true", description: 'Optional. If true -  тcreate new storage 1C', name: 'need1cStorage')
        string(defaultValue: "", description: 'Optional. System parameter with issue key from SD', name: 'issueKey')
        string(defaultValue: "", description: 'Optional. Issue author from SD', name: 'jiraReporter')
        string(defaultValue: "", description: 'Obsolete', name: 'isTestJenkinsJob')
    }

    agent { 
        label "${bitConvJava.combine1cProjectServerName(env.projectKey)}" 
    }

    options {
        timeout(time: 60000, unit: 'SECONDS') 
        buildDiscarder(logRotator(daysToKeepStr:'30'))
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
        stage("Заполнение параметров в Consul") {
            steps {
                timestamps {
                    script {
                         
                        if (!cfdtpath.isEmpty() && !fileExists(cfdtpath)) {
                            utils.raiseError("Ошибка. Не найден файл выгрузки базы ${cfdtpath}. Необходимо указать полный путь к файлу, включая само имя файла")
                        }
                        
                        projectKey = projectKey.toLowerCase()
                        jiraReporter = jiraReporter.toLowerCase()

                        jenkinsJobs.fillConsulTemplateBase(projectKey, confType, postfix, mergeType, "true", issueKey, jiraReporter, isTestJenkinsJob)

                        baseId = bitConvJava.combineBaseId(confType, postfix)

                        projectKey = projectKey.toLowerCase()
                        projectServerPlatform = consul.queryVal("${projectKey}/project_server_platform")
                        projectServer = consul.queryVal("${projectKey}/project_server")
                        templateBase = consul.queryVal("${projectKey}/templatebases/${baseId}/base")
                        users = consul.queryList("${projectKey}/users")
                        connString = consul.queryVal("${projectKey}/templatebases/${baseId}/connection_string")
                        admin1cUser = consul.queryVal("${projectKey}/templatebases/${baseId}/admin_1c_user")
                        admin1cPassword = consul.queryVal("${projectKey}/templatebases/${baseId}/admin_1c_password")

                        addRepo = bitConvJava.getADDRepo()
                        addRepoFolder = "build/add"
                        shortPlatform = utils.shortPlatformName(projectServerPlatform)
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
        stage("Создание эталонной базы") {
            steps {
                timestamps {
                    script {
                        if (projectHelpers.testInfobaseConnectionRAS(projectServer, templateBase, projectServerPlatform, admin1cUser, admin1cPassword)) {
                            projectHelpers.dropDb(utils, projectServerPlatform, projectServer, templateBase, admin1cUser, admin1cPassword, true)
                        }
                        projectHelpers.createLoadDb(projectServerPlatform, projectServer, templateBase, connString, "", "",  cfdtpath)
                    }
                }
            }
        }

        stage("Добавление базы в список пользователям") {
            steps {
                timestamps {
                    script {

                        if (projectHelpers.isDraftJob()) {
                            return 
                        }

                        users = consul.queryList("${projectKey}/users")
                        for (def user : users) {
                            projectHelpers.addTolistTask(
                                bitConvJava.userServiceAgent(user, projectKey),
                                projectServer,
                                templateBase,
                                user
                            )
                        }
                    }
                }
            }
        }

        stage("Создание настроек ADD в эталонной базе") {
            steps {
                timestamps {
                    script {
                        gitUtils.checkoutSCM(addRepo, addRepoFolder, "master")
                        returnCode = utils.cmd("runner vanessa --settings ci/bddPipeiline/runnerParams/vrunner.json --v8version ${shortPlatform} --ibconnection ${connString} --db-user ${admin1cUser} --db-pwd ${admin1cPassword} --vanessasettings ./ci/bddPipeiline/runnerParams/VBParamsEmpty.json --pathvanessa build/add/bddRunner.epf")
                    }
                }
            }
        }

        stage("Создание основного хранилища") {
            steps {
                timestamps {
                    script {
                        if (need1cStorage != "true") {
                            return
                        }
                        if (projectHelpers.testInfobaseConnectionRAS(projectServer, templateBase, projectServerPlatform)) {
                            // Это пустая база, хранилище не создаем в любом случае
                            echo "skip creating storage 1C due empty Administrator user in ${templateBase}"
                            return 
                        }

                        jenkinsJobs.createStorage1c(projectKey, baseId, issueKey, jiraReporter)
                    }
                }
            }
        }

        stage("Создание  хранилища для расширения") {
            steps {
                timestamps {
                    script {
                        if (need1cStorage != "true") {
                            return
                        }
                        if (projectHelpers.testInfobaseConnectionRAS(projectServer, templateBase, projectServerPlatform)) {
                            // Это пустая база, хранилище не создаем в любом случае
                            echo "skip creating extension storage 1C due empty Administrator user in ${templateBase}"
                            return 
                        }

                        jenkinsJobs.createStorage1cExt(projectKey, baseId, issueKey, jiraReporter)
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