@Library("shared-libraries")
import io.bit.Consul
import io.bit.ProjectHelpers
import io.bit.Utils
import io.bit.FileUtils
import io.bit.SqlUtils
import io.bit.JIRAIntegration

def consul = new Consul()
def projectHelpers = new ProjectHelpers()
def jiraIntegration = new JIRAIntegration()
def utils = new Utils()
def jobtasks = [:]

pipeline {

    agent {
        label "service_NewProject"
    }

    options {
        timeout(time: 60000, unit: 'SECONDS') 
        buildDiscarder(logRotator(daysToKeepStr:'30'))
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

        stage("Запуск заданий") {
            steps {
                timestamps {
                    script {
                        projects = consul.queryList("")
                        for (def projectKey : projects) {
                           
                            projectServer = consul.queryVal("${projectKey}/project_server", true)
                            platform = consul.queryVal("${projectKey}/project_server_platform", true)
                            
                            if (projectServer == null || platform == null || !utils.pingServer(projectServer, true)) {
                                // на этом проекте нет своего проектного сервера и платформы 1С
                                continue
                            }
                
                            custombasesUsers = consul.queryList("${projectKey}/custombases")
                            for (def customUser : custombasesUsers) {
                                
                                custombases = consul.queryList("${projectKey}/custombases/${customUser}") 
                                for (def baseId : custombases) {

                                    autoload = consul.queryVal("${projectKey}/custombases/${customUser}/${baseId}/autoload")
                                    if (autoload != "true") {
                                        echo "Autoload is off for ${baseId}. It skipped"
                                        continue
                                    }
                                    autoloadFolder = consul.queryVal("${projectKey}/custombases/${customUser}/${baseId}/autoload_folder")
                                    cfdtpath = getLastBackupFile(autoloadFolder)
                                    if (cfdtpath == null) {
                                        echo "No suitable backups was found in folder ${autoloadFolder}. Skipping ${baseId}"
                                        continue
                                    }

                                    echo "Backup has been found! Processing  ${cfdtpath}..."

                                    custombaseServer = consul.queryVal("${projectKey}/custombases/${customUser}/${baseId}/server")
                                    customBaseConnString = consul.queryVal("${projectKey}/custombases/${customUser}/${baseId}/connection_string")
                                    customBase = consul.queryVal("${projectKey}/custombases/${customUser}/${baseId}/base")
                                    admin1cUser = consul.queryVal("${projectKey}/custombases/${customUser}/${baseId}/admin_1c_user")
                                    admin1cPwd = consul.queryVal("${projectKey}/custombases/${customUser}/${baseId}/admin_1c_password")
                                    
                                    jobtasks["loading backup ${customBase}"] = loadBackupTask(
                                        projectServer, 
                                        projectServer, 
                                        cfdtpath, 
                                        platform,
                                        customBase,
                                        customBaseConnString,
                                        admin1cUser,
                                        admin1cPwd
                                    )
                                }
                            }
                        }
                        parallel jobtasks
                    }
                }
            }
        }
    }
    post {
        always {
            script {

                projectHelpers.createJiraBugIfFailed(null, null, jiraIntegration.getEpicCustomBase(), 
                    "Упал пайплайн загрузки бекапов произвольных баз от ${utils.currentDateWithFormat("dd.MM.yyyy")}")

                projectHelpers.beforeEndJob()
            }
        }
    }
}

def loadBackupTask(jenkinsNode, projectServer, cfdtpath, platform, customBase, customBaseConnString, admin1cUser, admin1cPwd) {
    return {
        node (jenkinsNode) {
            stage("Загрузка базы ${projectServer}/${customBase}") {
                timestamps {
                    checkout scm
                    utils = new Utils()
                    sqlUtils = new SqlUtils()
                    projectHelpers = new ProjectHelpers()

                    if (projectHelpers.testInfobaseConnectionRAS(projectServer, customBase, platform, admin1cUser, admin1cPwd)) {
                        projectHelpers.dropDb(utils, platform, projectServer, customBase, admin1cUser, admin1cPwd, false)
                    }
                    
                    projectHelpers.createLoadDb(platform, projectServer, customBase, customBaseConnString, "", "",  cfdtpath)
                }
            }
        }
    }
}

def getLastBackupFile(path) {
    fileUtils = new FileUtils()
    folderFiles = fileUtils.getAllFiles(path)

    if (folderFiles.size() == 0) {
        return null
    }

    latestBackup = folderFiles[0]
    for (def currFile : folderFiles) {
        if (currFile.name.endsWith(".bak") || currFile.name.endsWith(".cf") ||  currFile.name.endsWith(".dt")) {
            if (currFile.lastModified() > latestBackup.lastModified()) {
                latestBackup = currFile
            }
        }
    }
    return latestBackup.toString()
}