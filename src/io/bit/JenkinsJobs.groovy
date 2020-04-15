package io.bit
import io.bit.SlackHJava

def fillConsulProject(String projectKey, String platform1c, String needServer, projectParticipants, String issueKey, String jiraReporter, String isTestJenkinsJob) {
    result = build job: "${pathJenkinsJob(jiraReporter, isTestJenkinsJob)}infrastructure/projectManagement/createProject/steps/fillConsul",
        propagate: false,
        parameters: [
            [$class: 'StringParameterValue', name: 'projectKey', value: projectKey],
            [$class: 'StringParameterValue', name: 'platform1c', value: platform1c],
            [$class: 'StringParameterValue', name: 'needServer', value: needServer],
            [$class: 'StringParameterValue', name: 'projectParticipants', value: projectParticipants],
            [$class: 'StringParameterValue', name: 'issueKey', value: issueKey],
            [$class: 'StringParameterValue', name: 'jiraReporter', value: jiraReporter],
            [$class: 'StringParameterValue', name: 'isTestJenkinsJob', value: isTestJenkinsJob]
        ]
    updateBuildResultFromArtifact(result)
}

def fillConsulUser(String projectKey, String user, String issueKey, String jiraReporter, String isTestJenkinsJob) {
    result = build job: "${pathJenkinsJob(jiraReporter, isTestJenkinsJob)}infrastructure/projectManagement/addUserToProject/steps/fillConsul",
        propagate: false,
        parameters: [
            [$class: 'StringParameterValue', name: 'projectKey', value: projectKey],
            [$class: 'StringParameterValue', name: 'user', value: user],
            [$class: 'StringParameterValue', name: 'issueKey', value: issueKey],
            [$class: 'StringParameterValue', name: 'jiraReporter', value: jiraReporter],
            [$class: 'StringParameterValue', name: 'isTestJenkinsJob', value: isTestJenkinsJob]
        ]
    updateBuildResultFromArtifact(result)
}

def fillConsulTemplateBase(String projectKey, String confType, String postfix, String mergeType, String bddBase, String issueKey, String jiraReporter, String isTestJenkinsJob) {
    result = build job: "${pathJenkinsJob(jiraReporter, isTestJenkinsJob)}infrastructure/projectManagement/addTemplateBaseToProject/steps/fillConsul",
        propagate: false,
        parameters: [
            [$class: 'StringParameterValue', name: 'projectKey', value: projectKey],
            [$class: 'StringParameterValue', name: 'confType', value: confType],
            [$class: 'StringParameterValue', name: 'postfix', value: postfix],
            [$class: 'StringParameterValue', name: 'mergeType', value: mergeType],
            [$class: 'StringParameterValue', name: 'bddBase', value: bddBase],
            [$class: 'StringParameterValue', name: 'issueKey', value: issueKey],
            [$class: 'StringParameterValue', name: 'jiraReporter', value: jiraReporter],
            [$class: 'StringParameterValue', name: 'isTestJenkinsJob', value: isTestJenkinsJob]
        ]
    updateBuildResultFromArtifact(result)
}

def fillConsulCustomBase(String projectKey, String baseId, String autoload, String autoloadFolder, String jiraReporter, String issueKey) {
    result = build job: "${pathJenkinsJob(jiraReporter, "false")}infrastructure/projectManagement/addCustomBaseToUser/steps/fillConsul",
        propagate: false,
        parameters: [
            [$class: 'StringParameterValue', name: 'projectKey', value: projectKey],
            [$class: 'StringParameterValue', name: 'baseId', value: baseId],
            [$class: 'StringParameterValue', name: 'jiraReporter', value: jiraReporter],
            [$class: 'StringParameterValue', name: 'issueKey', value: issueKey],
            [$class: 'StringParameterValue', name: 'autoload', value: autoload],
            [$class: 'StringParameterValue', name: 'autoloadFolder', value: autoloadFolder]
        ]
    updateBuildResultFromArtifact(result)
}

