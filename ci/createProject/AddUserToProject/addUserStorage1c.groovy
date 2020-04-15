#!groovy
@Library("shared-libraries")
import io.bit.ProjectHelpers
import io.bit.GitUtils
import io.bit.Consul
import io.bit.FileUtils
import io.bit.BITConvJava
import io.bit.JIRAIntegration

def projectHelpers = new ProjectHelpers()
def consul = new Consul()
def bitConvJava = new BITConvJava()
def jiraIntegration = new JIRAIntegration()
def addAuthorsTasks = [:]

pipeline {

    options {
        buildDiscarder(logRotator(daysToKeepStr:'30'))
        timeout(time: 28800, unit: 'SECONDS')
        skipDefaultCheckout true
    }

    parameters {
        string(description: 'Jenkins agent', name: 'jenkinsAgent')
        string(description: 'Project key in jira', name: 'projectKey')
        string(description: 'User', name: 'user')
        string(defaultValue: "", description: 'Optional. System parameter with issue key from SD', name: 'issueKey')
        string(defaultValue: "", description: 'Optional. Issue author from SD', name: 'jiraReporter')
        string(defaultValue: "", description: 'Obsolete', name: 'isTestJenkinsJob')
    }

    agent {
        label "${jenkinsAgent}"
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
                        projectKey = projectKey.toLowerCase()
                        user = user.toLowerCase()

                        mainExtName = bitConvJava.getMainExtension()
                        userEmail = jiraIntegration.getEmailByLogin(user)

                        storage1cUser = consul.queryVal("${projectKey}/storage_1c_pipeline_user")
                        storage1cPassword  = consul.queryVal("${projectKey}/storage_1c_pipeline_password")
                        platform = consul.queryVal("${projectKey}/project_server_platform") 
                        templateBases = consul.queryList("${projectKey}/templatebases")
                    }
                }
            }
        }
        
        stage("Добавление пользователя в хранилище 1С") {
            steps {
                timestamps {
                    script {

                        for (def baseId : templateBases) {
                            storage1cTCP = consul.queryVal("${projectKey}/templatebases/${baseId}/storage1C_tcp")
                            storage1cFile = consul.queryVal("${projectKey}/templatebases/${baseId}/storage1C_file")
                            mainExtSorageTCP = consul.queryVal("${projectKey}/templatebases/${baseId}/main_extension_storage1C_tcp")
                            admin1cUser = consul.queryVal("${projectKey}/templatebases/${baseId}/admin_1c_user")
                            admin1cPwd = consul.queryVal("${projectKey}/templatebases/${baseId}/admin_1c_password")
                            server = consul.queryVal("${projectKey}/templatebases/${baseId}/server")
                            basename = consul.queryVal("${projectKey}/templatebases/${baseId}/base")
                            admin1cPassword = consul.queryVal("${projectKey}/templatebases/${baseId}/admin_1c_password")

                            if (!projectHelpers.checkStorage1cUser(storage1cFile, user)) {
                                projectHelpers.createRepoUser(storage1cTCP, storage1cUser, storage1cPassword, user, storage1cPassword)
                            }
                        }
                    }
                }
            }
        }
        stage("Добавление пользователя в хранилище расширения") {
            steps {
                timestamps {
                    script {

                        for (def baseId : templateBases) {
                            storage1cTCP = consul.queryVal("${projectKey}/templatebases/${baseId}/storage1C_tcp")
                            mainExtSorageTCP = consul.queryVal("${projectKey}/templatebases/${baseId}/main_extension_storage1C_tcp")
                            mainExtSorageFile = consul.queryVal("${projectKey}/templatebases/${baseId}/main_extension_storage1C_file")
                            admin1cUser = consul.queryVal("${projectKey}/templatebases/${baseId}/admin_1c_user")
                            admin1cPwd = consul.queryVal("${projectKey}/templatebases/${baseId}/admin_1c_password")
                            server = consul.queryVal("${projectKey}/templatebases/${baseId}/server")
                            basename = consul.queryVal("${projectKey}/templatebases/${baseId}/base")
                            admin1cPassword = consul.queryVal("${projectKey}/templatebases/${baseId}/admin_1c_password")

                            if (!projectHelpers.checkStorage1cUser(mainExtSorageFile, user)) {
                                try {
                                    projectHelpers.createStorage1cExtUser(
                                        server, 
                                        platform, 
                                        basename, 
                                        admin1cUser, 
                                        admin1cPwd,
                                        mainExtSorageTCP, 
                                        storage1cUser, 
                                        storage1cPassword,
                                        mainExtName, 
                                        user, 
                                        storage1cPassword
                                    )
                                } catch (excp) {
                                    echo "Adding user to extension repo failed with error: ${excp.getMessage()}"
                                }
                            }
                        }
                    }
                }
            }
        }
        
        stage("Запуск добавления пользователя в репозитории на bitbucket") {
            steps {
                timestamps {
                    script { 
                        // создаем пустые каталоги
                        dir ('build') {
                            writeFile file:'dummy', text:''
                        }
                          // создаем пустые каталоги
                        dir ('build/sourcerepos') {
                            writeFile file:'dummy', text:''
                        }
                        templatebases = consul.queryList("${projectKey}/templatebases")
                        for (def baseId : templatebases) {
                            
                            tempRepoFolder = "${env.WORKSPACE}/build/sourcerepos"
                            dir (tempRepoFolder) {writeFile file:'dummy', text:''}
                            
                            authorsLine = "${user}=${user} <${userEmail}>"

                            storages1c = []
                            storages1c.add(get1cStorageRepo(baseId, tempRepoFolder, "storage1C_git"))
                            storages1c.add(get1cStorageRepo(baseId, tempRepoFolder, "main_extension_storage1C_git"))
                            for (def store1c : storages1c) {
                                if (store1c == null) {
                                    continue
                                }
                                repoName = getRepoNameFromGitUrl(store1c)
                                gitFolder = "${tempRepoFolder}/${repoName}"
                                addAuthorsTasks["addAuthorsTaks_${repoName}"] = addAuthorsTask(tempRepoFolder, user, repoName, store1c, gitFolder, authorsLine)
                            }
                        }
                        parallel addAuthorsTasks
                    }
                }
            }
        }
    }
    post {
        always {
            script {
                deleteDir()
            }
        }
    }
}

