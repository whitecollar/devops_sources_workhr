@Library("shared-libraries")
import io.bit.Consul
import io.bit.ProjectHelpers
import io.bit.BITConvJava

def consul = new Consul()
def projectHelpers = new ProjectHelpers()
def bitConvJava = new BITConvJava()

pipeline {
    agent {
        label "service_NewProject"
    }

    parameters {
        string(description: 'Poject key in jira', name: 'projectKey')
        string(description: 'Custom base name', name: 'baseId')
        string(defaultValue: "", description: 'Optional. If this flag is true then infobase wil be updated automatically from share', name: 'autoload')
        string(defaultValue: "", description: 'Optional. Folder where backups for automatic loading are stored', name: 'autoloadFolder')
        string(defaultValue: "", description: 'Optional. Issue author from SD', name: 'jiraReporter')
        string(defaultValue: "", description: 'Optional. System parameter with issue key from SD', name: 'issueKey')
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
        stage("Заполнение настроек в Consul") {
            steps {
                timestamps {
                    script {
                        
                        projectKey = projectKey.toLowerCase()
                        jiraReporter = jiraReporter.toLowerCase()
                        admin1cUser = bitConvJava.getAdmin1cUser()
                        admin1cPwd = bitConvJava.getAdmin1cPwd()
                        baseName = bitConvJava.combineCustomBaseName(projectKey, baseId, jiraReporter)
                        customBaseGroup = "${projectKey}/custombases/${jiraReporter}/${baseId}"
                          
                        projectServer = consul.queryVal("${projectKey}/project_server")
                        
                        consul.putVal("${projectServer}", "${customBaseGroup}/server")
                        consul.putVal("${baseName}", "${customBaseGroup}/base")
                        consul.putVal("Srvr=\"${projectServer}\";Ref=\"${baseName}\";", "${customBaseGroup}/conn_string1C")
                        consul.putVal("/S${projectServer}\\${baseName}", "${customBaseGroup}/connection_string")
                        consul.putVal("${baseId}", "${customBaseGroup}/base_id")
                        consul.putVal("${admin1cUser}","${customBaseGroup}/admin_1c_user")
                        consul.putVal("${admin1cPwd}", "${customBaseGroup}/admin_1c_password")
                        consul.putVal("${autoloadFolder}", "${customBaseGroup}/autoload_folder")
                        consul.putVal("${autoload}", "${customBaseGroup}/autoload")
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