def createServer(String projectKey, String osName, String roleName, String cpu, String ram, String serverName, String issueKey, String userKey, String jiraReporter, String isTestJenkinsJob) {
    result = build job: "${pathJenkinsJob(jiraReporter, isTestJenkinsJob)}infrastructure/createServer/createServer",
        propagate: false,
        parameters: [
            [$class: 'StringParameterValue', name: 'projectKey', value: projectKey],
            [$class: 'StringParameterValue', name: 'osName', value: osName],
            [$class: 'StringParameterValue', name: 'roleName', value: roleName],
            [$class: 'StringParameterValue', name: 'cpu', value: cpu],
            [$class: 'StringParameterValue', name: 'ram', value: ram],
            [$class: 'StringParameterValue', name: 'serverName', value: serverName],
            [$class: 'StringParameterValue', name: 'issueKey', value: issueKey],
            [$class: 'StringParameterValue', name: 'userKey', value: userKey],
            [$class: 'StringParameterValue', name: 'jiraReporter', value: jiraReporter],
            [$class: 'StringParameterValue', name: 'isTestJenkinsJob', value: isTestJenkinsJob]
        ]
    updateBuildResultFromArtifact(result)
}

def addUserToProject(String projectKey, String user, String issueKey, String jiraReporter, String isTestJenkinsJob) {
    result = build job: "${pathJenkinsJob(jiraReporter, isTestJenkinsJob)}infrastructure/projectManagement/addUserToProject/addUser",
        propagate: false,
        parameters: [
            [$class: 'StringParameterValue', name: 'projectKey', value: projectKey],
            [$class: 'StringParameterValue', name: 'user', value: user],
            [$class: 'StringParameterValue', name: 'issueKey', value: issueKey],
            [$class: 'StringParameterValue', name: 'jiraReporter', value: jiraReporter],
            [$class: 'StringParameterValue', name: 'isTestJenkinsJob', value: isTestJenkinsJob]
        ]
    updateBuildResultFromArtifact(result)
}

def addUserRabbitMQ(String projectKey, String user, String issueKey, String jiraReporter, String isTestJenkinsJob) {
    result = build job: "${pathJenkinsJob(jiraReporter, isTestJenkinsJob)}infrastructure/projectManagement/addUserToProject/steps/addUserRabbitMQ",
        propagate: false,
        parameters: [
            [$class: 'StringParameterValue', name: 'projectKey', value: projectKey],
            [$class: 'StringParameterValue', name: 'user', value: user],
            [$class: 'StringParameterValue', name: 'issueKey', value: issueKey],
            [$class: 'StringParameterValue', name: 'jiraReporter', value: jiraReporter],
            [$class: 'StringParameterValue', name: 'isTestJenkinsJob', value: isTestJenkinsJob]
        ]
    updateBuildResultFromArtifact(result)
}
def buildCopyTemplateBases(String jenkinsAgent, String projectKey, String user, String baseIdFilter, String taskType, String issueKey, String jiraReporter, String isTestJenkinsJob) {
    result = build job: "${pathJenkinsJob(jiraReporter, isTestJenkinsJob)}ci/steps/copy_templatebases",
        propagate: false,
        parameters: [
            [$class: 'StringParameterValue', name: 'jenkinsAgent', value: jenkinsAgent],
            [$class: 'StringParameterValue', name: 'projectKey', value: projectKey],
            [$class: 'StringParameterValue', name: 'user', value: user],
            [$class: 'StringParameterValue', name: 'taskType', value: taskType],
            [$class: 'StringParameterValue', name: 'baseIdFilter', value: baseIdFilter],
            [$class: 'StringParameterValue', name: 'issueKey', value: issueKey],
            [$class: 'StringParameterValue', name: 'jiraReporter', value: jiraReporter],
            [$class: 'StringParameterValue', name: 'isTestJenkinsJob', value: isTestJenkinsJob]
        ]
    updateBuildResultFromArtifact(result)
}

