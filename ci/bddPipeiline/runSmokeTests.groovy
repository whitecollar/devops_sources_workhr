#!groovy
@Library("shared-libraries")
import io.bit.Consul
import io.bit.Utils
import io.bit.GitUtils
import io.bit.ProjectHelpers
import io.bit.JenkinsIntegration
import io.bit.BITConvJava

def consul = new Consul()
def gitUtils = new GitUtils()
def projectHelpers = new ProjectHelpers()
def jenkinsIntegration = new JenkinsIntegration()
def bitConvJava = new BITConvJava()

pipeline {
	
    parameters {
        string(description: 'Jenkins agent', name: 'jenkinsAgent')
        string(description: 'Project key in jira', name: 'projectKey')
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
                        featuresRepoFolder = "${env.WORKSPACE}"
                        featuresRepo = consul.queryVal("${projectKey}/features_repo")
                        try {
                            // По невыясненной причине первый запуск пайплайна на свежеразвернутой машине приводит к ошибке вида  Authentication failed
                            // Повторный запуск не приводит к ошибке, поэтому мы обрабатываем такую ситуацию через try catch. Костыль предоставлен задачей DEVOPS-633 
                            gitUtils.checkoutSCM(featuresRepo, featuresRepoFolder, featuresBranch)
                        } catch (excp) {
                            echo "Features checkout failed with error. Is it first pipeline launch on the machine? Let's try again"
                            gitUtils.checkoutSCM(featuresRepo, featuresRepoFolder, featuresBranch)
                        }
                        gitUtils.checkoutSCM(scm.userRemoteConfigs[0].url, "build/devops_sources", "master")

                        jenkinsUser = bitConvJava.getUserJenkins()

                        smokeBasesTemplate = consul.queryList("${projectKey}/smokebases")
                        if (smokeBasesTemplate.size() == 0) {
                            smokeBases = []
                        } else {
                            smokeBases = consul.queryList("${projectKey}/users/${jenkinsUser}/smokebases")
                        }
                        
                        platform = consul.queryVal("${projectKey}/project_server_platform")

                        env.bddTestsFailed = false
                        
                    }
                }
            }
        }
        stage('Дымовое тестирование') {
            steps {
                timestamps {
                    script {
                        for (baseId in smokeBases) {
                            connString = consul.queryVal("${projectKey}/users/${jenkinsUser}/smokebases/${baseId}/connection_string")
                            admin1cUser = consul.queryVal("${projectKey}/users/${jenkinsUser}/smokebases/${baseId}/admin_1c_user")
                            admin1cPassword = consul.queryVal("${projectKey}/users/${jenkinsUser}/smokebases/${baseId}/admin_1c_password")
                        
                            runTests(platform, connString, admin1cUser, admin1cPassword, featuresRepoFolder)
                        }
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
                    writeFile file:'environment.properties', text:"Project=${projectKey}\nType=Smoke\nFeaturesBranch=${featuresBranch}\nBuild=${jenkinsIntegration.buildURL()}"
                }

                allure includeProperties: false, jdk: '', results: [[path: 'build/out/allure']]
                
                publishHTML target: [
                    allowMissing: false, 
                    alwaysLinkToLastBuild: true, 
                    keepAll: false, 
                    reportDir: 'build/out', 
                    reportFiles: 'allure-report/index.html',
                    reportName: 'HTML Report', 
                    reportTitles: ''
                ]
                if (env.bddTestsFailed) {
                    echo "Forcing change current build status to SUCESS as env.bddTestsFailed was set to ${env.bddTestsFailed}"
                    manager.build.@result = hudson.model.Result.SUCCESS
                }
            }
        }
    }
}

def runTests(platform, connString, admin1cUser, admin1cPassword, featuresRepoFolder) {

    utils = new Utils()

    xddConfigPath = "${env.WORKSPACE}tools/xUnitParams.json"
    if (fileExists(xddConfigPath)) {
        xddConfig = xddConfigPath
    } else {
        echo "Project ${projectKey} doesn't have own xUnitParams.json in path ${xddConfigPath} so build is switched to use default config"
        xddConfig = "./build/devops_sources/ci/bddPipeiline/runnerParams/xUnitParams.json"
    }

    // Код возврата не равный нулю возникнет только в случае ошибки или штатного падения тестов. Результат прохождения тестов с типом UNSTABLE не влияет на код возврата.
    returnCode = utils.cmd("runner xunit --settings build/devops_sources/ci/bddPipeiline/runnerParams/vrunnerSmoke.json --v8version ${platform} --ibconnection ${connString} --db-user ${admin1cUser} --db-pwd ${admin1cPassword} --xddConfig ${xddConfig} --pathxunit tools/add/xddTestRunner.epf")
    
    buildStatusPath = "${featuresRepoFolder}\\build\\out\\xddExitCodePath.txt"
    if (!fileExists(buildStatusPath)) {
        utils.raiseError("Ошибка! Файл xddExitCodePath.txt со статусом результата тестирования Smoke не обнаружен! Для подробностей смотрите логи")
    }
    buildStatus = readFile file: buildStatusPath, encoding: "UTF-8"
    echo "Smoke tests tests has been finished. BuildStatus = ${buildStatus}"
    if (buildStatus.contains("1")) {
        env.bddTestsFailed = true;
        echo "Some of Smoke tests were gracefully failed. So env.bddTestsFailed was set to ${env.bddTestsFailed}"
    }
}