package io.bit

import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL
import net.sf.json.JSONObject
import groovy.json.JsonSlurper

public class SlackHJava
{
    private static JSONObject exec(String api, String token, String method, JSONObject body = null){
        URL url = new URL(BITConvJava.SLACK_URL + api)

        HttpURLConnection con = (HttpURLConnection) url.openConnection()
        con.setRequestMethod(method)
        con.setRequestProperty("Content-Type", "application/json")
        con.setRequestProperty("Authorization", "Bearer " + token)

        if(body != null){
            con.setDoOutput(true)
            OutputStream os = con.getOutputStream()
            os.write(body.toString().getBytes("UTF-8"))
            os.close()
        }

        InputStream inputStream = con.getErrorStream()
        if (inputStream == null) {
            inputStream = con.getInputStream()
        }
        def result = new JsonSlurper().parse(inputStream)
        con.disconnect()
        return result
    }

    public static JSONObject getMembers(){
        return exec("users.list", BITConvJava.SLACK_APP_TOKEN, "POST")
    }

    public static JSONObject getMemberByEmail(String email){
        return exec("users.lookupByEmail?email=" + email, BITConvJava.SLACK_APP_TOKEN, "POST")
    }

    public static JSONObject invite(String email, String firstName, String lastName, Boolean isGuest, List<String> channels){
        String url = "users.admin.invite"
        url += "?email="+email
        url += "&first_name="+URLEncoder.encode(firstName, "UTF-8")
        url += "&last_name="+URLEncoder.encode(lastName, "UTF-8")
        if(channels != null){
            url += "&channels="+String.join(',', channels)
        }
        url += "&ultra_restricted=" + isGuest.toString()
        return exec(url, BITConvJava.SLACK_TEST_TOKEN, "POST")
    }
    public static JSONObject getChannels(Boolean exclude_archived = false, List<String> types = null){
        String url = "conversations.list"
        url += "?exclude_archived=" + exclude_archived.toString()
        url += "&limit=999"
        if(types != null){
            url += "&types="+String.join(',', types)
        }
        return exec(url, BITConvJava.SLACK_APP_TOKEN, "POST")
    }
    public static JSONObject createChannel(String name, Boolean isPrivate = false){
        String url = "conversations.create"
        JSONObject o = new JSONObject()
        o.put("name", name)
        o.put("is_private", isPrivate)
        return exec(url, BITConvJava.SLACK_APP_TOKEN,"POST", o)

    }
    public static JSONObject getChannel(String id){
        String url = "conversations.info"
        url += "?channel="+id
        return exec(url, BITConvJava.SLACK_APP_TOKEN, "POST")
    }
    public static JSONObject inviteToConv(String channelId, List<String> userIDs){
        String url = "conversations.invite"
        JSONObject o = new JSONObject()
        o.put("channel", channelId)
        o.put("users", String.join(",", userIDs))
        return exec(url, BITConvJava.SLACK_APP_TOKEN,"POST", o)
    }
    public static JSONObject getConvMembers(String id){
        String url = "conversations.members"
        url += "?channel="+id
        return exec(url, BITConvJava.SLACK_APP_TOKEN, "POST")
    }
    public static JSONObject leaveConv(String channelId){
        String url = "conversations.leave"
        url += "?channel="+channelId
        return exec(url, BITConvJava.SLACK_APP_TOKEN, "POST")

    }

    public static JSONObject sendMessage(String channel, String text, String attachments, String threadId = null, String asUser = null){
        String url = "chat.postMessage"
        JSONObject o = new JSONObject()
        o.put("channel", channel)
        o.put("text", text)
        if(asUser != null){
            o.put("as_user", true)
            o.put("username", asUser)
        }else{
            o.put("as_user", false)
        }
        o.put("attachments", attachments)
        if(threadId != null){
            o.put("thread_ts", threadId)
        }
        return exec(url, BITConvJava.SLACK_APP_TOKEN, "POST", o)
    }

    public static JSONObject updateMessage(String channel, String text, String attachments, String threadId = null, String asUser = null){
            String url = "chat.update"
            JSONObject o = new JSONObject()
            o.put("channel", channel)
            o.put("text", text)
            if(asUser != null){
                o.put("as_user", true)
                o.put("username", asUser)
            }else{
                o.put("as_user", false)
            }
            o.put("attachments", attachments)
            o.put("ts", threadId)
            return exec(url, BITConvJava.SLACK_APP_TOKEN, "POST", o)
    }

    public static JSONObject disableUser(String id){
        String url = "users.admin.setInactive"
        url += "?user="+id
        return exec(url, BITConvJava.SLACK_TEST_TOKEN, "POST")
    }

    public static void sendHttpRequestJenkins(String urlString, String body) throws IOException {
        URL url = new URL(urlString);
        HttpURLConnection con =  (HttpURLConnection) url.openConnection();
        con.setRequestMethod("POST");
        con.setRequestProperty("Content-Type", "application/xml");
        con.setRequestProperty("Authorization", "Basic amVua2luczpeMmtrUjkybVMpUlM=");

        if(body != null){
            con.setDoOutput(true);
            OutputStream os = con.getOutputStream();
            os.write(body.toString().getBytes("UTF-8"));
            os.close();
        }

        InputStream inputStream = con.getErrorStream();
        if (inputStream == null) {
            inputStream = con.getInputStream();
        }
        con.disconnect();
    }

    @NonCPS
    public static String getHttpRequestJenkins(String urlString, String charset) throws IOException {

        URL url = new URL(urlString);
        HttpURLConnection con =  (HttpURLConnection) url.openConnection();
        con.setRequestMethod("GET");
        con.setRequestProperty("Content-Type", "application/xml");
        con.setRequestProperty("Authorization", "Basic amVua2luczpeMmtrUjkybVMpUlM=");

        InputStream inputStream = con.getErrorStream();
        String result = "";
        if (inputStream == null) {
            inputStream = con.getInputStream();
            result = convert(inputStream, charset);
        }

        con.disconnect();
        return result;

    }

    @NonCPS
    public static String convert(InputStream inputStream, String charset) throws IOException {

        StringBuilder stringBuilder = new StringBuilder();
        String line = null;

        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream, charset))
        while ((line = bufferedReader.readLine()) != null) {
            stringBuilder.append(line);
        }

        return stringBuilder.toString();
    }

}
