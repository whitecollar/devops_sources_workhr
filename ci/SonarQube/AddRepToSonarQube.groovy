#!groovy
@Library("shared-libraries")
import io.bit.Consul
import io.bit.RestJava
import io.bit.Utils
import  io.bit.BITConvJava
import io.bit.ProjectHelpers
import io.bit.JIRAIntegration
import io.bit.Notification

Consul consul = new Consul()
Utils utils = new Utils()
BITConvJava bITConvJava = new BITConvJava()
ProjectHelpers projectHelpers = new ProjectHelpers()
JIRAIntegration jIRAIntegration = new JIRAIntegration()
Notification notification = new Notification()
String projectName
String repozitoryPath

pipeline {

    agent {
        label "${env.jenkinsAgent}"
    }
    parameters {
        string(description: 'Project key in jira', name: 'projectKey')
        string(description: 'Type connect', name: 'connect')
        string(defaultValue: "", description: 'Name template bases', name: 'templateBase')
        string(defaultValue: "", description: 'Path in repository', name: 'repository')
        string(defaultValue: "", description: 'Schedule', name: 'schedule')
        string(defaultValue: "", description: 'Schedule cron expression', name: 'cronExpression')
        string(defaultValue: "newbdd", description: 'Jenkins agent for pipeline', name: 'jenkinsAgent')
        string(defaultValue: "jenkins", description: 'Optional. System parameter with issue key from SD', name: 'issueKey')
        string(defaultValue: "jenkins", description: 'Optional. Issue author from SD', name: 'jiraReporter')
        string(defaultValue: "false", description: 'Optional. Obsolete parameter you should not use', name: 'isTestJenkinsJob')
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
        stage("Подготовка  и запись параметров в консул") {
            steps {
                timestamps {
                    script {
                        if (schedule == 'Раз в день (в 02:00 мск)'){
                            schedule = 'cron'
                            cronExpression = "0 0 2 * * ?"
                        }
                        if (schedule == 'По коммиту'){
                            schedule = 'commit'
                        }
                        if (schedule == 'По расписанию cron'){
                            schedule = 'cron'
                        }
                        if (connect == 'Репозиторий'){//репозиторий не у нас
                            repozitoryPath = bITConvJava.BITBUCKET_URL + "/scm/" + projectKey + "/" + repository + ".git"
                            projectName = repository
                            consul.putVal(repozitoryPath, projectKey + "/sonarqube/" + projectName + "/repository")
                            consul.putVal(schedule, projectKey + "/sonarqube/" + projectName + "/schedule")
                            consul.putVal('repository', projectKey + "/sonarqube/" + projectName + "/type")
                            if (schedule == 'cron'){
                                consul.putVal(cronExpression, projectKey + "/sonarqube/" + projectName + "/cronExpression")
                            }
                        }
                        if (connect == 'Эталонную БД') {
                            storage1cFile = consul.queryVal("${projectKey}/templatebases/" + templateBase + "/storage1C_file")
                            repozitoryPath = consul.queryVal(projectKey + "/templatebases/" + templateBase + "/storage1C_git")
                            projectName = repozitoryPath.substring(repozitoryPath.lastIndexOf("/") + 1, repozitoryPath.lastIndexOf(".git"))
                            consul.putVal(templateBase, projectKey + "/sonarqube/" + projectName + "/templatebases")
                            consul.putVal(schedule, projectKey + "/sonarqube/" + projectName + "/schedule")
                            consul.putVal('templatebases', projectKey + "/sonarqube/" + projectName + "/type")
                            if (schedule == 'cron'){
                                consul.putVal(cronExpression, projectKey + "/sonarqube/" + projectName + "/cronExpression")
                            }
                        }
                        if (connect == 'Расширение эталонной БД') {
                            storage1cFile = consul.queryVal("${projectKey}/templatebases/" + templateBase + "/main_extension_storage1C_file")
                            repozitoryPath = consul.queryVal(projectKey + "/templatebases/" + templateBase + "/main_extension_storage1C_git")
                            projectName = repozitoryPath.substring(repozitoryPath.lastIndexOf("/") + 1, repozitoryPath.lastIndexOf(".git"))
                            consul.putVal(templateBase, projectKey + "/sonarqube/" + projectName + "/templatebases")
                            consul.putVal(schedule, projectKey + "/sonarqube/" + projectName + "/schedule")
                            consul.putVal('main_extension', projectKey + "/sonarqube/" + projectName + "/type")
                            if (schedule == 'cron'){
                                consul.putVal(cronExpression, projectKey + "/sonarqube/" + projectName + "/cronExpression")
                            }
                        }
                        repozitoryPath = repozitoryPath.replaceFirst('//',"//" + bITConvJava.getUserJenkins() + "@")
                        if (schedule == 'commit'){
                            commit = utils.cmdOut("git ls-remote -h ${repozitoryPath} refs/heads/master ").trim().split("\n")[1]
                            commit = commit.substring(0,commit.lastIndexOf("	refs"))
                            consul.putVal(commit, projectKey + "/sonarqube/" + projectName + "/commit")
                        }
                        slackChannel = consul.queryVal("${projectKey}/slack/${projectKey}_build_log")
                        taskType = jIRAIntegration.getIssueSummary(issueKey)
                    }
                }
            }
        }
        stage('Отправка уведомления в slack') {
            steps {
                timestamps {
                    script {
                        threadId = notification.sendSlackStartBuild(slackChannel, taskType, jiraReporter, issueKey)
                        currentBuild.result = "SUCCESS"
                    }
                }
            }
        }
        stage("Настройка проекта в sonarqube") {
            steps {
                timestamps {
                    script {
                        String url
                        url = "/api/projects/create?project=${projectName}&name=${projectName}"
                        try {
                            RestJava.exec(BITConvJava.SONARQUBE_URL, BITConvJava.SONARQUBE_BASE64_CRED, url,"POST")
                        }catch (Exception ex){
                            echo ex.toString()
                        }
                        url = "/api/user_groups/create?name=${projectKey}"
                        try {
                            RestJava.exec(BITConvJava.SONARQUBE_URL, BITConvJava.SONARQUBE_BASE64_CRED, url,"POST")
                        }
                        catch (Exception ex){
                            echo ex.toString()
                        }
                        url = "/api/projects/update_visibility?visibility=private&project=${projectName}"
                        try {
                            RestJava.exec(BITConvJava.SONARQUBE_URL, BITConvJava.SONARQUBE_BASE64_CRED, url,"POST")
                        }
                        catch (Exception ex){
                            echo ex.toString()
                        }
                        url = "/api/permissions/add_group?groupName=${projectKey}&permission=user&projectKey=${projectName}"
                        try {
                            RestJava.exec(BITConvJava.SONARQUBE_URL, BITConvJava.SONARQUBE_BASE64_CRED, url,"POST")
                        }
                        catch (Exception ex){
                            echo ex.toString()
                        }
                        url = "/api/permissions/add_group?groupName=${projectKey}&permission=codeviewer&projectKey=${projectName}"
                        try{
                            RestJava.exec(BITConvJava.SONARQUBE_URL, BITConvJava.SONARQUBE_BASE64_CRED, url,"POST")
                        }
                        catch (Exception ex){
                            echo ex.toString()
                        }
                        url = "/api/permissions/add_group?groupName=biterp&permission=codeviewer&projectKey=${projectName}"
                        try{
                            RestJava.exec(BITConvJava.SONARQUBE_URL, BITConvJava.SONARQUBE_BASE64_CRED, url,"POST")
                        }
                        catch (Exception ex){
                            echo ex.toString()
                        }
                        url = "/api/permissions/add_group?groupName=biterp&permission=user&projectKey=${projectName}"
                        try{
                            RestJava.exec(BITConvJava.SONARQUBE_URL, BITConvJava.SONARQUBE_BASE64_CRED, url,"POST")
                        }
                        catch (Exception ex){
                            echo ex.toString()
                        }
                        url = "/api/permissions/add_group?groupName=biterp&permission=issueadmin&projectKey=${projectName}"
                        try{
                            RestJava.exec(BITConvJava.SONARQUBE_URL, BITConvJava.SONARQUBE_BASE64_CRED, url,"POST")
                        }
                        catch (Exception ex){
                            echo ex.toString()
                        }
                    }
                }
            }
        }
        stage("Настройка репозитория в Bitbucket") {
            steps {
                timestamps {
                    script {
                        utils.cmd("git --git-dir=${env.WORKSPACE} config --global http.postBuffer 524288000")
                        utils.cmd("git --git-dir=${env.WORKSPACE} clone $repozitoryPath")
                        projectDir = "${env.WORKSPACE}\\$projectName"
                        if (connect != 'Репозиторий') {
                            utils.cmd("cd ${projectDir}\n vanessa-sonar configure -k ${projectName} -n ${projectName} --src $projectDir\\src\\cf --group --subsystemsConfPath ${env.WORKSPACE}\\ci\\SonarQube\\sonar.subsystem.json")
                            if (!fileExists ("$projectDir\\src\\cf\\AUTHORS") || !fileExists ("$projectDir\\src\\cf\\VERSION")) {
                                utils.cmd("gitsync init $storage1cFile $projectDir\\src\\cf")
                                }
                        }
                        if (!fileExists("$projectDir\\sonar-project.properties")){
                            def text = "sonar.projectKey=$projectName\nsonar.projectName=$projectName\nsonar.sources=.\nsonar.sourceEncoding=UTF-8\nsonar.inclusions=**/*.bsl, **/*.os, **/*.xml\nsonar.exclusions=**/*.html\nsonar.lang.patterns.xml=**/*.xsd,**/*.xsl\nsonar.bsl.skipVendorSupportedObjects=true"
                            writeFile(file: "$projectDir\\sonar-project.properties", text: text, encoding: "UTF-8")
                        }
                        if (fileExists("$projectDir\\src\\cf\\VERSION")){
                            String version = readFile(file: "$projectDir\\src\\cf\\VERSION", encoding: "UTF-8")
                            if (version.contains("<VERSION></VERSION>")){
                                version = version.replaceAll("<VERSION></VERSION>","<VERSION>0</VERSION>")
                                writeFile(file: "$projectDir\\src\\cf\\VERSION", text: version, encoding: "UTF-8")
                            }
                        }
                        if (fileExists("$projectDir\\src\\cf\\AUTHORS")) {
                            String autors = readFile(file: "$projectDir\\src\\cf\\AUTHORS", encoding: "UTF-8")
                            for (String autor : autors.trim().split("\n")){
                                String email = autor.substring(autor.indexOf("<") + 1,autor.indexOf(">"))
                                autor = autor.substring(0,autor.indexOf("="))
                                try{
                                    String emailNew = jIRAIntegration.getEmailByLogin(autor)
                                    autors = autors.replaceAll(email,emailNew)
                                }catch (Exception ex){
                                    echo ex.toString()
                                }
                            }
                            writeFile(file: "$projectDir\\src\\cf\\AUTHORS", text: autors, encoding: "UTF-8")
                        }
                        writeFile(file: "$projectDir\\.gitignore", text: "/.scannerwork/", encoding: "UTF-8")
                        utils.cmd("git --git-dir=$projectDir\\.git --work-tree=$projectDir add --all")
                        utils.cmd("git --git-dir=$projectDir\\.git --work-tree=$projectDir commit -m $issueKey")
                        utils.cmd("git --git-dir=$projectDir\\.git --work-tree=$projectDir push origin master")
                    }
                }
            }
        }
    }
    post {
        always {
            script {
                notification.sendSlackFinishBuild(
                    slackChannel,
                    taskType,
                    currentBuild.result,
                    null,
                    jiraReporter,
                    threadId,
                    "",
                    issueKey
                )
                projectHelpers.beforeEndJob()
                cleanWs()
            }
        }
    }
}

