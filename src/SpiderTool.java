import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SpiderTool {
    static String getGHtmlCode(String httpUrl) throws Exception {
        String content = "";
        URL url = new URL(httpUrl);
        URLConnection connection = url.openConnection();
        connection.setConnectTimeout(5000);
        connection.setReadTimeout(20000);
        BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        String input;
        while ((input = reader.readLine()) != null) {
            content += input;
        }
        reader.close();
        return content;
    }

    static String findFoodPicId(String input) {
        String regex = "image\":\\s+\"([^\"]*)";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(input);
        if (matcher.find()) {
            return matcher.group(1);
        } else return null;
    }

}
