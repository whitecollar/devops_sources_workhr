package io.bit
import groovy.json.JsonSlurperClassic

// Заполняет devbases и testbases для пользователя в консуле
//
// Параметры:
//  projectKey - ключ проекта
//  user - имя пользователя
//  baseId - ID базы (например erp)
//  consul - экземпляр класса библиотеки Consul.groovy
//
def createbases(projectKey, user, baseId, consul) {

    def bitConvJava = new BITConvJava()
    
    def userGroup = "${projectKey}/users/${user}"
    def baseSuffix = bitConvJava.combineUserTestbase(user, baseId, projectKey)

    projectServer = consul.queryValFromConsul(projectKey, "project_server")
    admin1cUser = consul.queryValFromConsul(projectKey, "templatebases/${baseId}/admin_1c_user")
    admin1cPassword = consul.queryValFromConsul(projectKey, "templatebases/${baseId}/admin_1c_password")
    //test bases
    consul.putVal("", userGroup+"/testbases/${baseId}/")
    consul.putVal("${projectServer}", userGroup+"/testbases/${baseId}/server")
    consul.putVal("${baseSuffix}", userGroup+"/testbases/${baseId}/base")
    consul.putVal("Srvr=\"${projectServer}\";Ref=\"${baseSuffix}\";", userGroup+"/testbases/${baseId}/conn_string1C")
    consul.putVal("/S${projectServer}\\${baseSuffix}", userGroup+"/testbases/${baseId}/connection_string")
    consul.putVal("${baseId}", userGroup+"/testbases/${baseId}/base_id")
    consul.putVal("${admin1cUser}", userGroup+"/testbases/${baseId}/admin_1c_user")
    consul.putVal("${admin1cPassword}", userGroup+"/testbases/${baseId}/admin_1c_password")
    // dev bases
    consul.putVal("", userGroup+"/devbases/${baseId}/")
    consul.putVal("${projectServer}", userGroup+"/devbases/${baseId}/server")
    consul.putVal("dev_${baseSuffix}", userGroup+"/devbases/${baseId}/base")
    consul.putVal("Srvr=\"${projectServer}\";Ref=\"dev_${baseSuffix}\";", userGroup+"/devbases/${baseId}/conn_string1C") 
    consul.putVal("/S${projectServer}\\dev_${baseSuffix}", userGroup+"/devbases/${baseId}/connection_string")
    consul.putVal("${baseId}", userGroup+"/devbases/${baseId}/base_id")
    consul.putVal("${admin1cUser}", userGroup+"/devbases/${baseId}/admin_1c_user")
    consul.putVal("${admin1cPassword}", userGroup+"/devbases/${baseId}/admin_1c_password")
}

def createSmokebases(user, baseId, projectKey) {

    consul = new Consul()
    bitConvJava = new BITConvJava()

    useTemplateBase = (consul.queryVal("${projectKey}/templatebases/${baseId}/base", true) != null)

    if (useTemplateBase) {
        basename = bitConvJava.combineUserTestbase(user, baseId, projectKey)
        admin1cUser = consul.queryValFromConsul(projectKey, "templatebases/${baseId}/admin_1c_user")
        admin1cPwd = consul.queryValFromConsul(projectKey, "templatebases/${baseId}/admin_1c_password")
    } else {
        basename = bitConvJava.combineSmoketestBase(user, baseId, projectKey)
        admin1cUser = bitConvJava.getAdmin1cUser()
        admin1cPwd = bitConvJava.getAdmin1cPwd()
    }
    
    projectServer = consul.queryValFromConsul(projectKey, "project_server")
    
    connString1c = bitConvJava.combineConnString1c(projectServer, basename)
    connString = bitConvJava.combineConnString(projectServer, basename)
    
    userGroup = "${projectKey}/users/${user}/smokebases"
    
    consul.putVal("", "${userGroup}/${baseId}/")
    consul.putVal(projectServer, "${userGroup}/${baseId}/server")
    consul.putVal(basename, "${userGroup}/${baseId}/base")
    consul.putVal(connString1c, "${userGroup}/${baseId}/conn_string1C")
    consul.putVal(connString, "${userGroup}/${baseId}/connection_string")
    consul.putVal(baseId, "${userGroup}/${baseId}/base_id")
    consul.putVal(admin1cUser, "${userGroup}/${baseId}/admin_1c_user")
    consul.putVal(admin1cPwd, "${userGroup}/${baseId}/admin_1c_password")
}

// Удаляет бекапы из сетевой шары
//
// Параметры:
//  utils - экземпляр библиотеки Utils.groovy
//  backup_path - путь к бекапам
//
def clearBackups(utils, backup_path) {
    echo "Deleting file ${backup_path}..."
    returnCode = utils.cmd("oscript ${env.WORKSPACE}/one_script_tools/deleteFile.os -file ${backup_path}")
    if (returnCode != 0) {
        echo "Error when deleting file: ${backup_path}"
    }    
}

// Убирает в 1С базу окошки с тем, что база перемещена, интернет поддержкой, очищает настройки ванессы
//
// Параметры:
//  utils - экземпляр библиотеки Utils.groovy
//  сonnection_string - путь к 1С базе.
//  admin_1c_user - имя админа 1С базы
//  admin_1c_password - пароль админа 1С базы
//
def unlocking1cBase(utils, onnection_string, admin_1c_user, admin_1c_password) {
    utils.cmd("runner run --execute ${env.WORKSPACE}/one_script_tools/unlockBase1C.epf --command \"-locktype unlock -usersettingsprovider FILE\" --db-user ${admin_1c_user} --db-pwd ${admin_1c_password} --ibconnection=${onnection_string}")
}

