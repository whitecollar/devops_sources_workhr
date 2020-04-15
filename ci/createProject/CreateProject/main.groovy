@Library("shared-libraries")
import io.bit.JenkinsJobs
import io.bit.BITConvJava
import io.bit.ProjectHelpers

def jenkinsJobs = new JenkinsJobs()
def bitConvJava = new BITConvJava()
def projectHelpers = new ProjectHelpers()

newProjectKey
projectParticipants

pipeline {
    agent {
        label "service_NewProject"
    }

    parameters {
        string(description: 'Project key in jira', name: 'newProjectKey')
        string(defaultValue: "", description: 'Optional. Platform 1С version. For example: 8.3.12.1529', name: 'platform1c')
        string(defaultValue: "false", description: 'Optional. Flag that server is needed. Allowed values are: true, false', name: 'needServer')
        string(defaultValue: "", description: 'Optional. Projects members via comma. For example username1,username2', name: 'projectParticipants')
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

                        newProjectKey = newProjectKey?.toLowerCase()?.replaceAll("\\s","")
                        projectParticipants = projectParticipants?.toLowerCase()?.replaceAll("\\s","")
                        userJenkins = bitConvJava.getUserJenkins()
                        if (projectParticipants && !projectParticipants.contains(userJenkins)) {
                            projectParticipants += ",${userJenkins}"
                        }
                    }
                }
            }
        }
        stage( 'Установка параметров проекта в consul') {
            steps {
                timestamps {
                    script {
                        jenkinsJobs.fillConsulProject(newProjectKey, platform1c, needServer, projectParticipants, issueKey, jiraReporter, isTestJenkinsJob)
                    }
                }
            }
        }
        stage( 'Заполнение репозитория фич') {
            steps {
                timestamps {
                    script {
                        jenkinsJobs.createFeaturesRepo(newProjectKey, issueKey, jiraReporter)
                    }
                }
            }
        }
        stage( 'Создание сервера RabbitMQ') {
            steps {
                timestamps {
                    script {
                        echo "Создание сервера"
                        jenkinsJobs.createServer(newProjectKey, 'Linux', 'Сервер RabbitMQ', '2', '4', '', '', '', jiraReporter, isTestJenkinsJob)
                    }
                }
            }
        }
        stage( 'Создание пользователей проекта') {
            steps {
                timestamps {
                    script {
                            for (def projectUser : projectParticipants?.split(",")) {
                                if (!projectUser.isEmpty()){
                                    jenkinsJobs.addUserToProject(newProjectKey, projectUser, issueKey, jiraReporter, isTestJenkinsJob)
                                }
                            }
                    }
                }
            }
        }
        stage( 'Создание папки проекта') {
            steps {
                timestamps {
                    script {
                        jenkinsJobs.createFolderProject(newProjectKey, issueKey, jiraReporter)
                    }
                }
            }
        }
    }
}

