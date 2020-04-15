package io.bit


// Удаляет произвольный файл или каталог. Если файл не найден, то ошибка не вызывается.
//
// Параметры:
//  filename - путь к файлу или каталогу
//
def deleteFile(filename) {
    utils = new Utils()
    echo "Deleting file ${filename}..."
    returnCode = utils.cmd("oscript ${env.WORKSPACE}/one_script_tools/deleteFile.os -file ${filename}")
    if (returnCode != 0) {
        utils.raiseError("Возникла ошибка при удалении файла: ${filename}")
    }
}

// Записывает произвольную строку в файл. Записываются только уникальные строки
//
// Параметры:
//  file - путь к файлу
//  line - строка текста
//  searchkey - ключ, по которому искать существующую строку, чтобы ее обновить. Если не задан, то ищется по параметру line
//
def writeLineToFile(file, line, searchkey = null) {
    utils = new Utils()
    searchkeyPar = searchkey
    if (searchkey == null) {
        searchkeyPar = line
    }
    retunStatus = utils.cmd("oscript one_script_tools/writeLineToFile.os -file ${file} -line \"${line}\" -searchkey \"${searchkeyPar}\"")
    if (retunStatus != 0) {
        utils.raiseError("Возникла ошибка при записи строки в файл ${file}. Для подробностей смотрите логи")
    }
}

// Копирует файл в файловой системе ноды дженкинса.
//
// Параметры:
//  sourceFile - файл источник
//  destFile - файл приемник
//
def copyFile(sourceFile, destFile) {
    utils = new Utils()
    utils.cmd("oscript one_script_tools/copyFile.os -sourcefile ${sourceFile} -destfile ${destFile}")
}

// Возвращает список файлов и каталогов, находящихся внутри каталога. Метод работает без рекурсии
//
// Параметры:
//
//  path - каталог, в котором нужно искать файлы
//
@NonCPS
def getAllFiles(path) {
    
    if (env['NODE_NAME'].equals("master")) {
        File localPath = new File(path)
        rootPath = new hudson.FilePath(localPath);
    } else {
        rootPath = new hudson.FilePath(Jenkins.getInstance().getComputer(env['NODE_NAME']).getChannel(), path);
    }
    
    def list = []
    for (subPath in rootPath.list()) {
        list.add(subPath)
    }
    return list
}

// Удаляет файлы и каталоги. Метод работает с рекурсией
//
// Параметры:
//
//  path - тип FilePath, абсолютный путь к директории, в которой необходимо удалить файл
//  filter - фильтр по которому происходит удаление
//
def deleteFileFilter(path, filter) {
    for (def files : path.list()){
        deleteFileFilter(files, filter)
        if (files.toString().contains(filter)){
            files.delete()
        }
    }
}