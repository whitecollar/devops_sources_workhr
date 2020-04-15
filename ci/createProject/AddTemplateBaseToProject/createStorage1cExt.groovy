@Library("shared-libraries")
import io.bit.Consul
import io.bit.ProjectHelpers
import io.bit.BITConvJava

def consul = new Consul()
def projectHelpers = new ProjectHelpers()
def bitConvJava = new BITConvJava()

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
        stage("Заполнение параметров в Consul") {
            steps {
                timestamps {
                    script {

                        projectKey = projectKey.toLowerCase()
                        jiraReporter = jiraReporter.toLowerCase()
                        
                        extensionPath = bitConvJava.getExtTemplatePath()
                        mainExtension = bitConvJava.getMainExtension()

                        projectServerPlatform = consul.queryVal("${projectKey}/project_server_platform")
                        projectServer = consul.queryVal("${projectKey}/project_server")
                        storage1User = consul.queryVal("${projectKey}/storage_1c_pipeline_user")
                        storage1cPassword  = consul.queryVal("${projectKey}/storage_1c_pipeline_password")
                        templateBase = consul.queryVal("${projectKey}/templatebases/${baseId}/base")
                        admin1cUser = consul.queryVal("${projectKey}/templatebases/${baseId}/admin_1c_user")
                        admin1cPassword = consul.queryVal("${projectKey}/templatebases/${baseId}/admin_1c_password")
                        mainExtensionStorageTcp = consul.queryVal("${projectKey}/templatebases/${baseId}/main_extension_storage1C_tcp")
                        users = consul.queryList("${projectKey}/users")
                    }
                }
            }
        }

        stage("Создание  хранилища для расширения") {
            steps {
                timestamps {
                    script {
                        try {
                            projectHelpers.loadExtensionFromFile(
                                projectServer,
                                projectServerPlatform,
                                templateBase,
                                admin1cUser, 
                                admin1cPassword,
                                extensionPath,
                                mainExtension
                            )

                            projectHelpers.createStorage1cExt(
                                projectServer, 
                                projectServerPlatform, 
                                templateBase, 
                                admin1cUser,
                                admin1cPassword,
                                mainExtensionStorageTcp,
                                storage1User,
                                storage1cPassword,
                                mainExtension
                            )

                            for (def user : users) {
                                projectHelpers.createStorage1cExtUser(
                                    projectServer, 
                                    projectServerPlatform, 
                                    templateBase, 
                                    admin1cUser, 
                                    admin1cPassword,
                                    mainExtensionStorageTcp,
                                    storage1User,
                                    storage1cPassword,
                                    mainExtension,
                                    user,
                                    storage1cPassword
                                ) 
                            } 
                        } catch (excp) {
                            echo "Error occured when creating extension storage. Probably ${templateBase} does not support extensions"
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