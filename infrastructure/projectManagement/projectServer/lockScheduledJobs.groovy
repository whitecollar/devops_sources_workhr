#!groovy
@Library("shared-libraries")
import io.bit.Consul
import io.bit.Utils
import io.bit.ProjectHelpers
import io.bit.JIRAIntegration
import io.bit.BITConvJava
import io.bit.GitUtils

def consul = new Consul()
def utils = new Utils()
def projectHelpers = new ProjectHelpers()
def jiraIntegration = new JIRAIntegration()
def bitConvJava = new BITConvJava()
def locktasks = [:]
def gitUtils = new GitUtils()

pipeline {
    options {
        timeout(time: 180, unit: 'SECONDS') 
        buildDiscarder(logRotator(numToKeepStr: '10'))
        skipDefaultCheckout true
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
        stage("Подготовка параметров") {
            steps {
                timestamps {
                    script {
                        deleteDir()
                        dir ('build') {
                            writeFile file:'dummy', text:''
                        }

                        admin1cFolder = "${env.WORKSPACE}/build/admin1c"
                        admin1cGit = "https://github.com/ripreal/admin1c.git"
                        admin1cExecPath = "${env.WORKSPACE}/build/admin1c/releases/admin1c.jar"
                        admin1cUsr = bitConvJava.getAdmin1cUser()
                        admin1cPwd = bitConvJava.getAdmin1cPwd()

                        gitUtils.clone(admin1cGit, "${env.WORKSPACE}/build", true);
                        gitUtils.reset(admin1cFolder)
                        gitUtils.checkoutOneFile("releases/admin1c.jar", admin1cFolder)

                        projects = consul.queryList("")
                    }
                }
            }
        }
        stage("Запуск") {
            steps {
                timestamps {
                    script {
                        for (def projectKey : projects) {
                            projectServer = consul.queryVal("${projectKey}/project_server", true)
                            projectAlive = consul.queryVal("${projectKey}/project_alive", true)
                            if (projectAlive != "true") {
                                // Проект временно или постоянно заморожен
                                continue
                            }
                            if (projectServer == null) {
                                // на этом проекте нет своего проектного сервера
                                continue
                            }
                            if (!utils.pingServer(projectServer, true)) {
                                continue
                            }

                            locktasks["lockBackgroundJob ${projectServer}"] = lockBackgroundJobsTask("lock", projectServer, admin1cUsr, admin1cPwd, admin1cExecPath)
                            locktasks["unlockBackgroundJob ${projectServer}"] = lockBackgroundJobsTask("unlock",  projectServer, admin1cUsr, admin1cPwd, admin1cExecPath)
                        }
                        parallel locktasks
                    }
                }
            }
        }
    }

    post {
        always {
            script {
                if (getGenuineBuildStatus() != "ABORTED") {
                    issues = jiraIntegration.getIssuesByEpic(jiraIntegration.getEpicLockSheduledJobs())
                    totalIssues = Integer.parseInt(issues.get("total").toString())
                    if (totalIssues == 0) {
                        echo "No issues with epic called \"block scheduled jobs service\" found. So let's create a new bug"
                        projectHelpers.createJiraBugIfFailed(null, null, jiraIntegration.getEpicLockSheduledJobs(), 
                            "Упал пайплайн блокировки РЗ от ${utils.currentDateWithFormat("dd.MM.yyyy")}")
                    } else {
                        echo "A issue with epic ${jiraIntegration.getEpicLockSheduledJobs()} was found. So we skip creating jira issue step.s"
                    }
                }
            }
        }
    }
}

def lockBackgroundJobsTask(lockType, server1c, admin1cUsr, admin1cPwd, admin1cExecPath) {
    return {
        stage("Запуск обработки РЗ ${lockType} на ${server1c}") {
            timestamps {
                def utils = new Utils()

                retunStatus = utils.cmd("java -jar ${admin1cExecPath} -mode ${lockType} -server1c ${server1c} -portras 1545 -admin1c ${admin1cUsr} -pwd1c ${admin1cPwd} -timer 120000")
                if (retunStatus != 0) {
                    utils.raiseError("Возникла ошибка при выполнении скрипта блокировки РЗ на ${server1c} с типом ${lockType}. Смотрите логи для подробной информации")
                }
            }
        }
    }
}

// We're using this hack now to get better detection of aborted builds because default property currentBuild.currentResult cannot recognize ABORTED status correctly:
def getGenuineBuildStatus() {
    if (currentBuild.rawBuild.getActions(jenkins.model.InterruptedBuildAction.class).isEmpty()) {
        echo "Our hack discovered ${currentBuild.currentResult} status. Proccessing it"
        return currentBuild.currentResult
    } else {
        echo "Our hack discovered ABORTED status. Proccessing it"
        return "ABORTED"
    }
}