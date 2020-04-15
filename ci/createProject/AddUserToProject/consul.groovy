@Library("shared-libraries")
import io.bit.Consul
import io.bit.ProjectHelpers
import io.bit.BITConvJava

def consul = new Consul()
def projectHelpers = new ProjectHelpers()
def bITConvJava = new BITConvJava()

pipeline {
    agent {
        label "service_NewProject"
    }

    options {
        buildDiscarder(logRotator(daysToKeepStr:'30'))
    }

    parameters {
        string(description: 'Project key in jira', name: 'projectKey')
        string(description: 'User', name: 'user')
        string(defaultValue: "", description: 'Optional. System parameter with issue key from SD', name: 'issueKey')
        string(defaultValue: "", description: 'Optional. Issue author from SD', name: 'jiraReporter')
        string(defaultValue: "", description: 'Obsolete', name: 'isTestJenkinsJob')
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
                        String projectKey = projectKey.toLowerCase()
                        String user = user.toLowerCase()
                        consul.putVal('', "${projectKey}/users/${user}/")
                        consul.putVal('', "${projectKey}/users/${user}/testbases/")
                        consul.putVal('', "${projectKey}/users/${user}/devbases/")
                        consul.putVal('', "${projectKey}/users/${user}/rabbitmq/")
                        templBaseList = consul.queryList("${projectKey}/templatebases")                  
                        for (def curBase : templBaseList) {
                            projectHelpers.createbases(projectKey, user, curBase, consul)
                        }

                        String server = bITConvJava.combineRabbitMQServerName(projectKey)
                        String port = bITConvJava.getRabbitmqDefPort()
                        String name = bITConvJava.combineRabbitMQUser(user, projectKey)
                        consul.putVal(name, "${projectKey}/users/${user}/rabbitmq/main/login")
                        consul.putVal(name, "${projectKey}/users/${user}/rabbitmq/main/password")
                        consul.putVal(port, "${projectKey}/users/${user}/rabbitmq/main/port")
                        consul.putVal(server, "${projectKey}/users/${user}/rabbitmq/main/server")
                        consul.putVal(name, "${projectKey}/users/${user}/rabbitmq/main/vhost")

                        if (user == bITConvJava.getUserJenkins()) {
                            smokeBases = consul.queryList("${projectKey}/smokebases")
                            for (def baseId : smokeBases) {
                                projectHelpers.createSmokebases(userJenkins, baseId, projectKey)
                            }
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
