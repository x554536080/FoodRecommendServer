import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import java.sql.*;
import java.util.*;

public class MySQLHelper {
    static Connection connection = null;
    static PreparedStatement prepareStatementForRegister = null;
    static PreparedStatement prepareStatementForLogin = null;
    static PreparedStatement preparedStatementForFetchingAllTypes = null;
    static PreparedStatement preparedStatementForGettingFoodNameById = null;
    static PreparedStatement preparedStatementForGettingPopularIds = null;
    static PreparedStatement preparedStatementForAddingPersonalTags = null;
    static PreparedStatement preparedStatementForGettingFollowers = null;
    static PreparedStatement preparedStatementForGettingFollowings = null;
    static PreparedStatement preparedStatementForGettingFoodFromTypeRandomly = null;
    static PreparedStatement preparedStatementForGettingPersonalTags = null;
    static PreparedStatement preparedStatementForGettingFoodInfo = null;
    static PreparedStatement preparedStatementForAddingURating = null;



    static {
        try {
            String driver = "com.mysql.cj.jdbc.Driver";
            Class.forName(driver);
            String url = "jdbc:mysql://localhost:3306/food_recommend?serverTimezone=UTC";
            String user = "root";
            String password = "554536080";
            connection = DriverManager.getConnection(url, user, password);
            if (!connection.isClosed()) {
                System.out.println("数据库成功连接");
            }
            String register = "insert into users (username,password) values (?,?)";
            String login = "select * from users where username = ?";
            String fetchingTypes = "select typename from ftypes";
            String gettingFoodNameById = "select fname from foods where fid = ?";
            String gettingPopularIds = "select fid from views order by times desc limit 10";
            String addingPersonalTags = "insert into utags (username,tagname) values (?,?)";
            String gettingFollowings = "select * from friends where uid = ?";
            String gettingFollowers = "select * from friends where fid = ?";
            String gettingFoodRandomly = "select * from foods where ftype = ? order by rand() desc limit ?";
            String gettingPersonalTags = "select * from utags where username = ?";
            String gettingFoodInfo = "select * from foods where fid = ?";
            String addingURating = "insert into urating (username,ftype,rating) values (?,?,?)";

            preparedStatementForGettingFollowers = connection.prepareStatement(gettingFollowers);
            preparedStatementForGettingFollowings = connection.prepareStatement(gettingFollowings);
            prepareStatementForLogin = connection.prepareStatement(login);
            prepareStatementForRegister = connection.prepareStatement(register);
            preparedStatementForFetchingAllTypes = connection.prepareStatement(fetchingTypes);
            preparedStatementForGettingFoodNameById = connection.prepareStatement(gettingFoodNameById);
            preparedStatementForGettingPopularIds = connection.prepareStatement(gettingPopularIds);
            preparedStatementForAddingPersonalTags = connection.prepareStatement(addingPersonalTags);
            preparedStatementForGettingFoodFromTypeRandomly = connection.prepareStatement(gettingFoodRandomly);
            preparedStatementForGettingPersonalTags = connection.prepareStatement(gettingPersonalTags);
            preparedStatementForGettingFoodInfo = connection.prepareStatement(gettingFoodInfo);
            preparedStatementForAddingURating = connection.prepareStatement(addingURating);

        } catch (
                Exception e1) {
            e1.printStackTrace();
        }
    }