// Заполняет пользовательские параметры консула в базе для ванессы
//
// Параметры:
//  utils - экземпляр библиотеки Utils.groovy
//  base_connstring - путь к 1С базе.
//  admin_1c_user - имя админа 1С базы
//  admin_1c_password - пароль админа 1С базы
//  projectKey - ключ проекта
//  user - пользователь консула
//
def preparingVannessa(utils, base_connstring, admin_1c_user, admin_1c_password, projectKey, user) {
    returnCode = utils.cmd("runner run --execute ${env.WORKSPACE}/one_script_tools/unlockBase1C.epf --command \"-locktype unlock -usersettingspath http://127.0.0.1:8500/v1/kv/${projectKey}/users/${user}/ -usersettingsprovider CONSUL\" --db-user ${admin_1c_user} --db-pwd ${admin_1c_password} --ibconnection=${base_connstring}")
    if (returnCode != 0) {
        utils.raiseError("Возникла ошибка при запуске обработки заполнения пользовательских настроек в базе ${base_connstring} для пользователя ${user}")
    }
}

def preparingVannessaAdnPublishWS(base_connstring, admin_1c_user, admin_1c_password, projectKey, user, defaultvrdfile) {
    utils = new Utils()
    returnCode = utils.cmd("runner run --execute ${env.WORKSPACE}/one_script_tools/unlockBase1C.epf --command \"-locktype unlockpublishws -defaultvrdfile ${defaultvrdfile} -usersettingspath http://127.0.0.1:8500/v1/kv/${projectKey}/users/${user}/ -usersettingsprovider CONSUL\" --db-user ${admin_1c_user} --db-pwd ${admin_1c_password} --ibconnection=${base_connstring}")
    if (returnCode != 0) {
        utils.raiseError("Возникла ошибка при запуске обработки заполнения пользовательских настроек в базе ${base_connstring} для пользователя ${user}")
    }
}


// OBSOLETE. Снимает блокировку РЗ в кластере для базы черезе powershell
//
// Параметры:
//  utils - экземпляр библиотеки Utils.groovy
//  baseServer - сервер
//  base - база
//  admin_1c_user - имя админа 1С базы
//  admin_1c_password - пароль админа 1С базы
//
def unlockScheduledJobsPowershell(utils, baseServer, base, admin1cUser, admin1cPassword) {
    // TODO: Легаси скрипт, временно пока не починим скрипт RAC
     utils.cmd("scheduled_jobs/launcher64.bat unblock ${baseServer} ${base} ${admin1cUser} ${admin1cPassword}")
}

// Удаляет базу из кластера через RAS. При неудачной попытке удаляет базу через powershell.
//
// Параметры:
//  utils - экземпляр класса библиотеки Utils.groovy
//  shortPlatform - номер платформы 1С, например 8.3.12
//  baseServer - сервер sql 
//  base - имя базы на сервере sql
//  admin1cUser - имя администратора 1С в кластере для базы
//  admin1cPassword - пароль администратора 1С в кластере для базы
//  fulldrop - если true, то удаляется база из кластера 1С и sql сервера
//
def dropDb(utils, shortPlatform, baseServer, base, admin1cUser, admin1cPassword, fulldrop = false) {

    fulldropLine = "";
    if (fulldrop) {
        fulldropLine = "-fulldrop true"
    }

    returnCode = utils.cmd("oscript one_script_tools/drop_db.os -rac ${baseServer} -platform ${shortPlatform} -base ${base} -admin1c ${admin1cUser} -passw1c ${admin1cPassword} ${fulldropLine}")
    if (returnCode != 0) {
        returnCode = utils.cmd("oscript one_script_tools/drop_db.os -rac ${baseServer} -platform ${shortPlatform} -base ${base} -admin1c ${admin1cUser} -passw1c ${admin1cPassword}")
        if (returnCode != 0) { 
            utils.raiseError("Возникла ошибка при повторной попытке удаления базы из кластера через RAS ${baseServer}\\${base}")
        }
        //echo "Echo Deleting base ${baseServer}\\${base} with RAS failed. Attempting to delete base with powershell COM."
        //returnCode = utils.cmd("powershell -file ${env.WORKSPACE}/copy_etalon/drop_db.ps1 -srv ${baseServer} -infobase ${base} -user ${admin1cUser} -passw ${admin1cPassword}")
        //if (returnCode != 0) { 
        //    eror "error when deleting base with COM ${baseServer}\\${base}. See logs above fore more information."
        //}
    }
}

// Очищает клиентский кеш через powershell.
def clearClientCache() {
    def utils = new Utils()
    returnCode = utils.cmd("powershell -file ${env.WORKSPACE}/one_script_tools/clearCache.ps1")
    if (returnCode != 0) { 
        echo "Unstable clearing cache. See logs above fore more information."
    }
    
}

// Очищает клиентский кеш с отбором по базе и по пользователю. Отбор по пользователю - необязательный.
//
// Параметры:
//
//  server - сервер баз
//  basefilter - имя базы
//  user - Необязательный. Имя пользователя для которго очищать кеш. Если не указан, то очищается для  всех.
//
def clearClientCacheFilter(server, basefilter, user = "") {
    def utils = new Utils()
    userLine = "";
    if (!user.isEmpty()) {
        userLine = "-userfilter ${user}"
    }

    retunStatus = utils.cmd("oscript one_script_tools/clearCache.os -server ${server} -basefilter ${basefilter} ${userLine}")
    if (retunStatus != 0) {
        utils.raiseError("Очистка клиентского кеша для базы ${basefilter} и пользователю ${user} завершилась с ошибкой")
    }
}

// Создает базу в кластере через RAS или пакетный режим. Для пакетного режима есть возможность создать базу с конфигурацией\
// Для RAS - если базы нет в СУБД, то скрипт упадет.
//
// Параметры:
//  utils - экземпляр класса библиотеки Utils.groovy
//  platform - номер платформы 1С, например 8.3.12.1529
//  server - сервер sql 
//  base - имя базы на сервере sql
//  cfdt - файловый путь к dt или cf конфигурации для загрузки. Только для пакетного режима!
//  isras - если true, то используется RAS для скрипта, в противном случае - пакетный режим
//
def createDb(utils, platform, server, base, cfdt, isras) {
    cfdtpath = ""
    if (cfdt != null && !cfdt.isEmpty()) {
        cfdtpath = "-cfdt ${cfdt}"
    }
    serverpath = "" 
    if (server != null) {
        serverpath = "-server ${server}"
    }
    israspath = ""
    if (isras) {
        israspath = "-isras true"
    }
    returnCode = utils.cmd("oscript one_script_tools/dbcreator.os -platform ${platform} ${serverpath} -base ${base} ${cfdtpath} ${israspath}")
    if (returnCode != 0) {
        utils.raiseError("Возникла ошибка при создании базы ${base} в кластере ${server}")
    }
}

