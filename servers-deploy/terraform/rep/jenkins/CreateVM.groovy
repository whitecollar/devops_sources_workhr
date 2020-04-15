@Library("shared-libraries")
import io.bit.Consul
import io.bit.Utils

def utils = new Utils()
def login = 'admin'
def password
def firstInitFail = false
def ouName = 'Computers'
projectKey

pipeline {

    parameters {
        string(defaultValue: "devops", description: '', name: 'projectKey')
        string(defaultValue: "nix", description: '', name: 'osName')
        string(description: '', name: 'roleName')
        string(description: '', name: 'cpu')
        string(description: '', name: 'ram')
        string(defaultValue: "", description: '', name: 'serverName')
        string(defaultValue: "", description: '', name: 'issueKey')
        string(description: '', name: 'jiraReporter')
    }

    agent{
        label "service_NewProject"
    }
    options {
        buildDiscarder(logRotator(numToKeepStr:'10'))
    }
    stages {
        stage("Основной блок") {
            agent {
                label "${env.jnode}"
            }
            steps {
                script {
                    ram = ram.toInteger() * 1024
                    osName = osName == 'Windows' ? 'win' : 'nix'
                    projectKey = projectKey.toLowerCase()
                    if (roleName == 'Сервер RabbitMQ') {
                        roleName = 'rabbitmq'
                        serverName = "dev${projectKey}rmq"
                        osName = 'nix'
                        password = utils.giveMeKey()
                    }
                    if (roleName == 'Сервер проектов') {
                        roleName = 'prj'
                        ouName='projects'
                        serverName = "dev${projectKey}"
                        osName = 'win'
                        password = utils.giveMeKey()
                    }
                    if (roleName == 'Сервер сотрудника') {
                        roleName = 'work'
                        ouName='dev_vm'
                        serverName = jiraReporter
                        osName = 'win'
                        projectKey = 'devops'
                    }
                    if (roleName == 'Сервер произвольной конфигурации') {
                        roleName = 'plain'
                        serverName = serverName
                        projectKey = 'devops'
                    }
                    if (roleName == 'Сервер MS SQL') {
                        roleName = 'sql'
                        serverName = "dev${projectKey}sql"
                        osName = 'win'
                        password = utils.giveMeKey()
                    }



                    utils.cmd("echo env.ProjectCode ${projectKey} >> ./test")
                    utils.cmd("echo env.os ${osName} >> ./test")
                    utils.cmd("echo env.role ${roleName} >> ./test")
                    utils.cmd("echo env.cpu ${cpu} >> ./test")
                    utils.cmd("echo env.ram ${ram} >> ./test")
                    utils.cmd("echo env.name ${serverName} >> ./test")
                    utils.cmd("set TF_LOG=DEBUG")
                    utils.cmd("export TF_LOG=DEBUG")
                    utils.cmd("pwd")
                    utils.cmd (" cp -Rv ./* ~/devops_sources")
                    echo "cd ~/devops_sources/servers-deploy/terraform/ && pwd && bash ~/devops_sources/servers-deploy/terraform/apply.sh -name ${serverName} -os ${osName} -role ${roleName} -cpu ${cpu} -ram ${ram} -pass ${password} -ou ${ouName}"
                    returnCode =  utils.cmd("cd ~/devops_sources/servers-deploy/terraform/ && pwd && bash ~/devops_sources/servers-deploy/terraform/apply.sh -name ${serverName} -os ${osName} -role ${roleName} -cpu ${cpu} -ram ${ram} -pass ${password} -ou ${ouName}")
                    utils.cmd("ls -la")
                    if (returnCode != 0){
                        firstInitFail = true
                        echo returnCode
                    }
                }
            }
        }
        stage("Запись в consul"){
            agent {
                label "service_NewProject"
            }
            steps{  
                script{
                    if (!firstInitFail){
                        def consul = new Consul()
                        if (roleName == 'rabbitmq')
                        {
                            consul.putVal("${serverName}.bit-erp.loc", "${projectKey}/servers/${roleName}/${serverName}/server")
                            consul.putVal('/', "${projectKey}/servers/${roleName}/${serverName}/vshost")
                            consul.putVal('5672', "${projectKey}/servers/${roleName}/${serverName}/port")
                            consul.putVal(login, "${projectKey}/servers/${roleName}/${serverName}/login")
                            consul.putVal(password, "${projectKey}/servers/${roleName}/${serverName}/password")
                        }
                        else{
                            consul.putVal("${serverName}.bit-erp.loc", "${projectKey}/servers/${serverName}/server")
                            consul.putVal(login, "${projectKey}/servers/${serverName}/login")
                            consul.putVal(password, "${projectKey}/servers/${serverName}/password")
                        }
                    }
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