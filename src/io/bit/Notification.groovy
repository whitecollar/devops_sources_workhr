package io.bit
import groovy.json.JsonSlurper
import groovy.json.JsonSlurperClassic
import net.sf.json.JSONObject
import net.sf.json.JSONArray
import io.bit.SlackHJava
import io.bit.Utils

// Отправляет уведомление о результатах сборки в slack
//
// Параметры:
//  channel - имя канала в слеке
//  buildStatus - статус сборки. Допускается SUCCESS, UNSTABLE или FAILURE
//  allureJobUrls - absoluteUrl для сборки с отчетом allure
//  user - логин пользователя запустившего паплайн в дженкинсе
//  threadId - id родительского сообщения, если передано, то будет создан трэд
//
def sendSlackFinishBuild(channel, bddType, buildStatus, allureJobUrls, user, threadId = null, addInfo = "", issueKey = "") {

    projectHelpers = new ProjectHelpers()
    utils = new Utils()
    jIRAIntegration = new JIRAIntegration()
    
    if (projectHelpers.isDraftJob()) {
        return
    }
    
    userId = jIRAIntegration.getSlackIDByLogin(user) == null ? user : jIRAIntegration.getSlackIDByLogin(user)
    
    commentBodyArr = []
    suitesDescr = ""
    if (allureJobUrls != null) {
        // Итерация через arrow function, чтобы избежать ошибки сериализации на типе LinkedMap$Entry
        allureJobUrls.each { allureJobKV -> 
            allureJobUrl = allureJobKV.value
            allureJobName = allureJobKV.key
            stats = getAllureSummaryStatistic(allureJobUrl)
            def statsDescr = ''
            if (stats.failed > 0) {
                statsDescr = " (Упало тестов - ${stats.failed})"
                suites = getAllureSuitesList(allureJobUrl, 'failed')
                suitesDescr = "\nУпавшие тесты:"
                for (suite in suites) {
                    suitesDescr +="\n-${suite}"
                }
            }
            commentBodyArr.add("<${allureJobUrl}allure/|${allureJobName}${statsDescr}>")
        }
    }

    title = "Статус сборки - ${buildStatusDescr(buildStatus)}"
    String commentBody = "${title} ${commentBodyArr.join(", ")}" // НЕЛЬЗЯ УБИРАТЬ ПРИСВОЕНИЕ ТИПА, ИНАЧЕ ПРЕОБРАЗОВАНИЕ ТИПА К СТРОКЕ БУДЕТ НЕПРАВИЛЬНЫМ
    comment = new JSONObject()
    comment.put("text", commentBody)
    comment.put("title", title)
    comment.put("color", buildStatusColor(buildStatus))
    attachments = new JSONArray()
    attachments.add(comment)
    message = getTitleBuild(userId, bddType, issueKey)
    SlackHJava.updateMessage(channel, message, attachments.toString(), threadId)

    title = "<@${userId}> Сборка завершена"
    message = suitesDescr.isEmpty() ? "${title}\n${addInfo}" : "${title}${suitesDescr}\n${addInfo}"
    SlackHJava.sendMessage(channel, message, "", threadId)
}

// Отправляет уведомление о начале сборки в slack
//
// Параметры:
//  user - логин пользователя запустившего паплайн в дженкинсе
//  bddType - тип заявки SD
//  channel - имя канала в слеке
//
def sendSlackStartBuild(channel, bddType, user, issueKey = "") {

    projectHelpers = new ProjectHelpers()
    jIRAIntegration = new JIRAIntegration()
    
    if (projectHelpers.isDraftJob()) {
        return
    }
    
    attachments = new JSONArray()
    userId = jIRAIntegration.getSlackIDByLogin(user) == null ? user : jIRAIntegration.getSlackIDByLogin(user)
    message = getTitleBuild(userId, bddType, issueKey)
    return SlackHJava.sendMessage(channel, message, attachments.toString(), null, null).get("ts")
}
// Возвращает Заголовок имени сборки
//
// Параметры:
//  user - логин пользователя запустившего паплайн в дженкинсе
//  bddType - тип заявки SD
//  issueKey - номер заявки из jira

def getTitleBuild(def userId, def bddType, def issueKey){
    def titleBuild =  "<@${userId}> ${bddTypeDescr(bddType)}. <${env.BUILD_URL}|Запустил сборку>"
    if (!issueKey.isEmpty()) {
        titleBuild += ", <${BITConvJava.combineJiraIssueLink(issueKey)}|Заявка ${issueKey}>"
    }
    return titleBuild
}