// Создает базу в кластере через RAS и сразу загружает в нее конфигурацию, если нужно.
//
// Параметры:
//  platform - номер платформы 1С, например 8.3.12.1529
//  server1c - сервер 1с. Сервер sql должен располагаться там же
//  infobase - имя базы на сервере sql
//  infobaseConnString = строка соединения вида /S${server}\\${infobase} 
//  admin1cUser - администратор базы 1С (имеет смысл только для cf и dt)
//  admin1cPwd - пароль администратора базы 1с (имеет смысл только для cf и dt)
//  confpath - файловый путь к bak, dt или cf конфигурации для загрузки.
//
def createLoadDb(platform, server1c, infobase, infobaseConnString, admin1cUser, admin1cPwd, confpath = null) {
    sqlUtils = new SqlUtils()
    utils = new Utils()

    if (confpath == null || confpath.isEmpty()) {
        sqlUtils.createEmptyDb(server1c, infobase)
        createDb(utils, platform, server1c, infobase, null, true)
    } else if (confpath.endsWith(".bak")) {
        sqlUtils.createEmptyDb(server1c, infobase)
        sqlUtils.restoreDb(server1c, infobase, confpath)
        createDb(utils, platform, server1c, infobase, null, true)
    } else if (confpath.endsWith(".cf")) {
        sqlUtils.createEmptyDb(server1c, infobase)
        createDb(utils, platform, server1c, infobase, null, true)
        loadCfg(utils, infobaseConnString, confpath, admin1cUser, admin1cPwd, platform)
        updateInfobase(utils, infobaseConnString, admin1cUser, admin1cPwd, platform)
    } else if (confpath.endsWith(".dt")) {
        sqlUtils.createEmptyDb(server1c, infobase)
        createDb(utils, platform, server1c, infobase, null, true)
        loadDt(platform, server1c, infobase, confpath, admin1cUser, admin1cPwd)
    } else {
        utils.raiseError("Файл ${confpath} не поддерживается в качесте источника для создания базы. Выберите файл с раширением *.cf, *.dt или *.bak")
    }
}

// Добавляет базу в список ibases.v8 для переданного пользователя и для jenkins
//
// Параметры:
//  utils - экземпляр библиотеки Utils.groovy
//  baseServer - сервер
//  base - база
//  user - имя пользователя, для корого добавляется база. Имя должно соответствовать имени в каталоге Documents. 
//         Также у  пользователя должны быть права записи в этот каталог
//
def addInfobaseToList(utils, baseServer, base, user) {
     utils.cmd("oscript one_script_tools/addInfobaseToList.os -server ${baseServer} -base ${base} -user ${user}")
}

// Разблокирует РЗ в кластере через RAS. ВНИМАНИЕ - из-за глюка скрипт требует монопольного доступа к базе!
//
// Параметры:
//  utils - экземпляр библиотеки Utils.groovy
//  shortPlatform - платформа 1С, например 8.3.12
//  baseServer - сервер
//  base - база
//  admin1cUser - имя админа базы в кластере серверов 
//  admin1cPassword - пароль админа базы в кластере серверов
//  unlock - если true, то разблокирует РЗ
//
def lockScheduledJobs(utils, shortPlatform, baseServer, base, admin1cUser, admin1cPassword, unlock = false) {
    // TODO: Скрипт не работает из-за бага в библиотеке irac которая требует монопольного доступа для блокировки разблокировки РЗ
    lokingTypePath = ""
    if (unlock) {
        lokingTypePath = "-lockingtype unlock"
    } else {
        lokingTypePath = "-lockingtype lock"
    }
    utils.cmd("oscript one_script_tools/scheduled_jobs.os -rac ${baseServer } -platform ${shortPlatform} -base ${base} ${lokingTypePath} -admin1c ${admin1cUser} -passw1c ${admin1cPassword}")
}

// Разблокирует РЗ в кластере через COM.
//
// Параметры:
//  utils - экземпляр библиотеки Utils.groovy
//  shortPlatform - платформа 1С, например 8.3.12
//  baseServer - сервер
//  base - база
//  admin1cUser - имя админа базы в кластере серверов 
//  admin1cPassword - пароль админа базы в кластере серверов
//
def lockScheduledJobsPowershell(utils, shortPlatform, baseServer, base, admin1cUser, admin1cPassword, unlock) {
    // TODO: Легаси скрипт, временно пока не починим скрипт RAC
    lokingTypePath = ""
    if (unlock) {
        lokingTypePath = "unblock"
    } else {
        lokingTypePath = "block"
    }
     utils.cmd("scheduled_jobs/launcher64.bat ${lokingTypePath} ${baseServer} ${base} ${admin1cUser} ${admin1cPassword}")
}

// Подключает базу к хранилищу 1С. Перед подключением рекомендуется выполнить отключение от хранилища. Вызывает ошибку в случае неудачи
//
// Параметры:
//  utils - экземпляр библиотеки Utils.groovy
//  storage1cTCP - путь к хранилищу, может быть как сетевым (tcp), так и файловым
//  storage1cUser - пользователь хранилища
//  server - сервер базы
//  infobase - имя подключаемой базы
//  platform - полный номер платформы 1с
//  admin1cUser - администратор базы
//  admin1cPassword - пароль администратора базы
//
def bindRepo(utils, storage1cTCP, storage1cUser, storage1cPassword, server, infobase, platform, admin1cUser, admin1cPassword) {

    admin1cUserLine = "";
    if (!admin1cUser.isEmpty()) {
        admin1cUserLine = "-user ${admin1cUser}"
    }
    admin1cPassLine = "";
    if (!admin1cPassword.isEmpty()) {
        admin1cPassLine = "-passw ${admin1cPassword}"
    }
    //TODO: Vanessa runner 1.6. не работает
    //retunStatus = utils.cmd("runner bindrepo ${storage1cTCP} ${storage1cUser} ${storage1cPassword} --ibconnection /S${server}\\${infobase} --v8version ${platform} ${admin1cUserLine} ${admin1cPassLine} --BindAlreadyBindedUser")
     
    retunStatus = utils.cmd("oscript one_script_tools/bindRepo.os -platform ${platform} -server ${server} -base ${infobase} ${admin1cUserLine} ${admin1cPassLine} -storage1c ${storage1cTCP} -storage1cuser ${storage1cUser} -storage1cpwd ${storage1cPassword}")
    if (retunStatus != 0) {
        utils.raiseError("Возникла ошибка при подключении базы ${infobase} к хранилищу 1С по адресу: ${storage1cTCP}. Смотрите логи для подробной информации")
    }
}

