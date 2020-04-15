#!groovy
@Library("shared-libraries")
import io.bit.JenkinsJobs
import io.bit.Consul
import io.bit.ProjectHelpers

def consul = new Consul()
def jenkinsJobs = new JenkinsJobs()
def projectHelpers = new ProjectHelpers()
def templateTasks = [:]
def usersTasks = [:]

pipeline {
    agent {
        label "service_NewProject"
    }

    parameters {
        string(description: 'Project key in jira', name: 'projectKey')
        string(defaultValue: "", description: 'Optional. Platform 1С version. For example: 8.3.12.1529', name: 'platform1c')
        string(defaultValue: "", description: 'Optional. Projects members via comma. For example username1,username2', name: 'projectParticipants')
        string(defaultValue: "", description: 'Optional. Bases to add. Пример erp,upp', name: 'confTypes')
        string(defaultValue: "", description: 'Optional. System parameter with issue key from SD', name: 'issueKey')
        string(defaultValue: "", description: 'Optional. Issue author from SD', name: 'jiraReporter')
        string(defaultValue: "", description: 'Obsolete', name: 'isTestJenkinsJob')
    }

    options {
        timeout(time: 3600, unit: 'SECONDS') 
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
        stage( 'Подготовка параметров') {
            steps {
                timestamps {
                    script {
                        projectKey = projectKey.toLowerCase().replaceAll("\\s", "")
                        projectParticipants = projectParticipants.toLowerCase().replaceAll("\\s", "")
                        confTypes = confTypes.toLowerCase().replaceAll("\\s", "")
                    }
                }
            }
        }
        stage( 'Сохранение старых параметров') {
            steps {
                timestamps {
                    script {
                        bddBase = consul.queryVal("${projectKey}/bddBase", true)
                        if (bddBase == null) {
                            bddBase = ""
                        }
                        templatebases = consul.queryList("${projectKey}/templatebases", true) 
                        if (templatebases == null) {
                            templatebases = []
                        }
                        users = consul.queryList("${projectKey}/users", true)
                        if (users == null) {
                            users = []
                        }
                        needServer = consul.queryVal("${projectKey}/need_server", true)
                        if (needServer == null) {
                            needServer = "false"
                        }
                    }
                }
            }
        }
        stage( 'Перезаполнение параметров проекта') {
            steps {
                timestamps {
                    script {
                        jenkinsJobs.fillConsulProject(projectKey, platform1c, needServer, projectParticipants, issueKey, jiraReporter, isTestJenkinsJob)
                    }
                }
            }
        }
        stage( 'Перезаполнение параметров эталонных баз') {
            steps {
                timestamps {
                    script {
                        for (def baseId : templatebases) {
                            postfix = consul.queryVal("${projectKey}/templatebases/${baseId}/postfix", true)
                            if (postfix == null) {
                                postfix = ""
                            }
                            mergeType = consul.queryVal("${projectKey}/templatebases/${baseId}/mergetype", true) 
                            if (mergeType == null) {
                                mergeType = "LOAD"
                            }
                            templateTasks["refill_template_${baseId}"] = fillConsulTemplateBaseTask(
                                projectKey,
                                baseId, 
                                postfix, 
                                mergeType, 
                                bddBase == baseId ? "true" : "false", 
                                issueKey, 
                                jiraReporter, 
                                isTestJenkinsJob
                            ) 
                        }
                        parallel templateTasks
                    }
                }
            }
        }
        stage('Перезаполнение параметров пользователей') {
            steps {
                timestamps {
                    script {
                        for (def user : users) {
                            usersTasks["refill_user_${user}"] = fillConsulUserTask(projectKey, user, issueKey, jiraReporter, isTestJenkinsJob)
                        }
                        parallel usersTasks
                    }
                }
            }
        }
        stage('Добавление новых эталонных баз') {
            steps {
                timestamps {
                    script {
                        for (String confType : confTypes.split(",")) {
                            if (confType.isEmpty()) {
                                continue
                            }
                            jenkinsJobs.fillConsulTemplateBase(projectKey, confType, "", "LOAD", bddBase, issueKey, jiraReporter, isTestJenkinsJob)
                        }
                    }
                }
            }
        }
        stage('Добавление новых пользователей') {
            steps {
                timestamps {
                    script {
                        for (String projectUser : projectParticipants.split(",")) {
                            if (projectUser.isEmpty()) {
                                continue
                            }
                            jenkinsJobs.fillConsulUser(projectKey, projectUser, issueKey, jiraReporter, isTestJenkinsJob)
                        }
                    }
                }
            }
        }
    }
}

def fillConsulUserTask(projectKey, user, issueKey, jiraReporter, isTestJenkinsJob) {
    return {
        timestamps {
            jenkinsJobs = new JenkinsJobs()
            jenkinsJobs.fillConsulUser(projectKey, user, issueKey, jiraReporter, isTestJenkinsJob)
        }
    }
}

def fillConsulTemplateBaseTask(projectKey, baseId, postfix, mergeType, bddBase, issueKey, jiraReporter, isTestJenkinsJob) {
    return {
        timestamps {
            jenkinsJobs = new JenkinsJobs()
            jenkinsJobs.fillConsulTemplateBase(projectKey, baseId, postfix, mergeType, bddBase, issueKey, jiraReporter, isTestJenkinsJob)
        }
    }
}