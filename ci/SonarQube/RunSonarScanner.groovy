@Library("shared-libraries")
import io.bit.Consul
import io.bit.Utils
import io.bit.BITConvJava
import io.bit.ProjectHelpers
import io.bit.JIRAIntegration

Utils utils = new Utils()
Consul consul = new Consul()
BITConvJava bITConvJava = new BITConvJava()
ProjectHelpers projectHelpers = new ProjectHelpers()
JIRAIntegration jIRAIntegration = new JIRAIntegration()
def newVersion

pipeline {

    agent {
        label "${env.jenkinsAgent}"
    }
    parameters {
        string(description: 'Project key in jira', name: 'projectKey')
        string(description: 'Type project: templatebases, main_extension, repository', name: 'type')
        string(defaultValue: "", description: 'Name template bases', name: 'templatebases')
        string(defaultValue: "", description: 'Path in repository', name: 'repozitoryPath')
        string(defaultValue: "newbdd", description: 'Jenkins agent for pipeline', name: 'jenkinsAgent')
        string(defaultValue: "jenkins", description: 'Optional. System parameter with issue key from SD', name: 'issueKey')
        string(defaultValue: "jenkins", description: 'Optional. Issue author from SD', name: 'jiraReporter')
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
                        if (type == 'templatebases') {
                            platform = consul.queryVal("${projectKey}/project_server_platform")
                            storage1cFile = consul.queryVal("${projectKey}/templatebases/${templatebases}/storage1C_file")
                            storageUser = consul.queryVal("${projectKey}/templatebases/${templatebases}/storage_user")
                        }
                        if (type == 'main_extension') {
                            platform = consul.queryVal("${projectKey}/project_server_platform")
                            storage1cFile = consul.queryVal("${projectKey}/templatebases/${templatebases}/main_extension_storage1C_file")
                            storage1cFile = storage1cFile.substring(0,storage1cFile.lastIndexOf("\\"))
                            storageUser = consul.queryVal("${projectKey}/templatebases/${templatebases}/storage_user")
                        }
                        projectName = repozitoryPath.substring(repozitoryPath.lastIndexOf("/") + 1, repozitoryPath.lastIndexOf(".git"))
                        version = consul.queryVal("${projectKey}/sonarqube/${projectName}/version",true)
                        projectDir = "${env.WORKSPACE}\\$projectName"
                    }
                }
            }
        }
        stage('Подготовка репозитория') {
            steps {
                timestamps {
                    script {
                        utils.cmd("git --git-dir=${env.WORKSPACE} config --global http.postBuffer 524288000")
                        returnCode = utils.cmd("git --git-dir=${env.WORKSPACE} clone $repozitoryPath")
                        if (returnCode != 0){
                            utils.cmd("git --git-dir=$projectDir\\.git --work-tree=$projectDir pull origin master")
                        }
                    }
                }
            }
        }
        stage("Получение исходников gitsync") {
            steps {
                timestamps {
                    script {
                        if (type == 'templatebases') {
                            returnCode = utils.cmd("gitsync export ${storage1cFile} $projectDir\\src\\cf -increment -v8version ${platform} --storage-user ${storageUser}")
                            if (returnCode != 0){
                                utils.raiseError("Ошибка при выполнения gitsync export")
                            }
                            if (fileExists("$projectDir\\src\\cf\\AUTHORS")) {
                                String autors = readFile(file: "$projectDir\\src\\cf\\AUTHORS", encoding: "UTF-8")
                                for (String autor : autors.trim().split("\n")){
                                    String email = autor.substring(autor.indexOf("<") + 1,autor.indexOf(">"))
                                    autor = autor.substring(0,autor.indexOf("="))
                                    try{
                                        String emailNew = jIRAIntegration.getEmailByLogin(autor)
                                        autors = autors.replaceAll(email,emailNew)
                                    }catch (Exception ex){
                                       echo ex.toString()
                                    }
                                }
                                writeFile(file: "$projectDir\\src\\cf\\AUTHORS", text: autors, encoding: "UTF-8")
                            }
                        }
                        if (type == 'main_extension'){
                            utils.cmd("\"C:\\Program Files (x86)\\gitsync3\\gitsync.exe\" sync --storage-user ${storageUser} --storage-pwd ${bITConvJava.getAdmin1cPwd()} --extension ${bITConvJava.getMainExtension()} ${storage1cFile}  $projectDir\\src\\cf ")
                        }
                        if (fileExists ("$projectDir\\src\\cf\\VERSION") ) {
                            def newVersionText = readFile (encoding: 'UTF-8', file:"$projectDir\\src\\cf\\VERSION")
                            newVersion = (newVersionText =~ /<VERSION>(.*)<\/VERSION>/)[0][1]
                            consul.putVal(newVersion, projectKey + "/sonarqube/" + projectName + "/version")
                        }
                        writeFile(file: "$projectDir\\.gitignore", text: "/.scannerwork/", encoding: "UTF-8")
                    }
                }
            }
        }
        stage("Настройка файла конфигурации sonar-project.properties") {
            steps{
                timestamps{
                    script{
                        if (newVersion > version && type == 'templatebases') {
                            utils.cmd("cd ${projectDir}\n vanessa-sonar configure -k ${projectName} -n ${projectName} --src $projectDir\\src\\cf --group --subsystemsConfPath ${env.WORKSPACE}\\ci\\SonarQube\\sonar.subsystem.json")
                        }
                    }
                }
            }
        }
        stage("Экспорт исходников в Bitbucket") {
            steps {
                timestamps {
                    script {
                        if (newVersion > version && type != 'repository') {
                            utils.cmd("git --git-dir=$projectDir\\.git --work-tree=$projectDir config --system core.longpaths true")
                            utils.cmd("git --git-dir=$projectDir\\.git --work-tree=$projectDir pull origin master")
                            utils.cmd("git --git-dir=$projectDir\\.git --work-tree=$projectDir add --all")
                            utils.cmd("git --git-dir=$projectDir\\.git --work-tree=$projectDir commit -m $issueKey")
                            utils.cmd("git --git-dir=$projectDir\\.git --work-tree=$projectDir push origin master")
                        }
                    }
                }
            }
        }
        stage("Запуск Sonar-scanner") {
            steps {
                timestamps {
                    script {
                        if (newVersion > version) {
                            def configurationText
                            def configurationVersion
                            def scannerHome = tool 'sonar'
                            if (fileExists ("$projectDir\\src\\cf\\Configuration.xml") && type != 'main_extension') {
                                configurationText = readFile encoding: 'UTF-8', file:"$projectDir\\src\\cf\\Configuration.xml"
                                configurationVersion = (configurationText =~ /<Version>(.*)<\/Version>/)[0][1]
                                sonarCommand = "${scannerHome}/bin/sonar-scanner -Dsonar.projectVersion=${configurationVersion}"
                            }
                            else {
                                sonarCommand = "${scannerHome}/bin/sonar-scanner"
                            }
                            withSonarQubeEnv('SonarServer') {
                                returnCode = utils.cmd("cd $projectDir\n${sonarCommand}")
                                if (returnCode != 0){
                                    utils.raiseError("Ошибка при выполнения анализа sonar-scanner")
                                }
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

