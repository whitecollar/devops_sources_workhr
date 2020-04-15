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
        string(description: 'base id', name: 'baseId')
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

                        userJenkins = bitConvJava.getUserJenkins()
                        admin1cUser = bitConvJava.getAdmin1cUser()
                        admin1cPwd = bitConvJava.getAdmin1cPwd()
                        projectServer = consul.queryVal("${projectKey}/project_server")
                        // Если находим эталонную базу с этим айдишником, считаем, то нужно использовать эталонную базу.
                        // В противном случае заводим новую базу
                        basename = consul.queryVal("${projectKey}/templatebases/${baseId}/base", true)
                        if (basename == null) {
                            basename = bitConvJava.combineSmokeTemplatebase(baseId, projectKey)
                        }
                        
                        connString1c = bitConvJava.combineConnString1c(projectServer, basename)
                        connString = bitConvJava.combineConnString(projectServer, basename)
                        baseGroup = "${projectKey}/smokebases/${baseId}"
                        
                        consul.putVal(projectServer, "${baseGroup}/server")
                        consul.putVal(basename, "${baseGroup}/base")
                        consul.putVal(connString1c, "${baseGroup}/conn_string1C")
                        consul.putVal(connString, "${baseGroup}/connection_string")
                        consul.putVal(baseId, "${baseGroup}/base_id")
                        consul.putVal(admin1cUser,"${baseGroup}/admin_1c_user")
                        consul.putVal(admin1cPwd, "${baseGroup}/admin_1c_password")

                        projectHelpers.createSmokebases(userJenkins, baseId, projectKey)
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