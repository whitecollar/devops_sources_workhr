@Library("shared-libraries")
import io.bit.Consul
import io.bit.ProjectHelpers
import io.bit.BITConvJava

def consul = new Consul()
def projectHelpers = new ProjectHelpers()
def bitConvJava = new BITConvJava()

pipeline {
    agent {
        label "service_NewProject"
    }

    parameters {
        string(description: 'Project key in jira', name: 'projectKey')
        string(description: 'Configuration 1c type', name: 'confType')
        string(description: 'Update type for infobase. Allowed values are: LOAD, MERGE_CFG, MERGE_DISTRIBUTION', name: 'mergeType')
        string(defaultValue: "", description: 'Optional. Infobase postfix', name: 'postfix')
        string(defaultValue: "false", description: 'Optional. If true, this infobase will be used fo running BDD tests', name: 'bddBase')
        string(defaultValue: "", description: 'Optional. System parameter with issue key from SD', name: 'issueKey')
        string(defaultValue: "", description: 'Optional. Issue author from SD', name: 'jiraReporter')
        string(defaultValue: "", description: 'Obsolete', name: 'isTestJenkinsJob')
    }

    options {
        buildDiscarder(logRotator(daysToKeepStr:'30'))
        timeout(time: 60000, unit: 'SECONDS') 
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
        stage("Заполнение настроек в Consul") {
            steps {
                timestamps {
                    script {
                        
                        projectKey = projectKey.toLowerCase()
                        baseId = bitConvJava.combineBaseId(confType, postfix);
                        templateBaseName = bitConvJava.combine1cBaseNameForTemplate(confType, projectKey, postfix)
                        admin1cUser = bitConvJava.getAdmin1cUser()
                        admin1cPwd = bitConvJava.getAdmin1cPwd()
                        storage1cExt = bitConvJava.combine1cStorageExt(projectKey, baseId)
                        
                        def templBaseGroup = "${env.projectKey}/templatebases/${baseId}/"

                        //Создаем группу для свойств эталнной базы

                        kvResponse = consul.putVal('', templBaseGroup)

                        //Задаем параметры эталонной базы
                        consul.putVal("${consul.queryVal("${projectKey}/project_server")}", templBaseGroup + "server")
                        consul.putVal(templateBaseName, templBaseGroup + "base")
                        consul.putVal("/S${consul.queryValFromConsul(projectKey, "project_server")}\\${templateBaseName}", templBaseGroup + "connection_string")
                        consul.putVal("${consul.queryValFromConsul(projectKey, "storage1с_server_tcp")}/${baseId}_${projectKey}", templBaseGroup + "storage1C_tcp")
                        consul.putVal("${consul.queryValFromConsul(projectKey, "storage1с_server_tcp")}/${storage1cExt}", templBaseGroup + "main_extension_storage1C_tcp")
                        platform1C = consul.queryValFromConsul(projectKey, "project_server_platform").replaceAll("\\.", "")
                        consul.putVal("${consul.queryVal("${projectKey}/storage1с_server_file")}\\${platform1C}\\${baseId}_${projectKey}\\1cv8ddb.1CD", templBaseGroup + "storage1C_file")
                        consul.putVal("${consul.queryVal("${projectKey}/storage1с_server_file")}\\${platform1C}\\${storage1cExt}\\1cv8ddb.1CD", templBaseGroup + "main_extension_storage1C_file")
                        consul.putVal("auto", templBaseGroup + "storage_user")
                        consul.putVal(admin1cUser, templBaseGroup + "admin_1c_user")
                        consul.putVal(admin1cPwd, templBaseGroup + "admin_1c_password")
                        consul.putVal(bitConvJava.combineBaseIdRepo(projectKey,baseId), "${projectKey}/templatebases/${baseId}/storage1C_git")
                        consul.putVal(bitConvJava.combineBaseExtensionRepo(projectKey,baseId), "${projectKey}/templatebases/${baseId}/main_extension_storage1C_git")
                        if (bddBase == "true") {
                            consul.putVal(baseId, "${env.projectKey}/bddBase")                            
                        }
                        consul.putVal(mergeType, templBaseGroup + "mergetype")
                        consul.putVal("MergeSettings${baseId.toUpperCase()}.xml", templBaseGroup + "mergesettings")
                        consul.putValIfNotExists("", templBaseGroup + "storage_edt")
                        consul.putVal(postfix, "${projectKey}/templatebases/${baseId}/postfix")

                        //Добавим базы, связанные с эталонной каждому пользователю 
                        userList = consul.queryList("${projectKey}/users")
                        for (def curUser : userList) {
                            projectHelpers.createbases(env.projectKey, curUser, baseId, consul)
                        }

                    }
                }
            }
        }
    }
    post {
        always {
            script {
                projectHelpers.beforeEndJob()
            }
        }
    }
}