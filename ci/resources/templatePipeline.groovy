// Шаблон пайплайна. 
// Наши требования к разработке пайплайна https://stackoverflow.com/c/bit-erp/questions/298/299#299
// Общий кодстайл https://google.github.io/styleguide/javaguide.html
@Library("shared-libraries")
import io.bit.ProjectHelpers

def projectHelpers = new ProjectHelpers()

pipeline {

    parameters {
        string(defaultValue: "", description: 'Optional. Issue author from SD', name: 'jiraReporter')
        string(defaultValue: "", description: 'Optional. System parameter with issue key from SD', name: 'issueKey')
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
        stage("Выполнение") {
            steps {
                timestamps {
                    script {
                       echo "empty build"
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