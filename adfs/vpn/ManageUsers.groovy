@Library("shared-libraries")
import io.bit.Utils 
import io.bit.JIRAIntegration
def utils = new Utils()
def JIRAIntegration = new JIRAIntegration()
def firstInitFail = false
def returnUri = "null"

// Создает и прикрепляет артефакт к сборке в виде текстового файла. Каждый вызов метода перезатирает артефакт.
// В файле содержится URI на VPN профиль пользователя
// Параметры:
//  uri - текст для помещения в артефакт
//
def setVpnUri(text){
    def fileName = 'urilink.txt'
    writeFile(file: fileName, text: text, encoding: "UTF-8")
    step([$class: 'ArtifactArchiver', artifacts: fileName, fingerprint: true])
}

pipeline {

    parameters {
       
        string(description: '', name: 'user')
        string(description: '', name: 'action')
        
        string(description: '', name: 'jiraReporter')
        string(description: '', name: 'issueKey')
        }

    agent{
        label "service_VpnUsers"
    }
    options {
        buildDiscarder(logRotator(numToKeepStr:'10'))
    }
    stages {
        stage("Основной блок") {
           
            steps {
                script {
                    mail = JIRAIntegration.getEmailByLogin (user) 
                    if (action == 'add') {
                      actionString = "python ./adfs/vpn/ManageUsers.py add ${user} ${mail}"  
                    }
                    if (action == 'resetLink') {
                     actionString = "python ./adfs/vpn/ManageUsers.py resetlink ${user} ${mail}"
                    }
                    
                    if (action == 'block') {
                        actionString ="python ./adfs/vpn/ManageUsers.py block ${user} ${mail}"
                    }
                    utils.cmd("ls -la")
                    echo "python ./${actionString}"
                    returnUri =  utils.cmdOut("${actionString}")
                    echo "${returnUri}"
                    setVpnUri(returnUri)
                    utils.cmdOut("ls -la")
                    
                }
            }
        }
     

    }
    post {
        always {
            script{
                if (firstInitFail){
                    currentBuild.result = 'FAILURE'
                }
            }
        }
    }
}