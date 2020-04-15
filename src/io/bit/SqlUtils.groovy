package io.bit;

// Проверяет соединение к БД и наличие базы
//
// Параметры:
//  dbServer - сервер БД
//  infobase - имя базы на сервере БД
//
def checkDb(dbServer, infobase) {
    utils = new Utils()
    returnCode = utils.cmd("sqlcmd -S ${dbServer} -E -i \"${env.WORKSPACE}/copy_etalon/error.sql\" -b -v restoreddb =${infobase}");
    if (returnCode != 0) {
        utils.raiseError("Возникла ошибка при при проверке соединения к sql базе ${dbServer}\\${infobase}. Для подробностей смотрите логи")
    }
}

// Создает бекап базы по пути указанному в параметре backupPath
//
// Параметры:
//  dbServer - сервер БД
//  infobase - имя базы на сервере БД
//  backupPath - каталог бекапов
//
def backupDb(dbServer, infobase, backupPath) {
    utils = new Utils()
    returnCode = utils.cmd("sqlcmd -S ${dbServer} -E -i \"${env.WORKSPACE}/copy_etalon/backup.sql\" -b -v backupdb =${infobase} -v bakfile =${backupPath}")
    if (returnCode != 0) {
        utils.raiseError("Возникла ошибки при создании бекапа sql базы ${dbServer}\\${infobase}. Для подробностей смотрите логи")
    }
}

// Создает пустую базу на сервере БД
//
// Параметры:
//  dbServer - сервер БД
//  infobase - имя базы на сервере БД
//  backupPath - каталог бекапов
//
def createEmptyDb(dbServer, infobase) {
    utils = new Utils()
    returnCode = utils.cmd("sqlcmd -S ${dbServer} -E -i \"${env.WORKSPACE}/copy_etalon/error_create.sql\" -b -v restoreddb =${infobase}")
    if (returnCode != 0) {
        utils.raiseError("Возникла ошибка при создании пустой sql базы на  ${dbServer}\\${infobase}. Для подробностей смотрите логи")
    }
}

// Восстанавливает базу из бекапа
//
// Параметры:
//  dbServer - сервер БД
//  infobase - имя базы на сервере БД
//  backupPath - каталог бекапов
//
def restoreDb(dbServer, infobase, backupPath) {
    utils = new Utils()
    returnCode = utils.cmd("sqlcmd -S ${dbServer} -E -i \"${env.WORKSPACE}/copy_etalon/restore.sql\" -b -v restoreddb =${infobase} -v bakfile =${backupPath}")
    if (returnCode != 0) {
         utils.raiseError("Возникла ошибка при восстановлении базы из sql бекапа ${dbServer}\\${infobase}. Для подробностей смотрите логи")
    } 
}

// Копирует базу через sql из источника в приемник
//
// Параметры:
//  serverSql - сервер БД
//  ibSource - имя базы источника на сервере БД
//  ibDestionation - имя базы приемника на сервере БД
//  backupFolder - каталог бекапов
//
def copyBase(serverSql, ibSource, ibDestionation, backupFolder) {
    utils = new Utils()
    backupPath = "${backupFolder}/${ibSource}_${utils.currentDateStamp()}.bak"
    backupDb(serverSql, ibSource, backupPath)
    createEmptyDb(serverSql, ibDestionation)
    restoreDb(serverSql, ibDestionation, backupPath)
}
