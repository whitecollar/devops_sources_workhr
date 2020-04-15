package io.bit
import io.bit.Utils

// Инициализирует пустой репозиторий
//
// Параметры:
//  workspace - рабочий каталог для команды git-а. По умолчанию текущий каталог проекта
//
def init(workspace = "") {
    utils = new Utils()
    retunStatus = utils.cmd("${getWorkspaceLine(workspace)} git init")
    if (retunStatus != 0) {
        utils.raiseError("Error when init empty git repo. See logs above for detailed information.")
    }
}

// Добавляет отслеживание удаленного репозитория
//
// Параметры:
//  workspace - рабочий каталог для команды git-а. По умолчанию текущий каталог проекта
//  url - адрес репозитория для clone операции
//
def addRemote(url, workspace = "") {
    utils = new Utils()
    retunStatus = utils.cmd("${getWorkspaceLine(workspace)} git remote add origin ${url}")
    if (retunStatus != 0) {
        utils.raiseError("Error when adding git remote ${url}. See logs above for detailed information.")
    }
}

// Изменяет путь к отслеживаемому удаленному репозиторию для локального. 
// Нужно, чтобы запушить текущий репозиторий по другому адресу
//
// Параметры:
//  workspace - рабочий каталог для команды git-а. По умолчанию текущий каталог проекта
//  url - адрес репозитория для clone операции
//
def changeRemote(url, workspace = "") {
    utils = new Utils()
    retunStatus = utils.cmd("${getWorkspaceLine(workspace)} git remote set-url origin ${url}")
    if (retunStatus != 0) {
        utils.raiseError("Error when changing git remote ${url}. See logs above for detailed information.")
    }
}

// Добавляет отслеживание удаленного репозитория
//
// Параметры:
//  workspace - рабочий каталог для команды git-а. По умолчанию текущий каталог проекта
//  url - адрес репозитория для clone операции
//  nocheckout - в репозиторий не будут склонированы файлы, а только их индекс (аналог fetch)
//
def clone(url, workspace = "", nocheckout = false) {
    utils = new Utils()
    nocheckoutLine = nocheckout == false ? "" : "--no-checkout"
    retunStatus = utils.cmd("${getWorkspaceLine(workspace)} git clone ${nocheckoutLine} ${url}")
    if (retunStatus != 0) {
        utils.raiseError("Error when adding git remote ${url}. See logs above for detailed information.")
    }
}

// Добавляет новый сабмодуль
//
// Параметры:
//  url - адрес репозитория сабмодуля
//  relativePath - относительный путь сабмодуля в каталоге основного репо
//  workspace - рабочий каталог для команды git-а. По умолчанию текущий каталог проекта
//
def addSubmodule(url, relativePath = "", workspace = "") {
    utils = new Utils()
    retunStatus = utils.cmd("${getWorkspaceLine(workspace)} git submodule add ${url} ${relativePath}")
    if (retunStatus != 0) {
        utils.raiseError("Error when adding submodule ${url}. See logs above for detailed information.")
    }
}

// Убирает все файлы из индекса git-а
//
// Параметры:
//  workspace - рабочий каталог для команды git-а. По умолчанию текущий каталог проекта
//
def reset(workspace = "") {
    utils = new Utils()
    retunStatus = utils.cmd("${getWorkspaceLine(workspace)} git reset")
    if (retunStatus != 0) {
        utils.raiseError("Error when fetch repo. See logs above for detailed information.")
    }
}

// Загружает индекс всех изменений в локальный репозиторий без загрузки самих файлов из удаленного репозитория
//
// Параметры:
//  workspace - рабочий каталог для команды git-а. По умолчанию текущий каталог проекта
//
def fetch(workspace = "") {
    utils = new Utils()
    retunStatus = utils.cmd("${getWorkspaceLine(workspace)} git fetch")
    if (retunStatus != 0) {
        utils.raiseError("Error when fetch repo. See logs above for detailed information.")
    }
}

// Загружает отдельный файл из удаленного репозитория
//
// Параметры:
//  path - относительный или абсолютный путь к файлу
//  workspace - рабочий каталог для команды git-а. По умолчанию текущий каталог проекта
//
def checkoutOneFile(path, workspace = "") {
    utils = new Utils()
    retunStatus = utils.cmd("${getWorkspaceLine(workspace)} git checkout origin/master ${path}")
    if (retunStatus != 0) {
        utils.raiseError("Error when checkout one file  ${path}. See logs above for detailed information.")
    }
}

