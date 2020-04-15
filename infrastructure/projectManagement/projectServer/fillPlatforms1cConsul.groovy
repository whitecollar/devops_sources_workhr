@Library("shared-libraries")
import io.bit.ProjectHelpers
import io.bit.Consul
import io.bit.BITConvJava

def projectHelpers = new ProjectHelpers()
def consul = new Consul()
def bitConvJava = new BITConvJava()

pipeline {

    parameters {
        string(description: 'Full platform 1c version to put in consul. For example 8.3.12.1685', name: 'platform1cNew')
        string(description: 'Full storage 1c server TCP path including port relating to platform 1C version', name: 'storage1cServerPort')
        string(defaultValue: "", description: 'Optional. System parameter with issue key from SD', name: 'issueKey')
        string(defaultValue: "", description: 'Optional. Issue author from SD', name: 'jiraReporter')
    }

    agent {
        label "service_NewProject"
    }

    options {
        timeout(time: 3600, unit: 'SECONDS') 
        buildDiscarder(logRotator(numToKeepStr: '10'))
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
        stage("Заполнение параметров в консуле") {
            steps {
                timestamps {
                    script {
                        platform1cUrl = bitConvJava.combinePlatform1cConsulPath(platform1cNew)
                        storage1cServerTCP = bitConvJava.combineStorage1cTCP(storage1cServerPort)

                        consul.putVal("", "${platform1cUrl}/")
                        consul.putVal(platform1cNew, "${platform1cUrl}/version")
                        consul.putVal(storage1cServerTCP, "${platform1cUrl}/storage_1c_server_tcp")
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