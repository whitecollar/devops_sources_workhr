@Library("shared-libraries")
import io.bit.ProjectHelpers
import io.bit.GitUtils
import io.bit.BITConvJava
import io.bit.FileUtils
import io.bit.Consul

def projectHelpers = new ProjectHelpers()
def gitUtils = new GitUtils()
def consul = new Consul()
def bitConvJava = new BITConvJava()
def fileUtils = new FileUtils()

pipeline {

    parameters {
        string(description: 'Project key in jira', name: 'projectKey')
        string(defaultValue: "", description: 'Optional. Issue author from SD', name: 'jiraReporter')
        string(defaultValue: "", description: 'Optional. System parameter with issue key from SD', name: 'issueKey')
    }

    agent {
        label "service_NewProject"
    }

    options {
        timeout(time: 3600, unit: 'SECONDS') 
        buildDiscarder(logRotator(daysToKeepStr:'30'))
        skipDefaultCheckout true
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
        stage('Кастомный Checkout SCM') {
            steps {
                deleteDir()
                checkout scm
            }
        }
        stage("Подготовка параметров") {
            steps {
                timestamps {
                    script {
                        buildFolder = "${env.WORKSPACE}/build"

                        fileUtils.deleteFile(buildFolder)

                        dir ('build') {
                            writeFile file:'dummy', text:''
                        }
                        
                        featuresRepo = consul.queryVal("${projectKey}/features_repo")

                        templateFeaturesRepo = bitConvJava.getTemplateFeaturesRepo()
                        projectFeatureRepoFolder = "${buildFolder}/featuresRepo"
                        repoFolder = "${buildFolder}/template_features"
                    }
                }
            }
        }
        stage("Выполнение") {
            steps {
                timestamps {
                    script {
                        gitUtils.clone(templateFeaturesRepo, buildFolder)
                        gitUtils.changeRemote(featuresRepo, repoFolder)
                        try {
                            gitUtils.push("master", repoFolder)
                        } catch (excp) {
                            echo "Failed pushing template features to project repo ${featuresRepo}. Probably project repo already initialized"
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