// Отключает базу от хранилища 1С.
//
// Параметры:
//  utils - экземпляр библиотеки Utils.groovy
//  server - сервер базы
//  infobase - имя подключаемой базы
//  platform - полный номер платформы 1с
//  admin1cUser - администратор базы
//  admin1cPassword - пароль администратора базы
//
def unbindRepo(utils, server, infobase, platform, admin1cUser, admin1cPassword) {

    admin1cUserLine = "";
    if (!admin1cUser.isEmpty()) {
        admin1cUserLine = "--db-user ${admin1cUser}"
    }
    admin1cPassLine = "";
    if (!admin1cPassword.isEmpty()) {
        admin1cPassLine = "--db-pwd ${admin1cPassword}"
    }

    retunStatus = utils.cmd("runner unbindrepo --ibconnection /S${server}\\${infobase} --v8version ${platform} ${admin1cUserLine} ${admin1cPassLine}")
    if (retunStatus != 0) {
        utils.raiseError("Возникла ошибка при отключении базы ${infobase} от хранилиза. Смотрите логи для подробной информации.")
    }
}

// Создает пользователя в хранилище 1С
//
// Параметры:
//
def createRepoUser(storage1cTCP, storage1User, storage1cPassword, admin1cUser, admin1cPassword) {
    utils = new Utils()
    //	ReadOnly — право на просмотр,
    //	LockObjects — право на захват объектов,
    // 	ManageConfigurationVersions — право на изменение состава версий,
    //	Administration — право на административные функции.
    retunStatus = utils.cmd("runner createrepouser ${storage1cTCP} ${storage1User} ${storage1cPassword} --storage-user ${admin1cUser} --storage-pwd ${admin1cPassword} --storage-role LockObjects")
    if (retunStatus != 0) {
        utils.raiseError("При создании пользователя ${storage1User} в хранилище: ${storage1cTCP} возникла ошибка. Для подробностей смотрите логи.")
    }
}

// Создает хранилище 1С
//
// Параметры:
//
def createStorage1c(projectServer, projectServerPlatform, templateBase, storage1cTCP, storage1User, storage1cPassword, admin1cUser, admin1cPassword) {
    utils = new Utils()
    retunStatus = utils.cmd("runner createrepo ${storage1cTCP} ${storage1User} ${storage1cPassword} --ibconnection /S${projectServer}\\${templateBase} --db-user ${admin1cUser}  --db-pwd ${admin1cPassword} --v8version ${projectServerPlatform}") 
    if (retunStatus != 0) {
        utils.raiseError("Возникла ошибка при создании хранилища 1С по  адресу: ${storage1cTCP}. Смотрите логи для подробной информации.")
    }
}

// Выводит в консоль информацию по базе
//
// Параметры:
//
//  server - сервер базы
//  infobase - имя подключаемой базы
//  platform - полный номер платформы 1с
//  admin1cUser - администратор базы
//  admin1cPassword - пароль администратора базы
//
def dbinfo(server, infobase, platform, admin1cUser = "", admin1cPassword = "") {
    def utils = new Utils()
    admin1cUserLine = "";
    if (!admin1cUser.isEmpty()) {
        admin1cUserLine = "--db-user ${admin1cUser}"
    }

    admin1cPassLine = "";
    if (!admin1cPassword.isEmpty()) {
        admin1cPassLine = "--db-pwd ${admin1cPassword}"
    }

    retunStatus = utils.cmd("runner dbinfo --ras ${server}:1545 --db ${infobase} --ibconnection /S${server}\\${infobase} --v8version ${platform} ${admin1cUserLine} ${admin1cPassLine}")
   if (retunStatus != 0) {
        utils.raiseError ("Ошибка при получении информации о базе чере RAS")
    }
}

// Пингует базу в режиме конфигуратор и проверяет доступ администратора для пользователя.
// Обратите внимание, что если в базе у админа пустой пароль, то пинг админа базы с заданным паролем всегда будет возвращать успех
//
// Параметры:
//
//  server - сервер базы
//  infobase - имя подключаемой базы
//  platform - полный номер платформы 1с
//  admin1cUser - администратор базы
//  admin1cPassword - пароль администратора базы
//
def testInfobaseConnectionRAS(server, infobase, platform, admin1cUser = "", admin1cPassword = "") {
    def utils = new Utils()
    admin1cUserLine = "";
    if (!admin1cUser.isEmpty()) {
        admin1cUserLine = "-admin1c \"${admin1cUser}\""
    }

    admin1cPassLine = "";
    if (!admin1cPassword.isEmpty()) {
        admin1cPassLine = "-passw1c \"${admin1cPassword}\""
    }

    retunStatus = utils.cmd("oscript one_script_tools/testConnectionInfobase.os -rac ${server} -platform ${platform} -base ${infobase} ${admin1cUserLine} ${admin1cPassLine}")
    if (retunStatus != 0) {
        return false
    }
    return true
}

