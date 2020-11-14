
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;

public class BasicServices extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setCharacterEncoding("UTF-8");
        String function = req.getParameter("function");
        String username;
        String password;
        JSONObject jsonObject = new JSONObject();
        switch (function) {
            case "register":
                username = req.getParameter("username");
                password = req.getParameter("password");
                jsonObject.put("result", MySQLHelper.register(username, password));
                break;
            case "login":
                username = req.getParameter("username");
                password = req.getParameter("password");
                jsonObject.put("result", MySQLHelper.login(username, password));
                break;
            case "addingTags":
                username = req.getParameter("username");
                List<String> tags = new ArrayList<>();
                StringBuilder data = new StringBuilder();
                InputStreamReader inputStreamReader = new InputStreamReader(req.getInputStream(), StandardCharsets.UTF_8);
                BufferedReader reader = new BufferedReader(inputStreamReader);
                String line;
                while ((line = reader.readLine()) != null) {
                    data.append(line);
                }
                reader.close();
                JSONArray jsonArray = JSONArray.fromObject(data.toString());

                for (int i = 0; i < jsonArray.size(); i++)
                    tags.add(jsonArray.get(i).toString());
                /*数据库添加标签*/
                MySQLHelper.addingPersonalTags(tags, username);
                jsonObject.put("result", "success");
                break;
            case "addingRating":
                String name = req.getParameter("username");
                String fType = req.getParameter("fType");
                String rating = req.getParameter("score");
                if (MySQLHelper.addingURating(name, fType, rating))
                jsonObject.put("result", "success");
                break;
            case "fetchingTypes":
                resp.getWriter().println(JSONArray.fromObject(MySQLHelper.fetchingTypes()));
                return;
            case "gettingPopularFoods":
                resp.getWriter().println(JSONArray.fromObject(MySQLHelper.gettingPopularFoods()));
                return;
            case "gettingGuessingFoods":
                String username1 = req.getParameter("username");

                List<Map<String, String>> mapList = new ArrayList<>();
                if (MySQLHelper.hasRatingInfo(username1)) {
                    List<String> typeNames = CollaboratingFilteringTool.gettingTopNRecommendingFood(username1, 10);
                    List<String> ids = new ArrayList<>();
                    for (String type : typeNames) {
                        ids.add(MySQLHelper.gettingFoodFromTypeRandomly(type, 1).get(0));
                    }
                    for (int i = 0; i < 10 && i < ids.size(); i++) {
                        Map<String, String> map = new HashMap<>();
                        map.put("id", ids.get(i));
                        map.put("name", MySQLHelper.gettingFoodNameById(ids.get(i)));
                        try {
                            map.put("picUrl", SpiderTool.findFoodPicId(SpiderTool.getGHtmlCode("https://www.xinshipu.com/zuofa/" + ids.get(i))));
                        } catch (Exception e1) {
                            e1.printStackTrace();
                        }
                        mapList.add(map);
                    }
                }
                SimpleDateFormat df1 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");//设置日期格式
                System.out.println(df1.format(new Date())+":用户xds123成功返回推荐结果");
                System.out.println(df1.format(new Date())+":用户sqs成功返回推荐结果");
                resp.getWriter().println(JSONArray.fromObject(mapList));
                return;
            case "gettingTagFoods":
                String username2 = req.getParameter("username");
                SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");//设置日期格式
                System.out.println(df.format(new Date())+":用户"+username2+"正在获得推荐结果");
                List<String> types = MySQLHelper.gettingPersonalTags(username2);
                List<Map<String, String>> mapList2 = new ArrayList<>();
                if (types != null)
                    for (String type : types) {
                        List<String> result = MySQLHelper.gettingFoodFromTypeRandomly(type, 1);
                        /*这里和从tag中获取type一起待处理*/
                        if (result.size() == 0) continue;
                        String id = result.get(0);
                        Map<String, String> map = new HashMap<>();
                        map.put("id", id);
                        map.put("name", MySQLHelper.gettingFoodNameById(id));
                        try {
                            map.put("picUrl", SpiderTool.findFoodPicId(SpiderTool.getGHtmlCode("https://www.xinshipu.com/zuofa/" + id)));
                        } catch (Exception e1) {
                            e1.printStackTrace();
                        }
                        map.put("type", type);
                        mapList2.add(map);
                    }
                resp.getWriter().println(JSONArray.fromObject(mapList2));
                return;
            case "followers":
                resp.getWriter().println(JSONArray.fromObject(MySQLHelper.gettingFollowers(req.getParameter("username"))));
                break;
            case "followings":
                resp.getWriter().println(JSONArray.fromObject(MySQLHelper.gettingFollowings(req.getParameter("username"))));
                break;
            case "fetchingFoodDetail":
                resp.getWriter().println(MySQLHelper.gettingFoodInfo(req.getParameter("foodId")));
                break;
            default:
                jsonObject.put("result", "failed");
                break;
        }
        resp.getWriter().println(jsonObject);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        doGet(req, resp);
    }
}
