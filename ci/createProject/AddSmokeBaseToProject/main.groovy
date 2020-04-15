@Library("shared-libraries")
import io.bit.Consul
import io.bit.ProjectHelpers
import io.bit.JenkinsJobs
import io.bit.BITConvJava
import io.bit.JenkinsSteps

def consul = new Consul()
def projectHelpers = new ProjectHelpers()
def bitConvJava = new BITConvJava()
def jenkinsJobs = new JenkinsJobs()
def jenkinsSteps = new JenkinsSteps()

pipeline {

    parameters {
        string(description: 'Poject key in jira', name: 'projectKey')
        string(defaultValue: "", description: 'Use when you want to attach existing templateBase to smoke testing', name: 'templateBase')
        string(defaultValue: "", description: 'Use when you want to create a new base', name: 'baseId')
        string(defaultValue: "", description: 'Optional. Full filepath with *bak, *cf or *dt extension to create infobase from', name: 'cfdtpath')
        string(defaultValue: "", description: 'Optional. Infobase administrator name. Can be empty', name: 'login')
        string(defaultValue: "", description: 'Optional. Infobase administrator password. Can be empty', name: 'password')
        string(defaultValue: "", description: 'Optional. Issue author from SD', name: 'jiraReporter')
        string(defaultValue: "", description: 'Optional. System parameter with issue key from SD', name: 'issueKey')
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
                        
                        assert projectKey

                        projectKey = projectKey.toLowerCase()
                        templateBase = templateBase.toLowerCase()
                        baseId = baseId.toLowerCase()
                        jiraReporter = jiraReporter.toLowerCase()
                        userJenkins = bitConvJava.getUserJenkins()

                        templateBaseBaseId = templateBase.isEmpty() ? baseId : templateBase
                        
                        jenkinsJobs.fillConsulSmokeBase(projectKey, templateBaseBaseId, jiraReporter, issueKey)

                        platform = consul.queryVal("${projectKey}/project_server_platform")
                        projectServer = consul.queryVal("${projectKey}/project_server")
                        connString = consul.queryVal("${projectKey}/smokebases/${templateBaseBaseId}/connection_string")
                        infobase = consul.queryVal("${projectKey}/smokebases/${templateBaseBaseId}/base")
                        admin1cUser = consul.queryVal("${projectKey}/smokebases/${templateBaseBaseId}/admin_1c_user")
                        admin1cPwd = consul.queryVal("${projectKey}/smokebases/${templateBaseBaseId}/admin_1c_password")

                        useTemplateBase = !templateBase.isEmpty()
                    }
                }
            }
        }
        stage("Выполнение") {
            steps {
                timestamps {
                    script {
                        if (useTemplateBase != true) {
                            jenkinsSteps.addTolistTask(projectServer, projectKey, infobase, userJenkins).call()
                            jenkinsSteps.createInfobaseTask(projectServer, platform, infobase, connString, admin1cUser, admin1cPwd, cfdtpath).call()
                            jenkinsSteps.createUser1cTask(projectServer, platform, infobase, connString, cfdtpath, login, password, admin1cUser, admin1cPwd).call()
                        }
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