// Обновляет базу в режиме конфигуратора. Аналог нажатия кнопки f7
//
// Параметры:
//
//  utils - инстантс библиотеки Utils
//  connString - строка соединения, например /Sdevadapter\template_adapter_adapter
//  platform - полный номер платформы 1с
//  admin1cUser - администратор базы
//  admin1cPassword - пароль администратора базы
//
def updateInfobase(utils, connString, admin1cUser, admin1cPassword, platform) {

    admin1cUserLine = "";
    if (!admin1cUser.isEmpty()) {
        admin1cUserLine = "--db-user ${admin1cUser}"
    }
    admin1cPassLine = "";
    if (!admin1cPassword.isEmpty()) {
        admin1cPassLine = "--db-pwd ${admin1cPassword}"
    }

    returnCode = utils.cmd("runner updatedb --ibconnection ${connString} ${admin1cUserLine} ${admin1cPassLine} --v8version ${platform}")
    if (returnCode != 0) {
        utils.raiseError("Обновление базы ${connString} в режиме конфигуратора завершилось с ошибкой. Для дополнительной информации смотрите логи")
    }
}

// Добавляет базу в список пользователя на сервере, переданному в параметре jenkinsAgent.
// Обратите внимание, что метод запрещенно выполняться палаллельно 
// из-за ошибок параллелизма при синхронном доступе к файлу v8Users.v8i. 
// Метод работает только для репозитория devops_sources
// Вызов метода запускает отдельный поток и шаг в дженкинсе.
//
// Параметры:
//
//  jenkinsAgent - нода дженкинса, на которой база будет добавлена в список
//  server - имя сервера базы
//  base - имя базы на сервере
//  user - доменное имя пользователя, для которого нужно добавить базу в список
//
def addTolistTask(jenkinsAgent, server, base, user) {
    utils = new Utils()
    if (utils.pingJenkinsAgent(jenkinsAgent)) {
        func = {
            node (jenkinsAgent) {
                timestamps {
                    
                    def projectHelpers = new ProjectHelpers()
                    def utils = new Utils()

                    checkout scm

                    projectHelpers.clearClientCacheFilter(server, base)
                    projectHelpers.addInfobaseToList(utils, server, base, user)
                }
            }
        }
        func.call()
    } else {
        echo "cannot ping server or node with label ${jenkinsAgent}"
    }
}

// Данный метод выполняет подготовительные шаги, он должен запускаться перед стартом каждой сборки.
//
// Параметры:
//
//
def beforeStartJob() {
    blockSheduledJob()
}

// Данный метод выполняет дополнительные шаги перед завершением каждой сборки.
//
// Параметры:
//
//
def beforeEndJob() {
    echo "empty"
}

// Блокирует расписание в тестовых пайплайнах, которые начинаются с "z_drafts". Рабочие пайплайны игнорируются
//
// Параметры:
//
//
def blockSheduledJob() {
    
    echo "Checking if the job shedule should be blocked"

    utils = new Utils()
    jenkinsIntegration = new JenkinsIntegration()
    
    jobName = env.JOB_NAME;
    if (jobName == null || !isDraftJob()) {
        echo "No need to block the job shedule"
        return;
    }
    echo "This is the test job. Need to block the shedule"

    configXml = jenkinsIntegration.execGet("config.xml")
    configXmlModified = configXml.replaceFirst("(?sim)<hudson\\.triggers\\.TimerTrigger>.*</triggers>", "<hudson\\.triggers\\.TimerTrigger/></triggers>");
    
    sheduleResetSucces = false
    if (configXml != configXmlModified) {
        try {
            echo "configXmlModified = ${configXmlModified}"
            jenkinsIntegration.execPost("config.xml", configXmlModified) // Для работы метода с русскими символами, при старте мастера и всех слейвов дженкинса нужно устанавливать кодировку  -Dfile.encoding=UTF-8
            sheduleResetSucces = true
            error "error" // Принудительно не даем пайплайну с расписанием выполниться
        } catch (error) {
            if (sheduleResetSucces) {
                utils.raiseError("Расписание тестовой сборки было сброшено успешно. Можно запустить сборку снова")
            } else {
                // неудачный POST запрос, возможно из-за русских символов в config-е
                utils.raiseError("Эта сборка не может иметь расписание, потому что она была помещена в каталог z_draft. Удалите расписание из pipeline конфига")
            }
        }
    } else {
        echo "Shedule for this job is empty"
    } 
}

// Загружает в базу конфигурацию из 1С хранилища. Базу желательно подключить к хранилищу под загружаемыйм пользователем,
//  т.к. это даст буст по скорости загрузки.
//
// Параметры:
//
//
def loadCfgFrom1CStorage(utils, storageTCP, storageUser, storagePwd, connString, admin1cUser, admin1cPassword, platform) {
    returnCode = utils.cmd("runner loadrepo --storage-name ${storageTCP} --storage-user ${storageUser} --storage-pwd ${storagePwd} --ibconnection ${connString} --db-user ${admin1cUser} --db-pwd ${admin1cPassword} --v8version ${platform}")
    if (returnCode != 0) {
         utils.raiseError("Загрузка конфигурации из 1С хранилища  ${storageTCP} завершилась с ошибкой. Для подробностей смотрите логи.")
    }
}

// Выгружает конфигурацию в cf файл
//
// Параметры:
//
//
def unloadCfg(utils, connString, cfgfileName, admin1cUser, admin1cPassword, platform) {
    returnCode = utils.cmd("runner unload ${cfgfileName} --ibconnection ${connString} --db-user ${admin1cUser} --db-pwd ${admin1cPassword} --v8version ${platform}")
    if (returnCode != 0) {
        utils.raiseError("""
    Выгрузка конфигурации в cf файл ${cfgfileName} завершилась с ошибкой. Возможные ошибки:
    1. Не найдена база в кластере ${connString}. Создайте ее через заявку.
    2. В базе не обнаружен пользователь ${admin1cUser} с паролем ${admin1cPassword}. Добавьте пользователя.
    3. База ${connString} открыта в режиме конфигуратора. Закройте конфигуратор.
    Для подробностей смотрите логи"""
        )
    }
}

