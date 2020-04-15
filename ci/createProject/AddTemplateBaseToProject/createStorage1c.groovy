@Library("shared-libraries")
import io.bit.Consul
import io.bit.ProjectHelpers

def consul = new Consul()
def projectHelpers = new ProjectHelpers()

pipeline {

    parameters {
        string(description: 'Project code in jira', name: 'projectKey')
        string(description: 'base id', name: 'baseId')
        string(defaultValue: "", description: 'Optional. System parameter with issue key from SD', name: 'issueKey')
        string(defaultValue: "", description: 'Optional. Issue author from SD', name: 'jiraReporter')
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
        stage("Подготовка параметров") {
            steps {
                timestamps {
                    script {
                        projectKey = projectKey.toLowerCase()
                        jiraReporter = jiraReporter.toLowerCase()

                        projectServerPlatform = consul.queryVal("${projectKey}/project_server_platform")
                        projectServer = consul.queryVal("${projectKey}/project_server")
                        storage1User = consul.queryVal("${projectKey}/storage_1c_pipeline_user")
                        storage1cPassword  = consul.queryVal("${projectKey}/storage_1c_pipeline_password")
                        templateBase = consul.queryVal("${projectKey}/templatebases/${baseId}/base")
                        storage1сTCP = consul.queryVal("${projectKey}/templatebases/${baseId}/storage1C_tcp")
                        admin1cUser = consul.queryVal("${projectKey}/templatebases/${baseId}/admin_1c_user")
                        admin1cPassword = consul.queryVal("${projectKey}/templatebases/${baseId}/admin_1c_password")
                        users = consul.queryList("${projectKey}/users")
                    }
                }
            }
        }
        stage("Создание основного хранилища") {
            steps {
                timestamps {
                    script {
                        projectHelpers.createStorage1c(
                            projectServer, 
                            projectServerPlatform, 
                            templateBase, 
                            storage1сTCP, 
                            storage1User, 
                            storage1cPassword, 
                            admin1cUser, 
                            admin1cPassword
                        )

                        // user auto 111
                        projectHelpers.createRepoUser(
                            storage1сTCP, 
                            storage1User, 
                            storage1cPassword, 
                            admin1cUser, 
                            admin1cPassword
                        )

                        for (def user : users) { 
                            projectHelpers.createRepoUser(
                                storage1сTCP, 
                                storage1User, 
                                storage1cPassword, 
                                user, 
                                admin1cPassword
                            )
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