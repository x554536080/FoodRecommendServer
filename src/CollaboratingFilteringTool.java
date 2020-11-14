import org.ejml.alg.dense.misc.ImplCommonOps_Matrix64F;
import org.ejml.data.DenseMatrix64F;
import org.ejml.ops.CommonOps;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

public class CollaboratingFilteringTool {
    static private int numTypes;
    static private int numUsers;
    static List<String> types;
    static List<String> users;


    static DenseMatrix64F ratingMatrix;


    static {
        types = new ArrayList<>();
        users = new ArrayList<>();
        try {
            ResultSet resultSet = MySQLHelper.connection.createStatement().executeQuery("select * from ftypes");
            while (resultSet.next())
                types.add(resultSet.getString(1));
            numTypes = types.size();
            resultSet = MySQLHelper.connection.createStatement().executeQuery("select * from users");
            while (resultSet.next())
                users.add(resultSet.getString(1));
            numUsers = users.size();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        initRatingMatrix();
    }

    static void initRatingMatrix() {
        ratingMatrix = new DenseMatrix64F(numUsers, numTypes);
        boolean hasRating;
        int rating = 0;
        for (int i = 0; i < numUsers; i++) {
            for (int j = 0; j < numTypes; j++) {
                /*如果存在评分信息，设置值为评分值*/
                try {
                    ResultSet resultSet = MySQLHelper.connection.createStatement().executeQuery(
                            "select rating from urating where username = " + "\"" + users.get(i) +
                                    "\"" + " and ftype = " + "\"" + types.get(j) + "\"");
                    resultSet.next();
                    rating = Integer.parseInt(resultSet.getString(1));
                    hasRating = true;
                } catch (SQLException e) {
                    hasRating = false;
                }
                if (hasRating)
                    ratingMatrix.set(i, j, rating);
                    /*没有评分则为0*/
                else ratingMatrix.set(i, j, 0);
            }
        }
//        System.out.println(ratingMatrix);
    }

    /*从rating矩阵中找到target用户的前n个相似用户及其相似度*/
    static List<Map<String, Object>> findingTopNSimilarUser(String targetUId, int n) {
        List<Map<String, Object>> userSimilarity = new ArrayList<>();
        List<String> userFriends = new ArrayList<>();
        try { ResultSet resultSet = MySQLHelper.connection.createStatement().executeQuery(
                    "select * from friends where uid = " + "\"" + targetUId + "\"");
            while (resultSet.next()) userFriends.add(resultSet.getString("fid"));
        } catch (SQLException e) {
            e.printStackTrace(); }
        DenseMatrix64F[] dataVectors = new DenseMatrix64F[numUsers];
        CommonOps.rowsToVector(ratingMatrix, dataVectors);
        /*从rating矩阵中找到target用户的前n个相似用户及其相似度*/
        for (String user : users) {
            if (user.equals(targetUId)) continue;
            int upper = 0;//分子
            for (int i = 0; i < numTypes; i++) {
                upper += dataVectors[users.indexOf(user)]
                        .get(i, 0) * dataVectors[users.indexOf(targetUId)].get(i, 0);
            }
            double lower = 0;int left = 0;int right = 0;
            for (int i = 0; i < dataVectors[users.indexOf(user)].numRows; i++) {
                left += dataVectors[users.indexOf(user)]
                        .get(i, 0) * dataVectors[users.indexOf(user)].get(i, 0);
            }
            for (int i = 0; i < dataVectors[users.indexOf(targetUId)].numRows; i++) {
                right += dataVectors[users.indexOf(targetUId)]
                        .get(i, 0) * dataVectors[users.indexOf(targetUId)].get(i, 0);
            }
            lower = Math.sqrt(left) * Math.sqrt(right);
            double result = upper / lower;
            if (lower == 0) { result = 0; }
            /*加上好友信息*/
            double resultWithSocialInfo;
            if (userFriends.contains(user)) {
                resultWithSocialInfo = (result + 1) / 2;
            } else resultWithSocialInfo = result / 2;


            HashMap<String, Object> similarity = new HashMap<>();
            similarity.put("id", user);
            similarity.put("sim", resultWithSocialInfo);
            userSimilarity.add(similarity);

        }


        Collections.sort(userSimilarity, new Comparator<Map<String, Object>>() {
            public int compare(Map<String, Object> o1, Map<String, Object> o2) {
                Double name1 = Double.valueOf(o1.get("sim").toString());//name1是从你list里面拿出来的一个
                Double name2 = Double.valueOf(o2.get("sim").toString()); //name1是从你list里面拿出来的第二个name
                return name2.compareTo(name1);
            }
        });

        List<Map<String, Object>> similarUsers = new ArrayList<>();
        for (int i = 0; i < n && i < userSimilarity.size() && (double) userSimilarity.get(i).get("sim") != 0; i++) {
            similarUsers.add(userSimilarity.get(i));
        }
        return similarUsers;
    }

    static List<String> gettingTopNRecommendingFood(String targetUId, int n) {
        int numNearNeighbors = 20;
        List<Map<String, Object>> similarUsers = findingTopNSimilarUser(targetUId, numNearNeighbors);
        List<Map<String, Object>> foodPreScores = new ArrayList<>();
        List<String> recommendingFoods = new ArrayList<>();
        List<String> existingScoreItem = new ArrayList<>();
        try {
            ResultSet resultSet = MySQLHelper.connection.createStatement().executeQuery(
                    "select * from urating where username = " + "\"" + targetUId + "\"");
            while (resultSet.next()) existingScoreItem.add(resultSet.getString("ftype"));
        } catch (SQLException e) {
            e.printStackTrace();
        }

        /*对目标用户的每一个食物进行预测评分，已有评分则跳过*/
        for (String foodType : types) {
            if (existingScoreItem.contains(foodType)) continue;
            double preScore = 0;
            for (Map<String, Object> similarUser : similarUsers) {
                int score = 0;
                try { ResultSet resultSet = MySQLHelper.connection.createStatement().executeQuery(
                            "select * from urating where username = " + "\"" + similarUser.get("id") + "\" "
                                    + "and ftype = " + "\"" + foodType + "\"");
                    while (resultSet.next()) score = Integer.parseInt(resultSet.getString("rating"));
                } catch (SQLException e) {
                    score = 0; }preScore += (double) similarUser.get("sim") * score; }
            if (!(similarUsers.size() == 0))
                preScore /= similarUsers.size();
            HashMap<String, Object> foodScore = new HashMap<>();
            foodScore.put("id", foodType);foodScore.put("score", preScore);foodPreScores.add(foodScore);
        }

        Collections.sort(foodPreScores, new Comparator<Map<String, Object>>() {
            public int compare(Map<String, Object> o1, Map<String, Object> o2) {
                Double name1 = Double.valueOf(o1.get("score").toString());//name1是从你list里面拿出来的一个
                Double name2 = Double.valueOf(o2.get("score").toString()); //name1是从你list里面拿出来的第二个name
                return name2.compareTo(name1);
            }
        });
        for (int i = 0; i < n && i < foodPreScores.size() && (double) foodPreScores.get(i).get("score") != 0; i++) {
            recommendingFoods.add(foodPreScores.get(i).get("id").toString());
        }
        return recommendingFoods;
    }

    public static void main(String[] args) {
//        System.out.println(new DenseMatrix64F(1, numTypes) );
        double a = 0;
        a /= 9;
        System.out.println(a);
    }

    static void testMatrix() {

        /*c参数分别为行row和列column数*/
        DenseMatrix64F L = new DenseMatrix64F(3, 3); //初始化一个矩阵，并进行下面的赋值
        {
            L.set(0, 0, 4.0);
            L.set(0, 1, 13.0);
            L.set(0, 2, -16.0);
            L.set(1, 0, 12.0);
            L.set(1, 1, 37.0);
            L.set(1, 2, -43.0);
            L.set(2, 0, -16.0);
            L.set(2, 1, -43.0);
            L.set(2, 2, 98.0);
        }
        System.out.println("data为:");
        System.out.println(L);
        DenseMatrix64F[] dataVectors = new DenseMatrix64F[3]; //初始化一个矩阵，并进行下面的赋值
        /*将矩阵按row或column分解成为对应数目的一维向量*/
        CommonOps.rowsToVector(L, dataVectors);/*rowsTo指的是每一row成为一个矩阵，矩阵只有一列，即将行向量表示为列向量*/
        System.out.println(dataVectors[0]);
        System.out.println(dataVectors[1]);
        System.out.println(dataVectors[2]);
        /*获得每一个列向量的均值，传递的参数是矩阵的列向量表示的行向量*/
        DenseMatrix64F meanOfEachRow = getSampleMean(dataVectors);
        System.out.println("每列数据的均值为:" + meanOfEachRow);

        DenseMatrix64F[] dataVectors2 = new DenseMatrix64F[3];
        CommonOps.columnsToVector(L, dataVectors2);
        DenseMatrix64F meanOfEachColumn = getSampleMean(dataVectors2);
        System.out.println("每行数据的均值为:" + meanOfEachColumn);

        DenseMatrix64F sigma_0 = CommonOps.identity(3); //设置单位阵
        System.out.println("单位阵为:" + sigma_0);

        DenseMatrix64F add = new DenseMatrix64F(3, 3);  //矩阵对应元素相加
        CommonOps.add(L, L, add);
        System.out.println("两个矩阵相加的值为:" + add);

        DenseMatrix64F sub = new DenseMatrix64F(3, 3);  //矩阵减法
        CommonOps.sub(L, sigma_0, sub);
        ;
        System.out.println("两个矩阵相减的值为:" + sub);

    }

    public static DenseMatrix64F getSampleMean(DenseMatrix64F[] data) {
        DenseMatrix64F mean = new DenseMatrix64F(3, 1);//initialized to 0
        for (DenseMatrix64F vec : data)
            /*将每一个列向量的元素加至mean的每一行的元素*/
            CommonOps.addEquals(mean, vec);
        /*每行的元素除以之前列向量的维数*/
        CommonOps.divide(3, mean);
        return mean;
    }

}