def addAuthorsTask(tempRepoFolder, user, repoName, storage1cGit, gitFolder, authorsLine) {
    return {
        stage("Добавление в репозиторий ${repoName}") {
            timestamps {
                
                def gitUtils = new GitUtils()
                def fileUtils = new FileUtils()

                echo "working with repo of ${repoName} located in ${gitFolder}"
                
                def authorsPath = "src/cf/AUTHORS"

                gitUtils.clone(storage1cGit, tempRepoFolder, true);

                gitUtils.reset(gitFolder)
                try {
                    gitUtils.checkoutOneFile(authorsPath, gitFolder)
                } catch (excp) {
                    echo "Cannot chekout file ${authorsPath}. Assuming that file does not exists we interrupt script for adding user to AUTHORS file. Detailed error is: ${excp.getMessage()}"
                    return
                }
                fileUtils.writeLineToFile("${gitFolder}/${authorsPath}", authorsLine, user)
                gitUtils.add(authorsPath, gitFolder)

                if (gitUtils.hasStagedFiles(gitFolder)) {
                    // Если AUTHORS  был модифицирован
                    gitUtils.commit("adding new user ${user} from pipeline script", gitFolder)
                    gitUtils.push("master", gitFolder)
                }
            }
        }
    }
}

def get1cStorageRepo(baseId, workspace, typeRepo) {
    def consul = new Consul()
    gitUtils = new GitUtils()

    storage1cGit = consul.queryVal("${projectKey}/templatebases/${baseId}/${typeRepo}", true)
    if (storage1cGit == null)
        return null
    
    gitFolder = "${workspace}/${getRepoNameFromGitUrl(storage1cGit)}"
    repoExists = gitUtils.validateRepoUrl(storage1cGit, gitFolder)
    if (!repoExists) {
        return null
    }
    return storage1cGit
}

def getRepoNameFromGitUrl(url) {
    nameParts = url.split("/");
    return nameParts[nameParts.length - 1].replaceAll(".git", "");
}
