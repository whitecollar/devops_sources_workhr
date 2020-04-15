#!groovy
@Library("shared-libraries")
import io.bit.Consul
import io.bit.JenkinsJobs
import io.bit.Notification
import io.bit.ProjectHelpers
import io.bit.BITConvJava
import io.bit.JIRAIntegration
import io.bit.Utils

def UPDATE_FROM_STORAGE = "UPDATE_FROM_STORAGE"
def UPDATE_FROM_DEVBASE = "UPDATE_FROM_DEVBASE"
def RUN_ALL_TESTS = "RUN_ALL_TESTS"
def consul = new Consul()
def notification = new Notification()
def jenkinsJobs = new JenkinsJobs()
def projectHelpers = new ProjectHelpers()
def bitConvJava = new BITConvJava()
def jiraIntegration = new JIRAIntegration()
def utils = new Utils()
def allureJobUrls = [:]

pipeline {

    options {
        timeout(time: 28800, unit: 'SECONDS') 
        buildDiscarder(logRotator(daysToKeepStr:'10'))
    }

    parameters {
        string(description: 'Project key in jira', name: 'projectKey')
        string(description: 'Issue type in SD. Allowed values are COPY_TEMPLATE_BASE, COPY_DEV_BASE, UPDATE_FROM_STORAGE, UPDATE_FROM_DEVBASE', name: 'taskType')
        string(description: 'Issue author in SD', name: 'jiraReporter')
        string(defaultValue: "master", description: 'Branch of features repository', name: 'featuresBranch')
        string(defaultValue: "master", description: 'Optional. Branch of sources repository stored in EDT (currently only for adapter)', name: 'edtBranch')
        string(defaultValue: "", description: 'Optional. Test type. Allowed values are: RUN_ALL_TESTS', name: 'testType')
        string(defaultValue: "", description: 'Optional. Base filter', name: 'templateBase')
        string(defaultValue: "", description: 'Optional. System parameter with issue key from SD', name: 'issueKey')
        string(defaultValue: "false", description: 'Optional. If set to true then bdd test will be executed on a temporary user that will be removed after build', name: 'useTempUser')
        string(defaultValue: "", description: 'Obsolete', name: 'isTestJenkinsJob')
    }

    agent {
        label "service_NewProject"
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

	                    jiraReporter = jiraReporter.toLowerCase()
                        projectKey = projectKey.toLowerCase()
                        userAgent = jiraReporter
                        jenkinsUser = bitConvJava.getUserJenkins()
                        userForTests = useTempUser == "true" ? bitConvJava.generateTempUser(jiraReporter) : jiraReporter
                        bddType = taskType
                        if (!testType.isEmpty()) {
                            bddType = testType
                        }
                        bddTestsFailed = false
                        allureJobUrl = null
                        tempbasesInfo = ""

                        devAgent = consul.queryVal("${projectKey}/project_server")
                        slackChannel = consul.queryVal("${projectKey}/slack/${projectKey}_build_log")
                        
                        currentBuild.result = "SUCCESS"
                    }
                }
            }
        }
        stage('Отправка уведомления в slack') {
            steps {
                timestamps {
                    script {
                        threadId = notification.sendSlackStartBuild(slackChannel, bddType, jiraReporter, issueKey)
                        currentBuild.result = "SUCCESS"
                    }
                }
            }
        }
        stage('Создание временного пользователя') {
            steps {
                timestamps {
                    script {
                        if (useTempUser == "true") {
                            jenkinsJobs.fillConsulUser(projectKey, userForTests, issueKey, jiraReporter, isTestJenkinsJob)
                            jenkinsJobs.addUserRabbitMQ(projectKey, userForTests, issueKey, jiraReporter, isTestJenkinsJob) 

                            tempbases = consul.queryList("${projectKey}/users/${userForTests}/testbases", true)
                            tempbasesInfo = ""
                            if (tempbases.size() > 0) {
                                tempbasesInfo += "\n Сборка выполнена на временных базах:"
                                for (def baseId : tempbases) {
                                    tempbasesInfo += "\n${consul.queryVal("${projectKey}/users/${userForTests}/testbases/${baseId}/conn_string1C")}"
                                }
                            }
                        }
                    }
                }
            }
        }
        stage('Копирование эталонных баз') {
            steps {
                timestamps {
                    script {
                        agentForUser = devAgent
                        jenkinsJobs.buildCopyTemplateBases(agentForUser, projectKey, userForTests, templateBase, taskType, issueKey, jiraReporter, isTestJenkinsJob)
                        currentBuild.result = "SUCCESS"
                    }
                }
            }
        }
        stage('Обновление хранилища конфигураций') {
            steps {
                timestamps {
                    script {
                        agentForUser = devAgent
                        if (taskType == UPDATE_FROM_STORAGE || taskType == UPDATE_FROM_DEVBASE) {
                            jenkinsJobs.buildUpdateTestBases(agentForUser, projectKey, userForTests, templateBase, taskType, edtBranch, issueKey, jiraReporter, isTestJenkinsJob)
                        }

                        currentBuild.result = "SUCCESS"
                    }
                }
            }
        }
        stage('Выполнение  BDD тестов') {
            steps {
                timestamps {
                    script {
                        if (testType != RUN_ALL_TESTS) {
                            return
                        }
                        agentForUser = userAgent
                        if (jiraReporter == jenkinsUser) {
                            agentForUser = devAgent
                        }
                        jobResult = jenkinsJobs.buildRunBddTests(agentForUser, projectKey, userForTests, featuresBranch, issueKey, jiraReporter, isTestJenkinsJob)
                        bddTestsFailed = jobResult.buildVariables.bddTestsFailed

                        echo "ADD tests were completed and  env.bddTestsFailed was set to ${bddTestsFailed}"

                        currentBuild.result = jobResult.result
                        allureJobUrls["Отчет bdd"] = jobResult.absoluteUrl

                        currentBuild.result = "SUCCESS"
                    }
                }
            }
        }
        stage('Выполнение smoke тестов') {
            steps {
                timestamps {
                    script {
                        if (testType != RUN_ALL_TESTS) {
                            return
                        }
                        // Необходимо проверять оба литерала, т.к. из-за особенности дженкинса переменная bddTestsFailed возвращается 
                        // из вложеннного пайплайна с типом строка, даже если в родительском пайплайне она инициализируется с типом булево.
                        if (bddTestsFailed == "true" || bddTestsFailed == true) {
                            return
                        }
                        agentForUser = userAgent
                        if (jiraReporter == jenkinsUser) {
                            agentForUser = devAgent
                        }
                        jobResult = jenkinsJobs.buildRunSmokeTests(agentForUser, projectKey, featuresBranch, issueKey, jiraReporter)
                        bddTestsFailed = jobResult.buildVariables.bddTestsFailed

                        echo "Smoke tests were completed and  env.bddTestsFailed was set to ${bddTestsFailed}"

                        currentBuild.result = jobResult.result
                        allureJobUrls["Отчет smoke"] = jobResult.absoluteUrl

                        currentBuild.result = "SUCCESS"
                    }
                }
            }
        }
        stage('Очистка временного пользователя') {
            steps {
                timestamps {
                    script {
                        agentForUser = devAgent
                        if (useTempUser == "true") {

                            jenkinsJobs.buildClearTempUser(agentForUser, projectKey, userForTests, issueKey, jiraReporter)
                        }

                        currentBuild.result = "SUCCESS"
                    }
                }
            }
        }
    }
    
    post {
        always {
            script {  
                if (jiraReporter == bitConvJava.getUserJenkins()) {
                    // Считаем, что сборка под jenkins-ом - это ночная сборка по расписанию

                    issueKey = projectHelpers.createJiraBugIfFailed(
                        projectKey,  
                        issueKey.isEmpty() ? null : issueKey, 
                        jiraIntegration.getEpicAutomaticTesting(), 
                        "Упала ночная сборка проекта ${projectKey} от ${utils.currentDateWithFormat("dd.MM.yyyy")}"
                    )
                }

                echo "got bddTestsFailed with type of ${bddTestsFailed.getClass()}"
                // Необходимо проверять оба литерала, т.к. из-за особенности дженкинса переменная bddTestsFailed возвращается 
                // из вложеннного пайплайна с типом строка, даже если в родительском пайплайне она инициализируется с типом булево.
                if (bddTestsFailed.getClass().equals(String.class)) {
                    buildStatus = bddTestsFailed == "true" ? "FAILURE" : currentBuild.result
                } else {
                    buildStatus = bddTestsFailed == true ? "FAILURE" : currentBuild.result
                }

                notification.sendSlackFinishBuild(
                    slackChannel,
                    bddType,
                    buildStatus, 
                    allureJobUrls,
                    jiraReporter, 
                    threadId,
                    tempbasesInfo,
                    issueKey
                )

                projectHelpers.beforeEndJob()
            }
        }
    }
}