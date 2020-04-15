package io.bit
import io.bit.BITConvJava


// Выполняется GET запрос к дженкинс REST API
//
// Параметры:
//  url - относительный адрес к методу, не включая адрес сервера.
//
// Возвращаемое значение
//  String - Ответ запроса
//
def execGet(url, logOutput = false) {

    bitConvJava = new BITConvJava()

    jobUrl = jobName()

    response = httpRequest  consoleLogResponseBody: logOutput,  
        url: "${bitConvJava.getServerJenkins()}/job/${jobUrl}/${url}",
        customHeaders:[[name:'Authorization', value: jenkinsCred()]] // jenkins cred

    return response.content
}

// Получает содержание файла BuildResult.txt
//
// Возвращаемое значение
//  String - Содержание 
//
def getBuildResultMessage() {
    if (fileExists("BuildResultMessage.txt")) {
        fileText = readFile file: "BuildResultMessage.txt", encoding: "UTF-8"
        return fileText
    }
    return ""
}

// Получает содержание файла BuildResult.txt
//
// Возвращаемое значение
//  String - Содержание 
//
def getBuildResultMessageOBSOLETE() {

    bitConvJava = new BITConvJava()
    def jenkinsIntegration = new JenkinsIntegration()

    jobUrl = jobName()

    response = httpRequest url: "${jenkinsIntegration.buildURL()}artifact/BuildResultMessage.txt",
        validResponseCodes: '200:600',
        customHeaders:[[name:'Authorization', value: jenkinsIntegration.jenkinsCred()]] // jenkins cred

    if(response.status != 200) {
        return ""
    }

    return response.content
}

// Выполняется POST запрос к дженкинс REST API
//
// Параметры:
//  url - относительный адрес к методу, не включая адрес сервера.
//
// Возвращаемое значение
//  String - Ответ запроса
//
def execPost(url, body, logOutput = false) {
    bitConvJava = new BITConvJava()

    jobUrl = jobName()

    httpRequest  consoleLogResponseBody: logOutput,  
        url: "${bitConvJava.getServerJenkins()}/job/${jobUrl}/${url}",
        contentType: 'TEXT_PLAIN',
        httpMode: 'POST', 
        requestBody: body,
        customHeaders:[[name:'Authorization', value: jenkinsCred()]] // jenkins cred
}


// Возвращает имя для текущеей сборки подготовленную для REST запроса. Внимание, метод  можно использовать только внутри pipeline script блока
//
// Возвращаемое значение
//  String - подготовленный url.
//
def jobName() {
    return "${env.JOB_NAME}".replaceAll("/", "/job/")
}

def jobBaseName() {
    return env.JOB_BASE_NAME
}

// Возвращает логин пользователя из имени сборки. Сборка должна быть подготовлена по шаблону
// z_drafts/ЛОГИН_ПОЛЬЗОВАТЕЛЯ/infrastructure/blockJobs/main
//
// Возвращаемое значение
//  String - логин пользователя, например rkudakov
//
def jobNameUser() {
    projectHelpers = new ProjectHelpers()
    if (!projectHelpers.isDraftJob())
        return ""
    return env.JOB_NAME.split("/")[1]
}

// Возвращает полный URL для текущей сборки подготовленную для REST запроса. Внимание, метод  можно использовать только внутри pipeline script блока
//
// Возвращаемое значение
//  String - подготовленный url.
//
def buildURL() {
    return env.BUILD_URL
}

def jenkinsCred() {
    credsId = "bitbuket_user"

    authHeader = ''
    def cred = com.cloudbees.plugins.credentials.CredentialsProvider.lookupCredentials(
        com.cloudbees.plugins.credentials.impl.BaseStandardCredentials.class,
        jenkins.model.Jenkins.instance).findAll {it.id == credsId}[0]

    if (cred.class == com.cloudbees.plugins.credentials.impl.UsernamePasswordCredentialsImpl.class) {
        authHeader = 'Basic ' + "${cred.getUsername()}:${cred.getPassword()}".getBytes('UTF-8').encodeBase64().toString()
    } else if (cred.class == org.jenkinsci.plugins.plaincredentials.impl.StringCredentialsImpl.class) {
        authHeader = 'Bearer ' + cred.getSecret()
    }
    return authHeader
}
// Возвращает root директорию ноды дженкинса
//
// Параметры:
//
//  label - метка ноды дженкинса
//
def getRootPathNode(label) {
    Jenkins.instance.getLabel(label).getNodes()[0].getRootPath()
}

// Удаляет файлы и каталоги. Метод работает с рекурсией
//
// Параметры:
//
//  label - метка ноды дженкинса
//  path - путь относительно root директории ноды дженкинса
//  filter - фильтр по которому происходит удаление
//
def deleteWorkspaceFile(label, path, filter) {
    def fileUtils = new FileUtils()
    def rootPathNode = getRootPathNode(label)
    fileUtils.deleteFileFilter(new hudson.FilePath(rootPathNode, path), filter)
}