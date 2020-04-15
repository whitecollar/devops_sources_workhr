// Содержит самостоятельные переиспользуемые шаги пайплайнов дженкинса
package io.bit

// ШАГИ ОБНОВЛЕНИЯ БАЗ 1С

def loadTestBaseTask(platform, storageTCP, storageEDT, userBaseConnString, userBaseServer , userBase, admin1cUser, admin1cPassword, storageUser, storagePwd,
     mergeType, baseId, projectDir, sourcesPath, cfgfileName, mergeSettingsPath, singleCfgEDT, cfgTempFileBase, distibFolder, storageExtTCP, mainExtension, edtBranch) {
    return {
        stage("loading from storage ${userBase}") {
            timestamps {
                echo "Executing updating from storage..."
                utils = new Utils()
                gitUtils = new GitUtils()
                fileUtils = new FileUtils()
                prHelpers = new ProjectHelpers()
                shortPlatform = utils.shortPlatformName(platform)

                if (mergeType == "MERGE_DISTRIBUTION" && storageEDT != null) {
                    echo "Merging with EDT storage using SUPPORT"
                    if (singleCfgEDT == null) {
                        gitUtils.checkoutSCM(storageEDT, projectDir, edtBranch)
                        prHelpers.exportEDTSources(utils, sourcesPath, projectDir, baseId)
                        prHelpers.compileSourcesToCfg(utils, sourcesPath, cfgfileName, userBaseConnString, platform)
                    } else {
                        fileUtils.copyFile(singleCfgEDT, cfgfileName)
                    }
                    prHelpers.createDb(utils, platform, null, cfgTempFileBase, cfgfileName, false)
                    prHelpers.createDistribPackage(utils, platform, null, cfgTempFileBase, null, null, distibFolder)
                    prHelpers.updateFromDistribPackage(utils, platform, userBaseServer, userBase, admin1cUser, admin1cPassword, distibFolder)
                } else if (mergeType == "MERGE_DISTRIBUTION" && storageEDT == null) {
                    utils.raiseError("Обновление через поставку из 1с хранилища не реализовано")
                } else if (mergeType == "MERGE_CFG" && storageEDT != null) {
                    echo "Merging with EDT storage using MergeSettings.xml ..."
                    gitUtils.checkoutSCM(storageEDT, projectDir, "master")
                    if (singleCfgEDT == null) {
                        prHelpers.exportEDTSources(utils, sourcesPath, projectDir, baseId)
                        prHelpers.compileSourcesToCfg(utils, sourcesPath, cfgfileName, userBaseConnString, platform)
                    } else {
                        fileUtils.copyFile(singleCfgEDT, cfgfileName)
                    }
                    prHelpers.disableSupportCfg(utils, platform, userBaseServer, userBase, admin1cUser, admin1cPassword)
                    prHelpers.disableSupportCfg(utils, platform, userBaseServer, userBase, admin1cUser, admin1cPassword)
                    prHelpers.mergeCfg(utils, userBaseConnString, cfgfileName, mergeSettingsPath, admin1cUser, admin1cPassword, platform) 
                } else if (mergeType == "MERGE_CFG" && storageTCP != null) { 
                    echo "Merging with 1C storage using MergeSettings.xml ..."
                    utils.raiseError("Сравнение объедиение с использованием MergeSettings.xml не реализовано, т.к. ни один проект того не требует ")
                    // Обновляем из хранилищаа
                    // cmd("runner loadrepo --storage-name ${env.ADAPTER_STORAGE} --storage-user auto --ibconnection ${env.ADAPTER_TEMPLATE_IBCONNECTION} --v8version 8.3.12")
                    // Обновляем конфигурацию БД адаптера
                    // cmd("runner updatedb --ibconnection ${env.ADAPTER_TEMPLATE_IBCONNECTION} --v8version 8.3.12")
                    // Выгружаем конфигурацию адаптера
                    // cmd("runner unload build/adapter.cf --ibconnection ${env.ADAPTER_TEMPLATE_IBCONNECTION}")
                    // Мерджим с эталонной ЕРП
                    // cmd("vrunner merge --src=./build/adapter.cf --merge-settings=./MergeSettingsERPАдаптер33_0_0_17.xml --force --ibconnection ${env.ERP_TEMPLATE_IBCONNECTION} --db-user ${env.ADMIN_USER} --db-pwd ${env.ADMIN_PASSW}")
                } else if (mergeType == "LOAD") {
                    echo "Loading from 1C storage..."
                    prHelpers.loadCfgFrom1CStorage(utils, storageTCP, storageUser, storagePwd, userBaseConnString, admin1cUser, admin1cPassword, platform)
                    try {
                        prHelpers.loadExtensionFromStorage(userBaseServer, platform, userBase, admin1cUser, admin1cPassword, storageExtTCP, storageUser, storagePwd, mainExtension)
                    } catch (excp) {
                        echo "error happend when loading extension from storage ${storageExtTCP}. Skip the error"
                    }
                } else {
                     utils.raiseError("Неверный параметр mergeType = " + mergeType) 
                }
                prHelpers.unbindRepo(utils, userBaseServer, userBase, platform, admin1cUser, admin1cPassword)
            }
        }
    }
}

