package io.bit

class BITConvJava
{
    public static final String JIRA_URL = "https://s.bit-erp.ru"
    public static final String BITBUCKET_URL = "https://code.bit-erp.ru"
    public static final String SONARQUBE_URL = "https://code.bit-erp.ru/sonar"
    public static final String EXCHANGE_URL = "https://mail.bit-erp.ru"
    public static final String SLACK_URL = "https://slack.com/api/"
    public static final String STORAGE_1C_SERVER_TCP = "tcp://storage1c.bit-erp.loc"
    public static final String JENKINS_BASE64_CRED = "amVua2luczpeMmtrUjkybVMpUlM="
    public static final String SONARQUBE_BASE64_CRED = "U29uYXJRdWJlSmF2YTprQWhmVTdOTnYwZChSOSkoKWx2aTN1MTN5ZjVT"
    public static final String SLACK_BOT_TOKEN = "xoxb-91011106341-456527166481-S3VIrzNHjc8JoRwSyBpybi1V"
    public static final String SLACK_TEST_TOKEN = "xoxp-91011106341-419637548257-456958324276-1b5a35b76255d56bd2f7eb9f9d8eca4f"
    public static final String SLACK_APP_TOKEN = "xoxp-91011106341-419637548257-457283692597-cf339fe4e9849e6c02f8d604e18383a2"
    /////////////////
    // COMBINERS
    ////////////////
    
    static String combine1cProjectServerName(String projectKey){
        projectKey = projectKey.toLowerCase()
        return "dev" + projectKey
    }
    static String combine1cBaseNameForDev(String username, String confType, String projectKey, String postfix = null){
        String result = "dev"
        result += "_" + username
        result += "_" + confType
        result += "_" + projectKey
        if(postfix != null && !postfix.isEmpty()){
            result += "_" + postfix
        }
        return result
    }
    static String combine1cBaseNameForTemplate(String confType, String projectKey, String postfix = null){
        String result = "template"
        result += "_" + confType
        if(postfix != null && !postfix.isEmpty()){
            result += postfix
        }
        result += "_" + projectKey.toLowerCase()
        return result
    }
    static String combineBaseId(String confType, String postfix = null){
        String result = confType
        if(postfix != null && !postfix.isEmpty()){
            result += postfix
        }
        return result
    }
    static String combineRepositoryNameForFeature(String projectKey){
        return projectKey.toLowerCase() + "_features"
    }
    static String combineRepositoryNameForGitSync(String projectKey, String confType){
        return projectKey.toLowerCase() + "_" + confType + "_gitsync"
    }
    static String combineRepositoryNameForSonar(String projectKey, String confType){
        return projectKey.toLowerCase() + "_" + confType + "_sonar"
    }
    static String combineRepositoryNameForSources(String projectKey, String confType){
        return projectKey.toLowerCase() + "_" + confType.toLowerCase() + "_sources1c"
    }
    static String combineRepositoryNameForExtension(String projectKey, String confType){
        return projectKey.toLowerCase() + "_" + confType.toLowerCase() + "_ext"
    }
    static String combineRabbitMQServerName(String projectKey){
        return combine1cProjectServerName(projectKey) + "rmq.bit-erp.loc"
    }
    static String combineRabbitMQUser(String user, String projectKey){
        return user + "_" + projectKey
    }
    static String combineSlackChannel(String projectKey){
        return projectKey + "_build_log"
    }
    static String combineFeaturesRepo(String projectKey){
        return "https://jenkins@code.bit-erp.ru/scm/" + projectKey + "/" + projectKey + "_features.git"
    }
    static String combineBaseIdRepo(String projectKey, String baseId){
        return BITBUCKET_URL + "/scm/" + projectKey + "/" + projectKey + "_" + baseId + "_sources1c.git"
    }
    static String combineBaseExtensionRepo(String projectKey, String baseId){
        return BITBUCKET_URL + "/scm/" + projectKey + "/" + projectKey + "_" + baseId + "_ext.git"
    }
    static String combinePlatform1cConsulPath(platform1c) {
        return "devops/platforms_1c/${platform1c.replaceAll("\\.", "_")}"
    }
    static combineEmail(String login) {
        return "${login}@bit-erp.ru"
    }
    static String combineCustomBaseName(String projectKey, String baseId, String user) {
        return "custom_${user}_${baseId}_${projectKey}"
    }
    static String combine1cStorageExt(String projectKey, String baseId) {
        return "${baseId}_ext_${projectKey}"
    }
    static String combineWebPubFolder(String publicname) {
        return "C:\\inetpub\\www\\${publicname}" 
    }
    static String combineRabbitMQPortAPI(port) {
        return "1${port}"
    }
    static String combineJiraIssueLink(issueKey) {
        return "${JIRA_URL}/browse/${issueKey}"
    }
    static String combineStorage1cTCP(port) {
        return "${STORAGE_1C_SERVER_TCP}:${port}"
    }
    static String combineUserTestbase(user, baseId, projectKey) {
        return "${user}_${baseId}_${projectKey}"
    }
    static String combineSmoketestBase(user, baseId, projectKey) {
        return "smoke_${user}_${baseId}_${projectKey}"
    }
    static String combineSmokeTemplatebase(baseId, projectKey) {
        return "smoke_template_${baseId}_${projectKey}"
    }
    static String combineConnString1c(server1c, infobase) {
        return "Srvr=\"${server1c}\";Ref=\"${infobase}\";"
    }
    static String combineConnString(server1c, infobase) {
        return "/S${server1c}\\${infobase}"
    }

    /////////////////
    // GETTERS
    ////////////////

    static String getGroupForProject(String projectKey){
        return projectKey.toLowerCase()
    }
    static String getReadOnlyGroupForProject(String projectKey){
        return (projectKey + "_RO").toLowerCase()
    }
    static String getStorage1CFolder(){
        return "\\\\172.16.50.22\\repos"
    }
    static String getBackupShare(){
        return "\\\\share\\temp"
    }
    static String getStorage1cDefUserName(){
        return "auto"
    }
    static String getStorage1cDefUserPass(){
        return "111"
    }
    static String userServiceAgent(user, projectKey) {
        if (user == getUserJenkins()) {
            return combine1cProjectServerName(projectKey) + "service"
        } else {
            return user + "service"
        }
    }
    static String getRabbitmqDefPort(){
        return "5672"
    }
    static String getUserJenkins() {
        return "jenkins"
    }
    static String getServerJenkins() {
        return "http://ci.bit-erp.ru"
    }
    static String getExtTemplatePath() {
        return "ci/resources/${getMainExtension()}.cfe"
    }
    static String getADDRepo() {
        return "https://jenkins@code.bit-erp.ru/scm/tool/add.git" 
    }
    static String getSikulixRepo() {
        return "https://jenkins@code.bit-erp.ru/scm/tool/sikulix.git" 
    }
    static String getBitlibsRepo() {
        return "https://jenkins@code.bit-erp.ru/scm/tool/vanessa_bit_libs.git" 
    }
    static String getAdmin1cUser() {
        return "Administrator"
    }
    static String getAdmin1cPwd() {
        return "111"
    }
    static String getMainExtension() {
        return "biterpExtension"
    }
    static String getTemplateFeaturesRepo() {
        return "https://jenkins@code.bit-erp.ru/scm/tool/template_features.git"
    }

    /////////////////
    // GENERATORS
    ////////////////

    static String generateTempUser(String user) {
        return "temp_${user}" // ели нужно будет, когда либо много временных пользователей нужно заменить на реальный генератор
    }
}