def buildUpdateTestBases(String jenkinsAgent, String projectKey, String user, String baseIdFilter, String taskType, String edtBranch, String issueKey, String jiraReporter, String isTestJenkinsJob) {
    result = build job: "${pathJenkinsJob(jiraReporter, isTestJenkinsJob)}ci/steps/updateTestBases",
        propagate: false,
        parameters: [
            [$class: 'StringParameterValue', name: 'jenkinsAgent', value: jenkinsAgent],
            [$class: 'StringParameterValue', name: 'projectKey', value: projectKey],
            [$class: 'StringParameterValue', name: 'user', value: user],
            [$class: 'StringParameterValue', name: 'baseIdFilter', value: baseIdFilter],
            [$class: 'StringParameterValue', name: 'taskType', value: taskType],
            [$class: 'StringParameterValue', name: 'edtBranch', value: edtBranch],
            [$class: 'StringParameterValue', name: 'issueKey', value: issueKey],
            [$class: 'StringParameterValue', name: 'jiraReporter', value: jiraReporter],
            [$class: 'StringParameterValue', name: 'isTestJenkinsJob', value: isTestJenkinsJob]
        ]
    updateBuildResultFromArtifact(result)
}

def buildRunBddTests(String jenkinsAgent, String projectKey, String user, String featuresBranch, String issueKey, String jiraReporter, String isTestJenkinsJob) {
    result = build job: "${pathJenkinsJob(jiraReporter, isTestJenkinsJob)}ci/steps/runTests",
        propagate: false,
        parameters: [
            [$class: 'StringParameterValue', name: 'jenkinsAgent', value: jenkinsAgent],
            [$class: 'StringParameterValue', name: 'projectKey', value: projectKey],
            [$class: 'StringParameterValue', name: 'user', value: user],
            [$class: 'StringParameterValue', name: 'featuresBranch', value: featuresBranch],
            [$class: 'StringParameterValue', name: 'issueKey', value: issueKey],
            [$class: 'StringParameterValue', name: 'jiraReporter', value: jiraReporter],
            [$class: 'StringParameterValue', name: 'isTestJenkinsJob', value: isTestJenkinsJob]
        ]
    updateBuildResultFromArtifact(result)
    return result
}

def buildRunSmokeTests(String jenkinsAgent, String projectKey, String featuresBranch, String issueKey, String jiraReporter) {
    result = build job: "${pathJenkinsJob(jiraReporter, 'false')}ci/steps/runSmokeTests",
        propagate: false,
        parameters: [
            [$class: 'StringParameterValue', name: 'jenkinsAgent', value: jenkinsAgent],
            [$class: 'StringParameterValue', name: 'projectKey', value: projectKey],
            [$class: 'StringParameterValue', name: 'featuresBranch', value: featuresBranch],
            [$class: 'StringParameterValue', name: 'issueKey', value: issueKey],
            [$class: 'StringParameterValue', name: 'jiraReporter', value: jiraReporter]
        ]
    updateBuildResultFromArtifact(result)
    return result
}

def buildClearTempUser(String jenkinsAgent, String projectKey, String user, String issueKey, String jiraReporter) {
    result = build job: "${pathJenkinsJob(jiraReporter, isTestJenkinsJob)}ci/steps/clearTempUser",
        propagate: false,
        parameters: [
            [$class: 'StringParameterValue', name: 'jenkinsAgent', value: jenkinsAgent],
            [$class: 'StringParameterValue', name: 'projectKey', value: projectKey],
            [$class: 'StringParameterValue', name: 'user', value: user],
            [$class: 'StringParameterValue', name: 'issueKey', value: issueKey],
            [$class: 'StringParameterValue', name: 'jiraReporter', value: jiraReporter]
        ]
    updateBuildResultFromArtifact(result)
    return result
}

def buildBddMain(String projectKey, String jiraReporter, String baseIdFilter, String taskType, String testType, String issueKey, String isTestJenkinsJob) {
    result = build job: "${pathJenkinsJob(jiraReporter, isTestJenkinsJob)}ci/bddPipeline",
        propagate: false,
        parameters: [
            [$class: 'StringParameterValue', name: 'projectKey', value: projectKey],
            [$class: 'StringParameterValue', name: 'taskType', value: taskType],
            [$class: 'StringParameterValue', name: 'testType', value: testType],
            [$class: 'StringParameterValue', name: 'baseIdFilter', value: baseIdFilter],
            [$class: 'StringParameterValue', name: 'issueKey', value: issueKey],
            [$class: 'StringParameterValue', name: 'jiraReporter', value: jiraReporter],
            [$class: 'StringParameterValue', name: 'isTestJenkinsJob', value: isTestJenkinsJob]
        ]
    updateBuildResultFromArtifact(result)
}