def loadDevBaseTask(platform, userBaseConnString, devBaseConnString, userBaseServer , userBase, admin1cUser, admin1cPassword, 
    cfgfileName, mainExtension, cfgExtfileName, devBase) {
    return {
        stage("loading from devbase ${userBase}") {
            timestamps {
                utils = new Utils()
                prHelpers = new ProjectHelpers()
                
                prHelpers.unloadCfg(utils, devBaseConnString, cfgfileName, admin1cUser, admin1cPassword, platform)
                prHelpers.loadCfg(utils, userBaseConnString, cfgfileName, admin1cUser, admin1cPassword, platform)
                try {
                    prHelpers.unloadExtensionToFile(userBaseServer, platform, devBase, admin1cUser, admin1cPassword, cfgExtfileName, mainExtension)
                    prHelpers.loadExtensionFromFile(userBaseServer, platform, userBase, admin1cUser, admin1cPassword, cfgExtfileName, mainExtension)
                } catch (excp) {
                    echo "Error happened when unloading / loading extension ${mainExtension} for ${devBase}. Skipping the error"
                }
            }
        }
    }
}

def updatedbTask(connString, server, infobase, admin1cUser, admin1cPassword, platform, mainExtension) {
    return {
        stage("Updating db in designer ${infobase}") {
            timestamps {
                utils = new Utils()
                prHelpers = new ProjectHelpers()
                
                prHelpers.updateInfobase(utils, connString, admin1cUser, admin1cPassword, platform)
                try {
                    prHelpers.updateExtension(server, platform, infobase, admin1cUser, admin1cPassword, mainExtension)
                } catch (excp) {
                    echo "error happend when updating extension in designer mode for ${infobase}. Skipping the error"
                }
                prHelpers.runUpdatingHandlers(utils, connString, admin1cUser, admin1cPassword, platform)
            }
        }
    }
}

def dropUsersTask(projectServer, platform, admin1cUser, admin1cPassword, userBase) {
    return {
        stage("drop users from ${userBase}") {
            timestamps {
                def projectHelpers = new ProjectHelpers()
                def utils = new Utils()

                projectHelpers.dropDb(utils, platform, projectServer, userBase, admin1cUser, admin1cPassword)

                try {
                    projectHelpers.createDb(utils, platform, projectServer, userBase, null, true)
                } catch (except) {
                    echo "Error happened when creating infobase with RAS equal true. Let's try again via plain command line mode"
                    projectHelpers.createDb(utils, platform, projectServer, userBase, null, false)
                }
            }
        }
    }
}

def addTolistTask(server1c, projectKey, infobase, user) {
    return {
        stage("Добавление базы в список на проектном сервере") {
            timestamps {
                script {
                    def projectHelpers = new ProjectHelpers()
                    def bitConvJava = new BITConvJava()

                    projectHelpers.addTolistTask(
                        bitConvJava.userServiceAgent(user, projectKey),
                        server1c,
                        infobase,
                        user
                    )
                }
            }
        }
    }
}