    static boolean addingURating(String username,String fType,String score){
        try {
            preparedStatementForAddingURating.setString(1,username);
            preparedStatementForAddingURating.setString(2,fType);
            preparedStatementForAddingURating.setString(3,score);
            preparedStatementForAddingURating.executeUpdate();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    static JSONObject gettingFoodInfo(String fid) {
        try {
            preparedStatementForGettingFoodInfo.setString(1, fid);
            ResultSet resultSet = preparedStatementForGettingFoodInfo.executeQuery();
            ResultSet resultSet2 = connection.createStatement().executeQuery("select count(*) from frating where fid = " + fid);

            JSONObject foodInfo = new JSONObject();
            resultSet.next();
            String score = resultSet.getString("fscore");
            if (score == null) {
                foodInfo.put("score", "no score");
            } else
                foodInfo.put("score", score);
            resultSet2.next();
            foodInfo.put("scoreNum", resultSet2.getString(1));
            String tagString = resultSet.getString("ftag");
            tagString = tagString.substring(0, tagString.length() - 1);
            String[] arr = tagString.split("%");
            List<String> tags = new ArrayList<>();
            for (String str : arr) {
                tags.add(str);
            }
            tags.add(0, resultSet.getString("ftype"));
            foodInfo.put("tags", JSONArray.fromObject(tags));
            foodInfo.put("des", resultSet.getString("fintro"));
            return foodInfo;
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }

    }

    static boolean hasRatingInfo(String username) {
        List<String> ratings = new ArrayList<>();
        try {
            ResultSet resultSet = connection.createStatement().executeQuery("select * from urating where username = " + "\"" + username + "\"");
            while (resultSet.next())
                ratings.add(resultSet.getString("rating"));
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return !ratings.isEmpty();
    }

    /*从tag中找type就放到下次登陆呈现吧*/
    static List<String> gettingFoodFromTypeRandomly(String type, int number) {
        List<String> fIds = new ArrayList<>();
        try {
            preparedStatementForGettingFoodFromTypeRandomly.setString(1, type);
            preparedStatementForGettingFoodFromTypeRandomly.setInt(2, number);
            ResultSet resultSet = preparedStatementForGettingFoodFromTypeRandomly.executeQuery();
            while (resultSet.next()) {
                fIds.add(resultSet.getString("fid"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return fIds;
    }

    static List<String> gettingPersonalTags(String username) {
        ArrayList<String> tags = new ArrayList<>();
        try {
            preparedStatementForGettingPersonalTags.setString(1, username);
            ResultSet resultSet = preparedStatementForGettingPersonalTags.executeQuery();
            while (resultSet.next()) {
                tags.add(resultSet.getString("tagname"));
            }
            return tags;
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    static List<String> gettingFollowers(String username) {
        ArrayList<String> users = new ArrayList<>();
        try {
            preparedStatementForGettingFollowers.setString(1, username);
            ResultSet resultSet = preparedStatementForGettingFollowers.executeQuery();
            while (resultSet.next()) {
                users.add(resultSet.getString("uid"));
            }
            return users;
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    static List<String> gettingFollowings(String username) {
        ArrayList<String> users = new ArrayList<>();
        try {
            preparedStatementForGettingFollowings.setString(1, username);
            ResultSet resultSet = preparedStatementForGettingFollowings.executeQuery();
            while (resultSet.next()) {
                users.add(resultSet.getString("fid"));
            }
            return users;
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    static void addingPersonalTags(List<String> tags, String username) {
        for (String tag : tags) {
            try {
                preparedStatementForAddingPersonalTags.setString(1, username);
                preparedStatementForAddingPersonalTags.setString(2, tag);
                preparedStatementForAddingPersonalTags.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    static List<Map<String, String>> gettingPopularFoods() {
        List<Map<String, String>> mapList = new ArrayList<>();
        try {
            List<String> ids = new ArrayList<>();
            ResultSet resultSet = preparedStatementForGettingPopularIds.executeQuery();
            while (resultSet.next()) {
                ids.add(resultSet.getString("fid"));
            }
            for (int i = 0; i < 10 && i < ids.size(); i++) {
                Map<String, String> map = new HashMap<>();
                map.put("id", ids.get(i));
                map.put("name", gettingFoodNameById(ids.get(i)));
                map.put("picUrl", SpiderTool.findFoodPicId(SpiderTool.getGHtmlCode("https://www.xinshipu.com/zuofa/" + ids.get(i))));
                mapList.add(map);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return mapList;
    }


    static List<String> fetchingTypes() {
        try {
            ResultSet resultSet = preparedStatementForFetchingAllTypes.executeQuery();
            List<String> results = new ArrayList<>();
            while (resultSet.next()) {
                results.add(resultSet.getString("typename"));
            }
            return results;
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    static String gettingFoodNameById(String id) {
        try {
            preparedStatementForGettingFoodNameById.setString(1, id);

            ResultSet resultSet = preparedStatementForGettingFoodNameById.executeQuery();
            resultSet.next();
            return resultSet.getString("fname");
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    static String register(String username, String password) {
        try {
            prepareStatementForRegister.setString(1, username);
            prepareStatementForRegister.setString(2, password);
            prepareStatementForRegister.executeUpdate();
            return "success";
        } catch (SQLIntegrityConstraintViolationException e) {
            e.printStackTrace();
            return "failed";
        } catch (SQLException e) {
            e.printStackTrace();
            return "failed";
        }
    }

    static String login(String username, String password) {
        try {
            prepareStatementForLogin.setString(1, username);
            ResultSet resultSet = prepareStatementForLogin.executeQuery();
            resultSet.next();
            if (resultSet.getString("password").equals(password))
                return "success";
            else return "failed";
        } catch (SQLException e) {
            e.printStackTrace();
            return "failed";
        }
    }

    public static void main(String[] args) {
        System.out.println(gettingFoodInfo("41"));
    }
}
