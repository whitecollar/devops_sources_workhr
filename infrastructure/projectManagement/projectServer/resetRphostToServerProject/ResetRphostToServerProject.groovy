@Library("shared-libraries")
import io.bit.Consul
import io.bit.Utils
import io.bit.JenkinsIntegration
import io.bit.ProjectHelpers
import io.bit.JIRAIntegration

def consul = new Consul()
def utils = new Utils()
def projectHelpers = new ProjectHelpers()
def jiraIntegration = new JIRAIntegration()
def jobtasks = [:]

pipeline {

    options {
        timeout(time: 7200, unit: 'SECONDS')
        buildDiscarder(logRotator(numToKeepStr: "10"))
    }

    agent {
        label "${env.jenkinsAgent}"
    }

    parameters {
        string(defaultValue: "service_NewProject", description: 'Jenkins agent for pipeline', name: 'jenkinsAgent')
        string(defaultValue: "", description: 'Optional. Projects to be ignored by script. Each project in list must be separated by comma ', name: 'exclusionProjects')
        string(defaultValue: "", description: 'Optional. System parameter with issue key from SD', name: 'issueKey')
        string(defaultValue: "", description: 'Optional. Issue author from SD', name: 'jiraReporter')
        string(defaultValue: "", description: 'Optional. Obsolete parameter you should not use', name: 'isTestJenkinsJob')
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

        stage("Подготовка параметров и запуск reset_rphost") {
            steps {
                timestamps {
                    script {
                        exclusionProjects = exclusionProjects?.replaceAll(' ','')
                        projectKeys = consul.queryList("")
                        for(def projectKey : projectKeys) {
                            projectServer = consul.queryVal("${projectKey}/project_server", true)
                            if (!exclusionProjects?.split(',')?.contains(projectKey) && projectServer && utils.pingServer(projectServer) && utils.pingJenkinsAgent("${projectServer}service")){
                                jobtasks["reset_rphost ${projectServer}"] = runResetRphostToServerProject("${projectServer}service", projectServer)
                            }
                        }
                        parallel jobtasks
                    }
                }
            }
        }
    }

    post {
        always {
            script {
                projectHelpers.createJiraBugIfFailed(null, null, jiraIntegration.getEpicMaintainingServers(), 
                    "Упал перезапуск rphost-ов от ${utils.currentDateWithFormat("dd.MM.yyyy")}")
                    
                projectHelpers.beforeEndJob()
            }
        }
    }
}
def runResetRphostToServerProject(jenkinsAgent, projectServer) {
    return {
        node (jenkinsAgent) {
            stage("Перезапуск rphost ${projectServer}") {
                timestamps {
                    checkout scm
                    Utils utils = new Utils()
                    JenkinsIntegration jenkinsIntegration = new JenkinsIntegration()
                    returnCode = utils.cmd("powershell -file ${env.WORKSPACE}/maintenance/restart1c.ps1")
                    if (returnCode != 0) {
                         utils.raiseError("Powershell raised an error when executing script. See logs above for detail information")
                    }
                    jenkinsIntegration.deleteWorkspaceFile(projectServer,"\\workspace\\infrastructure\\blockJobs\\", "git\\index.lock")
                }
            }
        }
    }
}