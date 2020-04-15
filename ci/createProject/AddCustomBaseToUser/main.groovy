@Library("shared-libraries")
import io.bit.Consul
import io.bit.ProjectHelpers
import io.bit.JenkinsJobs
import io.bit.BITConvJava
import io.bit.JenkinsSteps
import io.bit.Notification
import io.bit.JIRAIntegration

def consul = new Consul()
def projectHelpers = new ProjectHelpers()
def bitConvJava = new BITConvJava()
def jenkinsJobs = new JenkinsJobs()
def jenkinsSteps = new JenkinsSteps()
def notification = new Notification()
def jIRAIntegration = new JIRAIntegration()

pipeline {

    parameters {
        string(description: 'Poject key in jira', name: 'projectKey')
        string(description: 'Custom base name', name: 'baseId')
        string(description: 'Issue author from SD', name: 'jiraReporter')
        string(defaultValue: "", description: 'Optional. Full filepath with *bak, *cf or *dt extension to create infobase from', name: 'cfdtpath')
        string(defaultValue: "", description: 'Optional. Infobase administrator name. Can be empty', name: 'login')
        string(defaultValue: "", description: 'Optional. Infobase administrator password. Can be empty', name: 'password')
        string(defaultValue: "", description: 'Optional. If this flag is true then infobase wil be updated automatically from share', name: 'autoload')
        string(defaultValue: "", description: 'Optional. Folder where backups for automatic loading are stored', name: 'autoloadFolder')
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

                        projectKey = projectKey.toLowerCase()
                        jiraReporter = jiraReporter.toLowerCase()
                        
                        jenkinsJobs.fillConsulCustomBase(projectKey, baseId, autoload, autoloadFolder, jiraReporter, issueKey)

                        platform = consul.queryVal("${projectKey}/project_server_platform")
                        projectServer = consul.queryVal("${projectKey}/project_server")
                        custombaseServer = consul.queryVal("${projectKey}/custombases/${jiraReporter}/${baseId}/server")
                        customBaseConnString = consul.queryVal("${projectKey}/custombases/${jiraReporter}/${baseId}/connection_string")
                        customBase = consul.queryVal("${projectKey}/custombases/${jiraReporter}/${baseId}/base")
                        admin1cUserDefault = consul.queryVal("${projectKey}/custombases/${jiraReporter}/${baseId}/admin_1c_user")
                        admin1cPwdDefault = consul.queryVal("${projectKey}/custombases/${jiraReporter}/${baseId}/admin_1c_password")
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
        stage("Выполнение") {
            steps {
                timestamps {
                    script {
                        jenkinsSteps.addTolistTask(projectServer, projectKey, customBase, jiraReporter).call()
                        jenkinsSteps.createInfobaseTask(projectServer, platform, customBase, customBaseConnString, admin1cUserDefault, admin1cPwdDefault, cfdtpath).call()
                        jenkinsSteps.createUser1cTask(projectServer, platform, customBase, customBaseConnString, cfdtpath, login, password, admin1cUserDefault, admin1cPwdDefault).call()
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
