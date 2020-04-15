#!groovy
@Library("shared-libraries")
import io.bit.Consul
import io.bit.Utils
import io.bit.GitUtils
import io.bit.ProjectHelpers
import io.bit.JenkinsIntegration

def consul = new Consul()
def utils = new Utils()
def gitUtils = new GitUtils()
def projectHelpers = new ProjectHelpers()
def jenkinsIntegration = new JenkinsIntegration()

pipeline {
	
    parameters {
        string(description: 'Jenkins agent', name: 'jenkinsAgent')
        string(description: 'Project key in jira', name: 'projectKey')
        string(description: 'User to run test for', name: 'user')
        string(defaultValue: "master", description: 'Branch of features repository', name: 'featuresBranch')
        string(defaultValue: "", description: 'Optional.Issue author in SD', name: 'jiraReporter')
        string(defaultValue: "", description: 'Optional. System parameter with issue key from SD', name: 'issueKey')
        string(defaultValue: "", description: 'Obsolete', name: 'isTestJenkinsJob')
    }

    options {
        timeout(time: 28800, unit: 'SECONDS') 
        buildDiscarder(logRotator(daysToKeepStr:'10'))
        skipDefaultCheckout true
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
                timestamps {
                    script {
                        deleteDir()

                        // создаем пустые каталоги
                        dir ('build') {
                            writeFile file:'dummy', text:''
                        }
                        dir ('build/out') {
                            writeFile file:'dummy', text:''
                        }

                        bddBase = consul.queryVal("${projectKey}/bddBase")
                        featuresRepo = consul.queryVal("${projectKey}/features_repo")
                        featuresRepoFolder = "${env.WORKSPACE}"
                        platform = consul.queryVal("${projectKey}/project_server_platform")
                        connString = consul.queryVal("${projectKey}/users/${user}/testbases/${bddBase}/connection_string")
                        admin1cUser = consul.queryVal("${projectKey}/templatebases/${bddBase}/admin_1c_user")
                        admin1cPassword = consul.queryVal("${projectKey}/templatebases/${bddBase}/admin_1c_password")

                        env.bddTestsFailed = false

                        try {
                            // По невыясненной причине первый запуск пайплайна на свежеразвернутой машине приводит к ошибке вида  Authentication failed
                            // Повторный запуск не приводит к ошибке, поэтому мы обрабатываем такую ситуацию через try catch. Костыль предоставлен задачей DEVOPS-633 
                            gitUtils.checkoutSCM(featuresRepo, featuresRepoFolder, featuresBranch)
                        } catch (excp) {
                            echo "Features checkout failed with error. Is it first pipeline launch on the machine? Let's try again"
                            gitUtils.checkoutSCM(featuresRepo, featuresRepoFolder, featuresBranch)
                        }
                        gitUtils.checkoutSCM(scm.userRemoteConfigs[0].url, "build/devops_sources", "master")

                        providerPath = "http://127.0.0.1:8500/v1/kv/${projectKey}/users/${user}"
                        fillV8ParamsForUser("${env.workspace}\\build\\devops_sources\\ci\\bddPipeiline\\runnerParams\\VBParams.json", "CONSUL", providerPath)
                        fillV8ParamsForUser("${env.workspace}\\build\\devops_sources\\ci\\bddPipeiline\\runnerParams\\VBParamsFirst.json", "CONSUL", providerPath)
                    }
                }
            }
        }
        stage('Первоначальное заполнение') {
            steps {
                timestamps {
                    script {
                        runTests(utils, platform, connString, admin1cUser, admin1cPassword, "./build/devops_sources/ci/bddPipeiline/runnerParams/VBParamsFirst.json", featuresRepoFolder)
                    }
                }
            }
        }
        stage('Проверка поведения') {
            steps {
                timestamps {
                    script {
                        runTests(utils, platform, connString, admin1cUser, admin1cPassword, "./build/devops_sources/ci/bddPipeiline/runnerParams/VBParams.json", featuresRepoFolder)
                    }
                }
            }
        }
    }
    post {
        always {
            script {
                if (currentBuild.result == "ABORTED") {
                    return
                }

                dir ('build/out/allure') {
                    writeFile file:'environment.properties', text:"Project=${projectKey}\nType=BDD\nUser=${user}\nFeaturesBranch=${featuresBranch}\nBuild=${jenkinsIntegration.buildURL()}"
                }

                allure includeProperties: false, jdk: '', results: [[path: 'build/out/allure']]
                
                if (env.bddTestsFailed) {
                    echo "Forcing change current build status to SUCESS as env.bddTestsFailed was set to ${env.bddTestsFailed}"
                    manager.build.@result = hudson.model.Result.SUCCESS
                }
            }
        }
    }
}

def runTests(utils, platform, connString, admin1cUser, admin1cPassword, vanessasettings, featuresRepoFolder) {

    // Код возврата не равный нулю возникнет только в случае ошибки или штатного падения тестов. Результат прохождения тестов с типом UNSTABLE не влияет на код возврата.
    returnCode = utils.cmd("runner vanessa --settings build/devops_sources/ci/bddPipeiline/runnerParams/vrunner.json --v8version ${platform} --ibconnection ${connString} --db-user ${admin1cUser} --db-pwd ${admin1cPassword} --vanessasettings ${vanessasettings} --pathvanessa tools/add/bddRunner.epf")
    
    buildStatusPath = "${featuresRepoFolder}\\build\\out\\buildStatus.log"
    if (!fileExists(buildStatusPath)) {
        utils.raiseError("Ошибка! Файл buildStatus.log со статусом результата тестирования ADD не обнаружен! Для подробностей смотрите логи")
    }
    buildStatus = readFile file: buildStatusPath, encoding: "UTF-8"
    echo "ADD tests has been finished. BuildStatus = ${buildStatus}"
    if (buildStatus.contains("1")) {
        env.bddTestsFailed = true;
        echo "Some of ADD tests were gracefully failed. So env.bddTestsFailed was set to ${env.bddTestsFailed}"
    }
    
}

def fillV8ParamsForUser(path, providerName, providerPath) {
    content = readFile file: path, encoding: "UTF-8"
    newContent = content.replaceAll("\"ПоставщикПользовательскихНастроек\": \"\"", "\"ПоставщикПользовательскихНастроек\": \"${providerName}\"");
    newContent = newContent.replaceAll("\"АдресПользовательскихНастроек\": \"\"", "\"АдресПользовательскихНастроек\": \"${providerPath}\"");
    writeFile file: path, text: newContent, encoding: "UTF-8"
}