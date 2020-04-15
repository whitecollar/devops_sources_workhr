@Library("shared-libraries")
import io.bit.RabbitMQJava
import io.bit.BITConvJava
import io.bit.Consul
import io.bit.ProjectHelpers

def bitConvJava = new BITConvJava()
def consul = new Consul()
def projectHelpers = new ProjectHelpers()

pipeline {

    agent {
        label "${env.jenkinsAgent}"
    }

    parameters {
        string(description: 'Project key in jira', name: 'projectKey')
        string(description: 'User', name: 'user')
        string(defaultValue: "service_NewProject", description: 'Jenkins agent', name: 'jenkinsAgent')
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
        stage("Подготовка параметров") {
            steps {
                timestamps {
                    script {
                        server = consul.queryVal("${projectKey}/servers/rabbitmq/dev${projectKey}rmq/server")
                        port = consul.queryVal("${projectKey}/servers/rabbitmq/dev${projectKey}rmq/port")
                        login = consul.queryVal("${projectKey}/servers/rabbitmq/dev${projectKey}rmq/login")
                        password = consul.queryVal("${projectKey}/servers/rabbitmq/dev${projectKey}rmq/password")
                        name = bitConvJava.combineRabbitMQUser(user, projectKey)
                        portAPI = bitConvJava.combineRabbitMQPortAPI(port)
                    }
                }
            }
        }
        stage("Создание и добавление пользователя в RabbitMQ") {
            steps {
                timestamps {
                    script {
                        rabbitMQJava = new RabbitMQJava(server, portAPI, login, password)
                        rabbitMQJava.createVHost(name)
                        rabbitMQJava.createUser(name, name)
                        rabbitMQJava.addUserToVHost(name, name)
                    }
                }
            }
        }
    }
}