def addUserStorage1c(String jenkinsAgent, String projectKey, String user, String issueKey, String jiraReporter, String isTestJenkinsJob) {
    result = build job: "${pathJenkinsJob(jiraReporter, isTestJenkinsJob)}infrastructure/projectManagement/addUserToProject/steps/addUserStorage1c",
        propagate: false,
        parameters: [
            [$class: 'StringParameterValue', name: 'jenkinsAgent', value: jenkinsAgent],
            [$class: 'StringParameterValue', name: 'projectKey', value: projectKey],
            [$class: 'StringParameterValue', name: 'user', value: user],
            [$class: 'StringParameterValue', name: 'issueKey', value: issueKey],
            [$class: 'StringParameterValue', name: 'jiraReporter', value: jiraReporter],
            [$class: 'StringParameterValue', name: 'isTestJenkinsJob', value: isTestJenkinsJob]
        ]
    updateBuildResultFromArtifact(result)
}

def createStorage1c(String projectKey, String baseId, String issueKey, String jiraReporter) {
    result = build job: "${pathJenkinsJob(jiraReporter, "false")}infrastructure/projectManagement/addTemplateBaseToProject/steps/createStorage1c",
        propagate: false,
        parameters: [
            [$class: 'StringParameterValue', name: 'projectKey', value: projectKey],
            [$class: 'StringParameterValue', name: 'baseId', value: baseId],
            [$class: 'StringParameterValue', name: 'issueKey', value: issueKey],
            [$class: 'StringParameterValue', name: 'jiraReporter', value: jiraReporter],
        ]
    updateBuildResultFromArtifact(result)
}

def createStorage1cExt(String projectKey, String baseId, String issueKey, String jiraReporter) {
    result = build job: "${pathJenkinsJob(jiraReporter, "false")}infrastructure/projectManagement/addTemplateBaseToProject/steps/createStorage1cExt",
        propagate: false,
        parameters: [
            [$class: 'StringParameterValue', name: 'projectKey', value: projectKey],
            [$class: 'StringParameterValue', name: 'baseId', value: baseId],
            [$class: 'StringParameterValue', name: 'issueKey', value: issueKey],
            [$class: 'StringParameterValue', name: 'jiraReporter', value: jiraReporter],
        ]
    updateBuildResultFromArtifact(result)
}

def runSonarScanner(String projectKey, String type, String templatebases, String repozitoryPath, String jenkinsAgent, String issueKey = 'jenkins', String jiraReporter = 'jenkins') {
    build job: "${pathJenkinsJob(jiraReporter, "false")}sonarqube/run",
        parameters: [
            [$class: 'StringParameterValue', name: 'projectKey', value: projectKey],
            [$class: 'StringParameterValue', name: 'repozitoryPath', value: repozitoryPath],
            [$class: 'StringParameterValue', name: 'jenkinsAgent', value: jenkinsAgent],
            [$class: 'StringParameterValue', name: 'templatebases', value: templatebases],
            [$class: 'StringParameterValue', name: 'type', value: type],
            [$class: 'StringParameterValue', name: 'issueKey', value: issueKey],
            [$class: 'StringParameterValue', name: 'jiraReporter', value: jiraReporter],
        ],
        wait: false
}

def lockScheduledJobs(String jenkinsAgent, String dbServer, String sqlUser, String sqlPwd, String lockType, String jiraReporter) {
    result = build job: "${pathJenkinsJob(jiraReporter, "false")}infrastructure/blockJobs/steps/lockScheduledJobs",
        propagate: false,
        parameters: [
            [$class: 'StringParameterValue', name: 'JENKINS_AGENT', value: jenkinsAgent],
            [$class: 'StringParameterValue', name: 'DB_SERVER', value: dbServer],
            [$class: 'StringParameterValue', name: 'SQL_USER', value: sqlUser],
            [$class: 'StringParameterValue', name: 'SQL_PASSW', value: sqlPwd],
            [$class: 'StringParameterValue', name: 'LOCK_TYPE', value: lockType]
        ]
    updateBuildResultFromArtifact(result)
}

