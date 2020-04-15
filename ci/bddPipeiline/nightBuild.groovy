#!groovy
@Library("shared-libraries")
import io.bit.Consul
import io.bit.JenkinsJobs
import io.bit.ProjectHelpers
import io.bit.BITConvJava

def consul = new Consul()
def projectHelpers = new ProjectHelpers()
def bitConvJava = new BITConvJava()
def buildTasks = [:]

pipeline {

    options {
        timeout(time: 28800, unit: 'SECONDS') 
        buildDiscarder(logRotator(numToKeepStr:'10'))
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

                        list projects = consul.queryList("")
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
                            bddBase = consul.queryVal("${projectKey}/bddBase", true)
                            if (bddBase == null) {
                                // на этом проекте не определена база для запуска тестов
                                continue
                            }
                            buildTasks["build_${projectKey}"] = buildTask(projectKey, bitConvJava.getUserJenkins())
                        }
                        parallel buildTasks
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

def buildTask(projectKey, user) {
    return {
        stage("Ночная сборка ${projectKey} ${user}") {
            timestamps {
                def jenkinsJobs = new JenkinsJobs()
                def bitConvJava = new BITConvJava()
                jenkinsJobs.buildBddMain(projectKey, bitConvJava.getUserJenkins(), "", "UPDATE_FROM_STORAGE", "RUN_ALL_TESTS", '', '')
            }
        }
    }
}