// Загружает конфигурацию из cf файла. Работает только с cf файлами!
//
// Параметры:
//
//
def loadCfg(utils, connString, cfgfileName, admin1cUser = "", admin1cPassword = "", platform) {

    admin1cUserLine = "" 
    if (admin1cUser != null && !admin1cUser.isEmpty()) {
        admin1cUserLine = "--db-user ${admin1cUser}"
    }

    admin1cPasswordLine = "" 
    if (admin1cPassword != null && !admin1cPassword.isEmpty()) {
        admin1cPasswordLine = "--db-pwd ${admin1cPassword}"
    }

    returnCode = utils.cmd("runner load --src=${cfgfileName} --ibconnection ${connString} ${admin1cUserLine} ${admin1cPasswordLine} --v8version ${platform}")
    if (returnCode != 0) {
        utils.raiseError("Загрузка cf файла ${cfgfileName} в базу ${connString} завершилась с ошибкой. Для подробностей смотрите логи")
    }
}

// Загружает базу из dt файла.
//
// Параметры:
//
//
def loadDt(platform, server, base, dtpath, admin1cUser, admin1cPwd) {
    utils = new Utils()

    admin1cUserLine = "" 
    if (admin1cUser != null && !admin1cUser.isEmpty()) {
        admin1cUserLine = "-user ${admin1cUser}"
    }

    admin1cPasswordLine = "" 
    if (admin1cPwd != null && !admin1cPwd.isEmpty()) {
        admin1cPasswordLine = "-passw ${admin1cPwd}"
    }

    returnCode = utils.cmd("oscript one_script_tools/loadInfobaseFromDT.os -platform ${platform} -server ${server} -base ${base} -dtpath \"${dtpath}\" ${admin1cUserLine} ${admin1cPasswordLine}")
    if (returnCode != 0) {
        utils.raiseError("Загрузка dt файла ${dtpath} в базу ${server}/${base} завершилась с ошибкой. Для подробностей смотрите логи")
    }
}

// Запускает обработчики обновления в базах, за исключением отложенных. Обработчики запускаются даже если номер версии конфигурации не меняется
//
// Параметры:
//
//
def runUpdatingHandlers(utils, connString, admin1cUser, admin1cPassword, platform) {
   
    return // TODO обработчики обновление не получается выполнить в базах, в которых после обновления появляются модальные окошка. Восстановить после DEVOPS-618 

    echo "Executing updating handlers..."
    returnCode = utils.cmd("runner run --command \"ЗапуститьОбновлениеИнформационнойБазы;ЗавершитьРаботуСистемы;\" --execute \$runnerRoot/epf/ЗакрытьПредприятие.epf --ibconnection ${connString} --db-user ${admin1cUser} --db-pwd ${admin1cPassword} --v8version ${platform}")
    if (returnCode != 0) {
        utils.raiseError(" Выполнение обработчиков обновления для базы ${connString} в режиме Предприятия завершилось с ошибкой. Для подробностей смотрите логи")
    }
}

// Выполнение сравнение объединение конфигурации через файл mergeSettings
//
// Параметры:
//
//
def mergeCfg(utils, connString, cfgfileName, mergeSettings, admin1cUser, admin1cPassword, platform) {
    returnCode = utils.cmd("runner merge --src=${cfgfileName} --merge-settings=${mergeSettings} --force --ibconnection ${connString} --db-user ${admin1cUser} --db-pwd ${admin1cPassword} --v8version ${platform}")
    if (returnCode != 0) {
        utils.raiseError("Сравнение объединение с файлом cf ${cfgfileName}с merge settings ${mergeSettings} для базы ${connString} завершилось с ошибкой. Для подробностей смотрите логи")
    }
}

// Снимает  поддержку в конфигурации. За один вызов скрипта снимается только одна поддержка.
//
// Параметры:
//
//
def disableSupportCfg(utils, platform, server, userBase, admin1cUser, admin1cPassword) {
    returnCode = utils.cmd("oscript one_script_tools/disableCfgSupport.os -platform ${platform} -server ${server} -base ${userBase} -user ${admin1cUser} -passw ${admin1cPassword}")
    if (returnCode != 0) {
        utils.raiseError("Снятие поддежки для базы ${userBase} на сервере ${server} завершилось с ошибкой. Для подробностей смотрите логи")
    }
}

// Экспортирует исходники из формата EDT в формат 1С. Для срипта требуется утилита ring в поставке платформы 1С
//
// Параметры:
//
//
def exportEDTSources(utils, sourcesPath, projectDir, baseId) {
    returnCode = utils.cmd("ring edt workspace export --workspace-location ${env.WORKSPACE}/build/workspace/${baseId} --configuration-files ${sourcesPath} --project ${projectDir}/${projectKey}")
    if (returnCode != 0) {
        utils.raiseError("Экспорт исходников из EDT ${sourcesPath} завершился с ошибкой. Для подробностей смотрите логи")
    }
}

// Компилирует исходники в файл конфигурации cf
//
// Параметры:
//
//
def compileSourcesToCfg(utils, sourcesPath, cfgPath, connString, platform) {
    returnCode = utils.cmd("runner compile --src ${sourcesPath} --ibconnection ${connString} --out ${cfgPath} --v8version ${platform}")
    if (returnCode != 0) {
        utils.raiseError("Компиляция выгруженных исходников ${sourcesPath} в файл  ${cfgPath} завершилась с ошибкой. Для подробностей смотрите логи")
    }
}

// Обновляет конфигурацию базы из файла поставки. Для успешного обновления базы в файле поставке не должно 
// быть удаляемых объектов и измененых свойств корневого узла конфигурации. Также все объекты, кроме корневого узла, должны быть на замке.
//
// Параметры:
//
//
def updateFromDistribPackage(utils, platform, server, userBase, admin1cUser, admin1cPassword, distribFolder) {

    serverpath = "" 
    if (server != null) {
        serverpath = "-server ${server}"
    }

    returnCode = utils.cmd("oscript one_script_tools/updateFromDistribPackage.os ${serverpath} -platform ${platform} -base ${userBase} -user ${admin1cUser} -passw ${admin1cPassword} -distribfolder ${distribFolder}")
    if (returnCode != 0) {
        utils.raiseError("Обновление конфигурации из файла поставки ${distribFolder} для базы ${server}\\${userBase} завершилось с ошибкой. Для подробностей смотрите логи.")
    }
}