def unlockScheduledJobs(String jenkinsAgent, String dbServer, String sqlUser, String sqlPwd, String lockType, String jiraReporter) {
    result = build job: "${pathJenkinsJob(jiraReporter, "false")}infrastructure/blockJobs/steps/unlockScheduledJobs",
        propagate: false,
        parameters: [
            [$class: 'StringParameterValue', name: 'JENKINS_AGENT', value: jenkinsAgent],
            [$class: 'StringParameterValue', name: 'DB_SERVER', value: dbServer],
            [$class: 'StringParameterValue', name: 'SQL_USER', value: sqlUser],
            [$class: 'StringParameterValue', name: 'SQL_PASSW', value: sqlPwd],
            [$class: 'StringParameterValue', name: 'LOCK_TYPE', value: lockType]
        ]
    updateBuildResultFromArtifact(result)
}

def createFeaturesRepo(String projectKey, String issueKey, String jiraReporter) {
    result = build job: "${pathJenkinsJob(jiraReporter, "false")}infrastructure/projectManagement/createProject/steps/createFeaturesRepo",
        propagate: false,
        parameters: [
            [$class: 'StringParameterValue', name: 'projectKey', value: projectKey],
            [$class: 'StringParameterValue', name: 'jiraReporter', value: jiraReporter],
            [$class: 'StringParameterValue', name: 'issueKey', value: issueKey]
        ]
    updateBuildResultFromArtifact(result)
}

def fillConsulSmokeBase(String projectKey, String baseId, String jiraReporter, String issueKey) {
    result = build job: "${pathJenkinsJob(jiraReporter, "false")}/infrastructure/projectManagement/addSmokeBaseToProject/steps/fillConsul",
        propagate: false,
        parameters: [
            [$class: 'StringParameterValue', name: 'projectKey', value: projectKey],
            [$class: 'StringParameterValue', name: 'baseId', value: baseId],
            [$class: 'StringParameterValue', name: 'jiraReporter', value: jiraReporter],
            [$class: 'StringParameterValue', name: 'issueKey', value: issueKey]
        ]
    updateBuildResultFromArtifact(result)
}

def createFolderProject(String projectKey, String issueKey, String jiraReporter) {
    result = build job: "${pathJenkinsJob(jiraReporter, "false")}/infrastructure/projectManagement/createProject/steps/createFolderProject",
        propagate: false,
        parameters: [
            [$class: 'StringParameterValue', name: 'projectKey', value: projectKey],
            [$class: 'StringParameterValue', name: 'jiraReporter', value: jiraReporter],
            [$class: 'StringParameterValue', name: 'issueKey', value: issueKey]
        ]
    updateBuildResultFromArtifact(result)
}

def pathJenkinsJob(String jiraReporter, String isTestJenkinsJob) {

    projectHelpers = new ProjectHelpers()
    jenkinsIntegration = new JenkinsIntegration()
    bitConvJava = new BITConvJava()

    if (projectHelpers.isDraftJob()) {
        isTestJenkinsJob = "true"
    }
    if (jiraReporter == null || jiraReporter.isEmpty() || jiraReporter == bitConvJava.getUserJenkins()) {
        jiraReporter = jenkinsIntegration.jobNameUser()
    }
    
    return isTestJenkinsJob.contains('true') ? "z_drafts/${jiraReporter}/" : ''
}

def updateBuildResultFromArtifact(build){

    Utils utils = new Utils()

    currentBuild.result  = build.getResult()
    if (build.getResult() == "FAILURE") {
        artifactUrl = "${build.getAbsoluteUrl()}/artifact/BuildResultMessage.txt"
        errorText = ""
        try {
            echo "sending http request to ${artifactUrl}"
            errorText = SlackHJava.getHttpRequestJenkins(artifactUrl, "UTF-8")
        } catch (excp) {
            utils.raiseError("Child build failed when sending http request for BuildResultMessage.txt. See detailed info by link: ${excp.getMessage()}")
        }

        if (errorText.isEmpty()) {
            errorText = "Вложенный пайплайн по адресу ${build.getAbsoluteUrl()} завершился с неизвестной ошибкой. Смотрите логи для подробностей"
        }
        utils.raiseError(errorText)
    } else if (build.getResult() == "ABORTED") {
        utils.raiseError("Вложенный пайплайн ${build.getAbsoluteUrl()} был прерван пользователем")
    }
}