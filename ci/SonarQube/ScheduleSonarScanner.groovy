#!groovy
@Library("shared-libraries")
import io.bit.Consul
import io.bit.Utils
import io.bit.CustomCronExpressionJava
import  io.bit.BITConvJava
import io.bit.JenkinsJobs
import io.bit.ProjectHelpers

BITConvJava bITConvJava = new BITConvJava()
Consul consul = new Consul()
Utils utils = new Utils()
ProjectHelpers projectHelpers = new ProjectHelpers()
CustomCronExpressionJava cronExpression = new CustomCronExpressionJava()
JenkinsJobs jenkinsJobs =new JenkinsJobs()

pipeline {

    agent {
        label "${env.jenkinsAgent}"
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
        stage("Подготовка  и запись параметров") {
            steps {
                timestamps {
                    script {
                        Date date = new Date(currentBuild.startTimeInMillis)
                        ArrayList projectKeys = consul.queryList("")
                        for(int i = 0; i < projectKeys.size(); i++){
                            ArrayList sonarqubes = consul.queryList("${projectKeys[i]}/sonarqube",true)
                            for (int j = 0; j < sonarqubes?.size(); j++) {
                                String type = consul.queryVal("${projectKeys[i]}/sonarqube/${sonarqubes[j]}/type",true)
                                String repozitoryPath
                                String templatebases
                                if (type == 'repository'){
                                    repozitoryPath = consul.queryVal("${projectKeys[i]}/sonarqube/${sonarqubes[j]}/repository")
                                    templatebases = 'null'
                                }
                                if (type == 'templatebases'){
                                    templatebases = consul.queryVal("${projectKeys[i]}/sonarqube/${sonarqubes[j]}/templatebases")
                                    repozitoryPath = consul.queryVal("${projectKeys[i]}/templatebases/${templatebases}/storage1C_git")
                                }
                                if (type == 'main_extension'){
                                    templatebases = consul.queryVal("${projectKeys[i]}/sonarqube/${sonarqubes[j]}/templatebases")
                                    repozitoryPath = consul.queryVal("${projectKeys[i]}/templatebases/${templatebases}/main_extension_storage1C_git")
                                }
                                String schedule = consul.queryVal("${projectKeys[i]}/sonarqube/${sonarqubes[j]}/schedule")
                                repozitoryPath = repozitoryPath.replaceFirst('//',"//" + bITConvJava.getUserJenkins() + "@")

                                if (schedule == 'cron') {
                                    String cron = consul.queryVal("${projectKeys[i]}/sonarqube/${sonarqubes[j]}/cronExpression")
                                    echo cron
                                    if (cron){
                                        Date dateStart = cronExpression.getNextValidTimeAfter(cron,date)
                                        echo dateStart.toString()
                                        long diff = dateStart.getTime() - date.getTime()
                                        long diffMinutes = diff / (60 * 1000)
                                        echo diffMinutes.toString()
                                        if (diffMinutes < 5) {
                                            String project_server = consul.queryVal("${projectKeys[i]}/project_server",true)
                                            String labelJenkinsAgent = jenkinsAgent
                                            if (project_server) {
                                                if (utils.pingJenkinsAgent(project_server)) {
                                                    //TODO
                                                    //labelJenkinsAgent = project_server
                                                }
                                            }
                                            jenkinsJobs.runSonarScanner(projectKeys[i], type, templatebases, repozitoryPath, labelJenkinsAgent)
                                        }
                                    }
                                }
                                if (schedule == 'commit'){
                                    echo 'commit'
                                    commit = consul.queryVal("${projectKeys[i]}/sonarqube/${sonarqubes[j]}/commit")
                                    commitRemote = utils.cmdOut("git ls-remote -h ${repozitoryPath} refs/heads/master ").trim().split("\n")[1]
                                    commitRemote = commitRemote.substring(0,commitRemote.lastIndexOf("	refs"))
                                    if (commit != commitRemote){
                                        consul.putVal(commitRemote, "${projectKeys[i]}/sonarqube/${sonarqubes[j]}/commit")
                                        String project_server = consul.queryVal("${projectKeys[i]}/project_server",true)
                                        String labelJenkinsAgent = jenkinsAgent
                                        if (project_server) {
                                            if (utils.pingJenkinsAgent(project_server)) {
                                                //labelJenkinsAgent = project_server
                                            }
                                        }
                                        jenkinsJobs.runSonarScanner(projectKeys[i], type, templatebases, repozitoryPath, labelJenkinsAgent)
                                    }
                                }
                            }
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