// Создает файл поставки для конфигурации
//
// Параметры:
//
//
def createDistribPackage(utils, platform, server, userBase, admin1cUser, admin1cPassword, distribFolder) {

    serverpath = "" 
    if (server != null) {
        serverpath = "-server ${server}"
    }

    admin1cUserPath = ""
    if (admin1cUser != null) {
        admin1cUserPath = "-user ${admin1cUser}"
    }

    admin1cPasswordPath = ""
    if (admin1cPassword != null) {
        admin1cPasswordPath = "-passw ${admin1cPassword}"
    }

    returnCode = utils.cmd("oscript one_script_tools/createDistribPackage.os ${serverpath} -platform ${platform} -base ${userBase} ${admin1cUserPath} ${admin1cPasswordPath} -distribfolder ${distribFolder}")
    if (returnCode != 0) {
        utils.raiseError("Возникла ошибка при создании файла поставки для базы ${server}\\${userBase}. Для подробностей смотрите логи.")
    }
}

def isDraftJob() {
    jobName = env.JOB_NAME;
    if (jobName == null) {
        return false
    }
    return jobName.trim().toLowerCase().startsWith("z_drafts")
}

// Загружает cfe файл расширения в базу 1С
//
// Параметры:
//
def loadExtensionFromFile(server, platform, infobase, admin1cUser, admin1cPwd, extensionFile, extension) {
    utils = new Utils()
    returnCode = utils.cmd("oscript ${env.WORKSPACE}/one_script_tools/loadExtensionFromFile.os -server ${server} -platform ${platform} -base ${infobase} -user ${admin1cUser} -passw ${admin1cPwd} -extensionfile ${extensionFile} -extension ${extension}")
    if (returnCode != 0) {
        utils.raiseError("При загрузке расширения конфигурации ${extensionFile} в базу ${server}/${infobase} возникла ошибка. Для подробностей смотрите логи")
    }
}

// Загружает файл расширения в базу 1С из хранилища
//
// Параметры:
//
def loadExtensionFromStorage(server, platform, infobase, admin1cUser, admin1cPwd, repostorage, repouser, repopwd, extension) {
    utils = new Utils()
    returnCode = utils.cmd("oscript ${env.WORKSPACE}/one_script_tools/loadExtensionFromStorage.os -server ${server} -platform ${platform} -base ${infobase} -user ${admin1cUser} -passw ${admin1cPwd} -repostorage ${repostorage} -repouser ${repouser} -repopwd ${repopwd} -extension ${extension}")
    if (returnCode != 0) {
        utils.raiseError("При загрузке расширения конфигурации ${extension} в базу ${server}/${infobase} из хранилища ${repostorage} возникла ошибка. Для подробностей смотрите логи")
    }
}

// Выгружает расширение в файл cfe
//
// Параметры:
//
def unloadExtensionToFile(server, platform, infobase, admin1cUser, admin1cPwd, extensionFile, extension) {
    utils = new Utils()
    returnCode = utils.cmd("oscript ${env.WORKSPACE}/one_script_tools/unloadExtensionToFile.os -server ${server} -platform ${platform} -base ${infobase} -user ${admin1cUser} -passw ${admin1cPwd} -extensionfile ${extensionFile} -extension ${extension}")
    if (returnCode != 0) {
        utils.raiseError("При загрузке расширения конфигурации ${extensionFile} в базу ${server}/${infobase} возникла ошибка. Для подробностей смотрите логи")
    }
}

// Обновляет расширение в режиме конфигуратора
//
// Параметры:
//
def updateExtension(server, platform, infobase, admin1cUser, admin1cPwd, extension) {
    utils = new Utils()
    returnCode = utils.cmd("oscript ${env.WORKSPACE}/one_script_tools/updateExtension.os -server ${server} -platform ${platform} -base ${infobase} -user ${admin1cUser} -passw ${admin1cPwd} -extension ${extension}")
    if (returnCode != 0) {
        utils.raiseError("При обновлении расширения конфигурации ${extension} в базе ${server}/${infobase} в режиме конфигуратора возникла ошибка. Для подробностей смотрите логи")
    }
}

// Отключает расширение от хранилища
//
// Параметры:
//
def unbindExtensionRepo(server, platform, infobase, admin1cUser, admin1cPwd, extension) {
    utils = new Utils()
    returnCode = utils.cmd("oscript ${env.WORKSPACE}/one_script_tools/unbindExtensionRepo.os -server ${server} -platform ${platform} -base ${infobase} -user ${admin1cUser} -passw ${admin1cPwd} -extension ${extension}")
    if (returnCode != 0) {
        utils.raiseError("При отключении расширения конфигурации ${extension} в базе ${server}/${infobase} возникла ошибка. Для подробностей смотрите логи")
    }
}

// Удаляет файл расширения из базы 1С
//
// Параметры:
//
def deleteStorageExtension(server, platform, infobase, admin1cUser, admin1cPwd, extension) {
    utils = new Utils()
    returnCode = utils.cmd("oscript ${env.WORKSPACE}/one_script_tools/deleteExtension.os -server ${server} -platform ${platform} -base ${infobase} -user ${admin1cUser} -passw ${admin1cPwd} -extension ${extension}")
    if (returnCode != 0) {
        echo "При удалении расширения конфигурации ${extension} в базу ${server}/${infobase} возникла ошибка. Возможно указанное расширение отсутствует в базе"
    }
}

// Создает хранилище 1с для расширения. Расширение должно быть предварительно загружено в базу
//
// Параметры:
//
def createStorage1cExt(server, platform, infobase, admin1cUser, admin1cPwd, repoFolder, repoUser, repoPwd, extension) {
    utils = new Utils()
    returnCode = utils.cmd("oscript ${env.WORKSPACE}/one_script_tools/createExtensionRepo.os -server ${server} -platform ${platform} -base ${infobase} -user ${admin1cUser} -passw ${admin1cPwd} -repofolder ${repoFolder} -repouser ${repoUser} -repopwd ${repoPwd} -extension ${extension}")
    if (returnCode != 0) {
        utils.raiseError("При созданиии хранилища для расширения 1С по адресу ${repoFolder} для базы ${server}/${infobase} возникла ошибка. Для подробностей смотрите логи")
    }
}