// Возвращает имя пользователя, зарегистрированного в slack-е
//
// Параметры:
//  email - email пользователя
//  withDoggySymb - включить в резльтута символа для Slack-а вида @Username
//
// Возвращаемое значение
//  String - Представление пользователя
//
def getUserName(email, withDoggySymb = false) {
    def response = httpRequest 'https://slack.com/api/users.lookupByEmail?token=xoxp-91011106341-92270826068-353040826676-96a6e0cfd66609b0dcf3a04e4158e2cd&email='+email
    def json = new JsonSlurper().parseText(response.content)
    if (json.ok) {
        if (withDoggySymb) {
            return ("@" + json.user.name)
        } else {
            return json.user.name
        }
    } else {
        return email
    }
}

// Возвращает пользователя, запустившего пайплайн. Требует плагин  user build vars
//
// Возвращаемое значение
//  String - Имя фамилия пользователя
//
def getCurrentJobUser() {
    wrap([$class: 'BuildUser']) {
        def fullUser = env.BUILD_USER
        userParts = fullUser.split("\\s")
        return "${userParts[0]} ${userParts[1]}";
    }
}

// Возвращает email пользователя, запустившего пайплайн. Требует плагин  user build vars
//
// Возвращаемое значение
//  String - email пользователя
//
def getCurrentJobUserEmail() {
    wrap([$class: 'BuildUser']) {
        return env.BUILD_USER_EMAIL
    }
}

// Возвращает общую статистику по отчету Allure для сборки. 
//
// Параметры:
//  jobUrl - url к сборке с отчетом allure. Пример http://ci.bit-erp.ru/job/ci/job/steps/job/runTests/110
//
// Возвращаемое значение
//  Object - статистика. Включает свойства failed, total и др. Если статистики нет, то возвращается null
//
def getAllureSummaryStatistic(jobUrl) {
    response = httpRequest  consoleLogResponseBody: false,  
        url: "${jobUrl}/allure/widgets/summary.json",
        validResponseCodes: '200:404',
        customHeaders:[[name:'Authorization', value:"Basic amVua2luczpeMmtrUjkybVMpUlM="]] // jenkins cred

    if (response.status == 404) {
        return null;
    }
            
    allureSummary = new JsonSlurperClassic().parseText(response.content)
    return allureSummary.statistic
}

// Возвращает  имена тестов по отчету Allure для сборки. 
//
// Параметры:
//  jobUrl - url к сборке с отчетом allure. Пример http://ci.bit-erp.ru/job/ci/job/steps/job/runTests/110
//  status - отбор по статусу тестов. Допускаются: failed, passed, skipped
//
// Возвращаемое значение
//  Object - массив со списков тестов. Если тестов нет, то возвращается пустой массив
//
def getAllureSuitesList(jobUrl, status = null) {
    resultList = []
    response = httpRequest  consoleLogResponseBody: false,  
        url: "${jobUrl}/allure/data/suites.json",
        validResponseCodes: '200:404',
        customHeaders:[[name:'Authorization', value:"Basic amVua2luczpeMmtrUjkybVMpUlM="]] // jenkins cred

    if (response.status == 404) {
        return resultList;
    }
            
    allureDataJson = new JsonSlurperClassic().parseText(response.content)

    for (def suitJson : allureDataJson.children) {
        if (status != null && status != suitJson.status) {
            continue
        }
        resultList.add(suitJson.name)
    }
    return resultList
}

def buildStatusColor(buildStatus) {
    def color = [:]
    color.put('SUCCESS', '#00FF00')
    color.put('FAILURE', '#FF0000')
    color.put('UNSTABLE', '#FFFF00')
    color.put('ABORTED', '#646464')
    return color.containsKey(buildStatus) ? color.get(buildStatus) : '#ec3cf2'
}

def buildStatusDescr(buildStatus) {
    def result = [:]
    result.put('SUCCESS', 'успешно')
    result.put('FAILURE', 'были ошибки')
    result.put('UNSTABLE', 'нестабильно')
    result.put('ABORTED', 'отменена')
    return result.containsKey(buildStatus) ? result.get(buildStatus) : buildStatus
}

def bddTypeDescr(bddType) {
    def result = [:]
    result.put('COPY_TEMPLATE_BASE', 'Создать копию эталонной базы 1С')
    result.put('COPY_DEV_BASE', 'Создать копию баз для разработки')
    result.put('UPDATE_FROM_STORAGE', 'Создать копию эталонной базы 1С с обновлением из хранилища')
    result.put('UPDATE_FROM_DEVBASE', 'Создать копию эталонной базы 1С с обновлением из баз разработки')
    result.put('RUN_ALL_TESTS', 'Запуск интерактивного тестирования')
    result.put('UPDATE_TEMPLATE_BASES', 'Обновление эталонных баз')
    return result.containsKey(bddType) ? result.get(bddType) : bddType
}