// Загружает отдельный файл из удаленного репозитория
//
// Параметры:
//  path - относительный или абсолютный путь к файлу
//  workspace - рабочий каталог для команды git-а. По умолчанию текущий каталог проекта
//
def add(path, workspace = "") {
    utils = new Utils()
    retunStatus = utils.cmd("${getWorkspaceLine(workspace)} git add ${path}")
    if (retunStatus != 0) {
        utils.raiseError("Error when adding file one file ${path}. See logs above for detailed information.")
    }
}

// Делает коммит в локальный репозиторий. Если нечего коммитить, то сработает исключение.
//
// Параметры:
//  message - сообщение коммита
//  workspace - рабочий каталог для команды git-а. По умолчанию текущий каталог проекта
//
def commit(message, workspace = "") {
    utils = new Utils()
    retunStatus = utils.cmd("${getWorkspaceLine(workspace)} git commit -m \"${message}\"")
    if (retunStatus != 0) { 
        utils.raiseError("Error when performing git commit with message ${message}. See logs above for detailed information.")
    }
}

// Инициализирует пустой репозиторий
//
// Параметры:
//  branch - ветка репозитория, по умолчанию master
//  workspace - рабочий каталог для команды git-а. По умолчанию текущий каталог проекта
//
def push(branch = "master", workspace = "") {
    utils = new Utils()
    retunStatus = utils.cmd("${getWorkspaceLine(workspace)} git push -u origin ${branch}")
    if (retunStatus != 0) {
        utils.raiseError("Error when pushing to remote git repo. See logs above for detailed information.")
    }
}

// Проверяет доступ к репозиторию по ссылке. 
//
// Параметры:
//  url - magent-ссылка, например https://jenkins@code.bit-erp.ru/scm/mdmcorp/mdmcorp_upp_sources1c.git 
//  workspace - рабочий каталог для команды git-а. По умолчанию текущий каталог проекта
//
// Возвращаемое значение
//  boolean - true если репозиторий существует и к нему есть доступ на чтение
//
def validateRepoUrl(url, workspace = "") {
    utils = new Utils()
    retunStatus = utils.cmd("${getWorkspaceLine(workspace)} git ls-remote  ${url}")
    if (retunStatus != 0) {
       return false
    }
    return true
}

// Проверяет, наличие файлов в индексе. Необходимо вызывать перед коммитом. 
//
// Параметры:
//  workspace - рабочий каталог для команды git-а. По умолчанию текущий каталог проекта
//
// Возвращаемое значение
//  boolean - true, если есть файлы для коммита.
//
def hasStagedFiles(workspace = "") {
    utils = new Utils()
    output = utils.cmdOut("${getWorkspaceLine(workspace)} git diff --name-only --cached").trim().split("\n")
    for (def line : output) {
        echo "hasStagedFiles output: ${line}" 
    }
    return output.length > 1
}

// Возвращает почту пользователя, совершившего последний коммит в гите
//
// Возвращаемое значение
//  String - Представление пользователя для slack вида @Username
//
def getCommitterEmail(workspace = "") {
    utils = new Utils()
    outLines = utils.cmdOut("${getWorkspaceLine(workspace)} git --no-pager show -s --format=\"%%ae\"").trim().split("\n")
    return outLines[1]
}

// СЛУЖЕБНЫЕ МЕТОДЫ

def getWorkspaceLine(workspace = "") {
    utils = new Utils()
    return utils.getWorkspaceLine(workspace)
}
// Выполняет клонирование репозитория со всеми сабмодулями
//
// Параметры:
//  repo - url к репозиторию, включая @ - имя пользователя авторизации
//  credentialsId - SHH-key bitbucket-а или имя пользователя при обычной авторизации
//  targetDir - каталог, куда клонировать репозиторий
//  branch - имя ветки, которую клонировать
//
def checkoutSCM(repo, targetDir, branch) {
    checkout changelog: false,
    poll: false,
    scm: [$class: 'GitSCM',
        branches: [[name: branch]],
        doGenerateSubmoduleConfigurations: false,
        submoduleCfg: [],
        userRemoteConfigs: [[credentialsId: "bitbuket_user", url: repo]],
        extensions: [
            [$class: 'CleanBeforeCheckout'],
            [$class: 'RelativeTargetDirectory',
            relativeTargetDir: targetDir],
            [$class: 'SubmoduleOption',
            disableSubmodules: false,
            parentCredentials: true,
            recursiveSubmodules: true,
            trackingSubmodules: true,
            reference: '',
            trackingSubmodules: false]]
        ]
}