// Создает пользователя в хранилище 1с для расширения.
//
// Параметры:
//
def createStorage1cExtUser(server, platform, infobase, admin1cUser, admin1cPwd, repoFolder, repoUser, repoPwd, extension, newUser, newPwd) {
    utils = new Utils()
    returnCode = utils.cmd("oscript ${env.WORKSPACE}/one_script_tools/addUserToExtensionStorage.os -server ${server} -platform ${platform} -base ${infobase} -user ${admin1cUser} -passw ${admin1cPwd} -repofolder ${repoFolder} -repouser ${repoUser} -repopwd ${repoPwd} -extension ${extension} -newuser ${newUser} -newpwd ${newPwd}")
    if (returnCode != 0) {
        utils.raiseError("Добавление пользователя ${newUser} в хранилище 1с для расширения ${extension} по адресу ${repoFolder} завершилось с ошибкой. Для подробностей смотрите логи")
    }
}

def createJiraBugIfFailed(projectKey, issueKey, epic, summary, descr = null) {

    taskKey = ""

    jiraIntegration = new JIRAIntegration()
    jenkinsIntegration = new JenkinsIntegration()

    if (descr == null) {
        descr = jenkinsIntegration.getBuildResultMessage()
    }

    if (!isDraftJob() && currentBuild.result != null && !(currentBuild.result == "SUCCESS" || currentBuild.result == "ABORTED")) {
        taskKey = jiraIntegration.createBug(jenkinsIntegration.buildURL(), projectKey, issueKey, epic, summary, descr)
        echo "Build completed with status = ${currentBuild.result}. So jira task with key ${taskKey} was created"
    }

    return taskKey
}

// Копирует пользователя 1С, включая права. Если такой пользователь существует, то он обновляется. Источником для копирования является администратор
//
// Параметры:
//  conString1c - строка соединения вида /Sdevadapter\work
//  admin1cUser - Необязательный. Имя админа 1С базы, который будет использован в качесте источника для копирования
//  admin1cPwd -  Необязательный. пароль админа 1С базы
//  newUserName - имя нового юзера 
//  newUserPwd - пароль нового юзера
//
def copy1cInfobaseUser(connString, admin1cUser = "", admin1cPwd = "", newUserName, newUserPwd) {
    utils = new Utils()
    
    admin1cUserLine = "";
    if (!admin1cUser.isEmpty()) {
        admin1cUserLine = "--db-user \"${admin1cUser}\""
    }

    admin1cPassLine = "";
    if (!admin1cPwd.isEmpty()) {
        admin1cPassLine = "--db-pwd \"${admin1cPwd}\""
    }

    utils.cmd("runner run --execute ${env.WORKSPACE}/one_script_tools/copyUser.epf --command \"-newusername ${newUserName} -newuserpwd ${newUserPwd}\" ${admin1cUserLine} ${admin1cPassLine} --ibconnection=${connString}")
}

// Публикует все веб-сервисы для базы. Для выполнения скрипта в каталоге defaultvrdfile долэен уже существовать данный файл
//
// Параметры:
//  conString1c - строка соединения вида /Sdevadapter\work
//  admin1cUser - Необязательный. Имя админа 1С базы, который будет использован в качесте источника для копирования
//  admin1cPwd -  Необязательный. пароль админа 1С базы
//  defaultvrdfile - путь к файлу публикации default.vrd
//
def publishWebServices(connString, admin1cUser = "", admin1cPwd = "", defaultvrdfile) {
    utils = new Utils()
    
    admin1cUserLine = "";
    if (!admin1cUser.isEmpty()) {
        admin1cUserLine = "--db-user \"${admin1cUser}\""
    }

    admin1cPassLine = "";
    if (!admin1cPwd.isEmpty()) {
        admin1cPassLine = "--db-pwd \"${admin1cPwd}\""
    }

    returnCode = utils.cmd("runner run --execute ${env.WORKSPACE}/one_script_tools/publishWebServices.epf --command \"-defaultvrdfile ${defaultvrdfile}\" ${admin1cUserLine} ${admin1cPassLine} --ibconnection=${connString}")
    if (returnCode != 0) {
        utils.raiseError("Публикация веб-сервисов для ${infobase} по адресу ${defaultvrdfile} завершилась с ошибкой. Для подробностей смотрите логи")
    }
}

// Публикует веб-клиент на веб-сервере. Требуются права администратора. Работает только с IIS
//
// Параметры:
//  publicname - Имя виртуального каталога
//  publicpath - Имя физического каталога, куда будет отображен виртуальный каталог веб-сервера.  Если каталог не существует, то он создается
//
def createWebPublication(server, platform, infobase, admin1cUser, admin1cPwd, publicname, publicpath) {
    utils = new Utils()
    returnCode = utils.cmd("oscript ${env.WORKSPACE}/one_script_tools/createWebPublication.os -server ${server} -platform ${platform} -base ${infobase} -user ${admin1cUser} -passw ${admin1cPwd} -publicname ${publicname} -publicpath ${publicpath}")
    if (returnCode != 0) {
        utils.raiseError("Публикация веб-клиента для ${infobase} по адресу ${publicpath} завершилась с ошибкой. Для подробностей смотрите логи")
    }
}

// Првоеряет наличие пользователя в хранилище конфигурации или в хранилище расширения. Требует gitsync.
//
// Параметры:
//  storage1cfile - путь к файлу хранилища 1с
//  user - имя пользователя хранилища 1с
//
// Возвращаемое значение
//
// boolean - если true, пользователь найден в хранилище
//
def checkStorage1cUser(storage1cfile, user) {
    utils = new Utils()
    returnCode = utils.cmd("oscript ${env.WORKSPACE}/one_script_tools/checkStorage1cUser.os -storage1cfile \"${storage1cfile}\" -user ${user}")
    if (returnCode != 0) {
        return false
    }
    return true
}

return this;