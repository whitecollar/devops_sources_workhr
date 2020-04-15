#!groovy
@Library("shared-libraries")
import io.bit.Consul
import io.bit.ProjectHelpers
import io.bit.Utils
import io.bit.RabbitMQJava
import io.bit.BITConvJava

def bitConvJava = new BITConvJava()
def consul = new Consul()
def projectHelpers = new ProjectHelpers()
def utils = new Utils()

pipeline {

    options {
        timeout(time: 28800, unit: 'SECONDS') 
        buildDiscarder(logRotator(daysToKeepStr:'10'))
    }

    parameters {
        string(description: 'Jenkins agent', name: 'jenkinsAgent')
        string(description: 'Project key in jira', name: 'projectKey')
        string(description: 'User to clean', name: 'user')
        string(defaultValue: "", description: 'Issue author in SD', name: 'jiraReporter')
        string(defaultValue: "", description: 'Optional. System parameter with issue key from SD', name: 'issueKey')
    }

    agent {
        label "${env.jenkinsAgent}"
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
        stage('Подготовка параметров') {
            steps {
                timestamps {
                    script {
	                    jiraReporter = jiraReporter.toLowerCase()
                        projectKey = projectKey.toLowerCase()
                        user = user.toLowerCase()
                        
                        platform = consul.queryVal("${projectKey}/project_server_platform")
                        projectServer = consul.queryVal("${projectKey}/project_server")
                        adminRabbitLogin = consul.queryVal("${projectKey}/servers/rabbitmq/dev${projectKey}rmq/login")
                        userPort = consul.queryVal("${projectKey}/servers/rabbitmq/dev${projectKey}rmq/port")
                        adminRabbitpassword = consul.queryVal("${projectKey}/servers/rabbitmq/dev${projectKey}rmq/password")
                        rabbitMQServer = consul.queryVal("${projectKey}/servers/rabbitmq/dev${projectKey}rmq/server")

                        portAPI = bitConvJava.combineRabbitMQPortAPI(userPort)
                    }
                }
            }
        }
        stage('Удаление баз на сервере 1С и sql') {
            steps {
                timestamps {
                    script {
                        userbases = consul.queryList("${projectKey}/users/${user}/testbases", true)
                        for (def baseId : userbases) {

                            userbase_server = consul.queryVal("${projectKey}/users/${user}/testbases/${baseId}/server")
                            userBase = consul.queryVal("${projectKey}/users/${user}/testbases/${baseId}/base")
                            admin1cUser = consul.queryVal("${projectKey}/users/${user}/testbases/${baseId}/admin_1c_user")
                            admin1cPwd = consul.queryVal("${projectKey}/users/${user}/testbases/${baseId}/admin_1c_password")

                            projectHelpers.dropDb(utils, platform, userbase_server, userBase, admin1cUser, admin1cPwd, true)
                        }
                    }
                }
            }
        }
        stage('Удаление пользователя RabbitMQ') {
            steps {
                timestamps {
                    script {
                        loginRabbbit = consul.queryVal("${projectKey}/users/${user}/rabbitmq/main/login")
                        passwordRabbbit = consul.queryVal("${projectKey}/users/${user}/rabbitmq/main/password")

                        rabbitMQJava = new RabbitMQJava(rabbitMQServer, portAPI, adminRabbitLogin, adminRabbitpassword)
                        rabbitMQJava.deleteUser(loginRabbbit, passwordRabbbit)
                        rabbitMQJava.deleteVHost(loginRabbbit)
                    }
                }
            }
        }
        stage('Удаление пользователя Consul') {
            steps {
                timestamps {
                    script {
                        consul.deleteVal("${projectKey}/users/${user}")
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