def createInfobaseTask(projectServer, platform, infobase, connString, admin1cUser, admin1cPwd, cfdtpath = null) {
    return {
        stage("Создание  базы") {
            timestamps {
                script {
                    def projectHelpers = new ProjectHelpers()
                    def utils = new Utils()

                    if (projectHelpers.testInfobaseConnectionRAS(projectServer, infobase, platform, admin1cUser, admin1cPwd)) {
                        projectHelpers.dropDb(utils, platform, projectServer, infobase, admin1cUser, admin1cPwd, true)
                    }
                    projectHelpers.createLoadDb(platform, projectServer, infobase, connString, "", "",  cfdtpath)
                }
            }
        }
    }
}
def createUser1cTask(server1c, platform, infobase, connString, cfdtpath, login, password, newLogin, newPassword) {
    return {
        stage("Создание дефолтного администратора") {
            timestamps {
                script {
                    def projectHelpers = new ProjectHelpers()
                    def utils = new Utils()

                    if (cfdtpath.isEmpty() || cfdtpath.endsWith(".cf")) {
                        return
                    }

                    if (cfdtpath.endsWith(".cf")) {
                        // Мы всегда заменяем прежнюю базу на пустую, даже если это cf
                        login = ""
                        password = ""
                    } 

                    if (!projectHelpers.testInfobaseConnectionRAS(server1c, infobase, platform, login, password)) {
                        utils.raiseError("Ошибка! Невозможно подключиться к базе ${infobase} под логином ${login} и паролем ${password} для создания администратора.")
                    }
                    
                    if (login == newLogin && password == newPassword) {
                        echo "Infobase ${baseId} already provided with correct user=${newLogin} and pwd=${newPassword} . There is no need to create default Admnistrator"
                        return
                    }

                    projectHelpers.copy1cInfobaseUser(connString, login, password, newLogin, newPassword)
                }
            }
        }
    }
}

def applySingleStorageEDT(templateBases, platform, edtBranch, user) {

    utils = new Utils()
    gitUtils = new GitUtils()
    consul = new Consul()
    prHelpers = new ProjectHelpers()

    singleStorageEDT = null
    baseConnString = null
    for (def baseId : templateBases) { 
        
        storageEDT = consul.queryVal("${projectKey}/templatebases/${baseId}/storage_edt")
        mergeType = consul.queryVal("${projectKey}/templatebases/${baseId}/mergetype")
        if (user != null) {
            baseConnString = consul.queryVal("${projectKey}/users/${user}/testbases/${baseId}/connection_string")
        } else {
            baseConnString = consul.queryVal("${projectKey}/templatebases/${baseId}/connection_string")
        }

        if (!(mergeType == "MERGE_CFG" || mergeType == "MERGE_DISTRIBUTION")) {
            // продолжаем, возможно следующая конфа будет этого типа
            continue
        }

        if (singleStorageEDT == null) {
            singleStorageEDT = storageEDT
        } else if (singleStorageEDT != null && singleStorageEDT != storageEDT) {
            // конфигурации отличаются, значит единое хранилище EDT неприменимо
            singleStorageEDT = null;
            return null;
        }
    }

    if (singleStorageEDT == null) {
        return null;
    }

    echo "Using uber single 1c merging file"

    singleBase = "singleBase"
    projectDir = "${env.WORKSPACE}/build/edt/${singleBase}"
    sourcesPath = "${env.WORKSPACE}/build/exported/${singleBase}";
    cfgfileName = "${env.WORKSPACE}/build/${singleBase}_temp.cf"

    gitUtils.checkoutSCM(storageEDT, projectDir, edtBranch)
    prHelpers.exportEDTSources(utils, sourcesPath, projectDir, singleBase)
    prHelpers.compileSourcesToCfg(utils, sourcesPath, cfgfileName, baseConnString, platform)

    return cfgfileName;
}

// ШАГИ КОПИРОВАНИЯ БАЗ