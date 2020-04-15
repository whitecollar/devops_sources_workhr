package io.bit

import net.sf.json.JSONObject

// Создает ошибку в JIRA через HTTP запрос и возращает результат запроса
// Логика назначения исполнителя на ошибку - если в параметре передан эпик - исполнитель эпика,
// в других случаях - ответственный за компонент it_ci_error в проекте DEVOPS
//
// Параметры:
//    build - ссылка на сборку (обязательно)
//    projectKey - ключ проекта, в рамках которого запускался пайплайн
//    issueKey - ключ задачи-инициатора, из которой запустился пайплайн, обычно передается в пайплайн в параметре issueKey
//    epic - ключ задачи-эпика
//    summary - заголовок ошибки
//    descr - текст ошибки
//
// Возвращаемое значение
//    String - При успешном создании ошибки возвращается ключ ошибки. Иначе - текст ошибки
//
def createBug(buildUrl, projectKey = null, issueKey = null, epic = null, summary = null, descr = null){

    validResponses = '200:299'

    def jiraUrl = "https://s.bit-erp.ru/rest/scriptrunner/latest/custom/jenkinsCreateBug?build=${buildUrl}"

    if(projectKey != null){
        jiraUrl += "&projectKey=${projectKey}"
    }
    if(issueKey != null){
        jiraUrl += "&issueKey=${issueKey}"
    }
    if(epic != null){
        jiraUrl += "&epic=${epic}"
    }

    JSONObject body = new JSONObject()
    if(summary != null){
        def encodedSummary = java.net.URLEncoder.encode(summary, "UTF-8")
        body.put("summary", encodedSummary)
    }
    if(descr != null){
        def encodedDescr = java.net.URLEncoder.encode(descr, "UTF-8")
        body.put("descr", encodedDescr)
    }
    jiraResponse = httpRequest httpMode: 'POST', url: jiraUrl, requestBody: body.toString(), validResponseCodes: validResponses
    return jiraResponse.content
}

def getEmailByLogin(login) {
    return RestJava.exec(BITConvJava.JIRA_URL, BITConvJava.JENKINS_BASE64_CRED, "/rest/api/2/user?username=${login}", "GET").get("emailAddress")
}

def getSlackIDByLogin(login) {
    projectHelpers = new ProjectHelpers()
    if (projectHelpers.isDraftJob() && (login == null || login.isEmpty())) {
        jenkinsIntegration = new JenkinsIntegration()
        login = jenkinsIntegration.jobNameUser()
    }
    return RestJava.exec(BITConvJava.JIRA_URL, BITConvJava.JENKINS_BASE64_CRED, "/rest/scriptrunner/latest/custom/getUserProperty?login=${login}&prop=jira.meta.SlackID", "GET").get("prop")
}

def getIssueSummary(issueKey) {
    if (issueKey.isEmpty()) {
        return ""
    }
    else {
        return RestJava.exec(BITConvJava.JIRA_URL, BITConvJava.JENKINS_BASE64_CRED, "/rest/api/2/issue/${issueKey}", "GET").get("fields").get("summary")
    }
}

def getIssuesByEpic(epicKey) {
    return RestJava.exec(
        BITConvJava.JIRA_URL, 
        BITConvJava.JENKINS_BASE64_CRED, 
        // /rest/api/2/search?jql=issuetype = Ошибка AND status != Done AND "Epic Link" = DEVOPS-608 order by status ASC,Rank
        "/rest/api/2/search?jql=issuetype%20%3D%20%D0%9E%D1%88%D0%B8%D0%B1%D0%BA%D0%B0%20AND%20status%20!%3D%20Done%20AND%20%22Epic%20Link%22%20%3D%20${epicKey}%20order%20by%20status%20ASC%2CRank", 
        "GET",
        null
    )
}

def getEpicAutomaticTesting() {
    return "DEVOPS-609"
}

def getEpicCreateTemplateBase() {
    return "DEVOPS-613"
}

def getEpicCreateOtherBases() {
    return "DEVOPS-610"
}

def getEpicCreateProject() {
    return "DEVOPS-611"
}

def getEpicLockSheduledJobs() {
    return "DEVOPS-608"
}

def getEpicCreateTestBases() {
    return "DEVOPS-615"
}

def getEpicMaintainingServers() {
    return "DEVOPS-616"
}

def getEpicCustomBase() {
    return "DEVOPS-703"
}

def getEpicSonarQube() {
    return "DEVOPS-612"
}