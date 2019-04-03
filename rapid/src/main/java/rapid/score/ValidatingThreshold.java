package rapid.score;

import config.Database;
import utils.Utils;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;

public class ValidatingThreshold {
    /*public static double calculateThreshold(List<Double> randomVariableList) {
        double threshold = 1;
        double mean = Utils.mean(randomVariableList.toArray());
        double variance = Utils.variance(randomVariableList.toArray());
        double sd = Utils.standardDeviation(randomVariableList.toArray());

        if (Double.isNaN(variance))
            variance = 0;
        if (Double.isNaN(sd))
            sd = 0;

        *//*for (Double randomVariable : randomVariableList) {
            threshold = threshold * gaussianMLE(randomVariable, mean, variance);
        }*//*

//        return threshold;
//        return (mean);
//        return (mean + sd);
//        return 0.0;
        return mean;
    }*/

    private HashMap<String, Double> getThresholdMap(double alpha, double beta) {
        HashMap<String, List<Double>> propertyTPConfidenceListMap = getConfidenceListMap(alpha, beta, "oke_prop_true_positive");
        HashMap<String, List<Double>> propertyFPConfidenceListMap = getConfidenceListMap(alpha, beta, "oke_prop_false_positive");

        HashMap<String, Double> propertyThresholdMap = new HashMap<>();
        Set<String> properties = new HashSet<>();
        properties.addAll(propertyTPConfidenceListMap.keySet());
        properties.addAll(propertyFPConfidenceListMap.keySet());

        for (String property : properties) {
            List<Double> tpConfidences = new ArrayList<>();
            List<Double> fpConfidences = new ArrayList<>();

            if (propertyTPConfidenceListMap.containsKey(property))
                tpConfidences = propertyTPConfidenceListMap.get(property);
            if (propertyFPConfidenceListMap.containsKey(property))
                fpConfidences = propertyFPConfidenceListMap.get(property);

            propertyThresholdMap.put(property, calculateThresholdTPFP(tpConfidences, fpConfidences));
        }
        return propertyThresholdMap;
    }

    private static double calculateThresholdTPFP(List<Double> tpConfidences, List<Double> fpConfidences) {
        double posMean = Utils.mean(tpConfidences.toArray());
        double posVar = Utils.variance(tpConfidences.toArray());
        double posSD = Utils.standardDeviation(tpConfidences.toArray());

        double negMean = Utils.mean(fpConfidences.toArray());
        double negVar = Utils.variance(fpConfidences.toArray());
        double negSD = Utils.standardDeviation(fpConfidences.toArray());

        if (Double.isNaN(posMean)) posMean = 0;
        if (Double.isNaN(posVar)) posVar = 0;
        if (Double.isNaN(posSD)) posSD = 0;

        if (Double.isNaN(negMean)) negMean = 0;
        if (Double.isNaN(negVar)) negVar = 0;
        if (Double.isNaN(negSD)) negSD = 0;

        double posPrevMargin = posMean - posVar;
        double negFrontMargin = negMean + negVar;

        double threshold = (negFrontMargin + posPrevMargin) / 2;
        return threshold;
    }

    public static HashMap<String, List<Double>> getConfidenceListMap(double alpha, double beta, String table) {
        String selectQuery = String.format("select prop_uri, confidence FROM %s \n" +
                "                   WHERE alpha = %.1f and beta = %.1f " +
                "                   ORDER BY prop_uri;", table, alpha, beta);
        HashMap<String, List<Double>> propertyConfidenceListMap = new HashMap<>();
        Statement statement = null;
        try {
            statement = Database.databaseInstance.conn.createStatement();
            java.sql.ResultSet rs = statement.executeQuery(selectQuery);

            while (rs.next()) {
                String property = rs.getString("prop_uri");
                double confidence = rs.getDouble("confidence");

                List<Double> confidenceList;
                if (propertyConfidenceListMap.containsKey(property)) {
                    confidenceList = propertyConfidenceListMap.get(property);
                } else {
                    confidenceList = new ArrayList<>();
                }
                confidenceList.add(confidence);
                propertyConfidenceListMap.put(property, confidenceList);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return propertyConfidenceListMap;
    }

    public static void storePredictionScore(String table, double alpha, double beta,
                                            HashMap<String, List<HashMap<String, Double>>> propTruePosi) {

        for (String property : propTruePosi.keySet()) {
            for (HashMap<String, Double> scoreMap : propTruePosi.get(property)) {
                double confidence = scoreMap.get("confidence");
                double embedding = scoreMap.get("embedding");

                String insertQuery = String.format("INSERT INTO %s (prop_uri, " +
                        "alpha, beta, " +
                        "confidence, embedding) " +
                        "VALUES (?, " +
                        "?, ?, " +
                        "?, ?); ", table);
                PreparedStatement prepareStatement = null;
                try {
                    prepareStatement = Database.databaseInstance.conn.prepareStatement(insertQuery, Statement.RETURN_GENERATED_KEYS);
                    prepareStatement.setString(1, property);

                    prepareStatement.setDouble(2, alpha);
                    prepareStatement.setDouble(3, beta);

                    prepareStatement.setDouble(4, confidence);
                    prepareStatement.setDouble(5, embedding);

                    prepareStatement.executeUpdate();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static Set<String> getPropertiesHavingConfidence(double alpha, double beta) {
        String selectQuery = String.format("SELECT fp.prop_uri from oke_prop_false_positive fp " +
                "inner join oke_prop_true_positive tp ON fp.prop_uri = tp.prop_uri " +
                "where fp.alpha = %.1f and fp.beta = %.1f;", alpha, beta);
        Set<String> distinctProperties = new HashSet<>();
        Statement statement = null;
        try {
            statement = Database.databaseInstance.conn.createStatement();
            java.sql.ResultSet rs = statement.executeQuery(selectQuery);

            while (rs.next()) {
                distinctProperties.add(rs.getString("prop_uri"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return distinctProperties;
    }

    public void storeThresholds() {
        for (double alpha = 1; alpha <= 9; alpha++) {
            for (double beta = 1; beta <= 9; beta++) {
                HashMap<String, Double> propertyThresholdMap = getThresholdMap(alpha / 10.0, beta / 10.0);
                for (String property : propertyThresholdMap.keySet()) {
                    double threshold = propertyThresholdMap.get(property);
                    insertThreshold(alpha, beta, property, threshold);
                }
            }
        }
    }

    private void insertThreshold(double alpha, double beta, String property, double threshold) {
        String insertQuery = "INSERT INTO `validating_threshold` (prop_uri, alpha, beta, threshold) " +
                "VALUES (?, ?, ?, ?);";
        PreparedStatement prepareStatement = null;
        try {
            prepareStatement = Database.databaseInstance.conn.prepareStatement(insertQuery, Statement.RETURN_GENERATED_KEYS);
            prepareStatement.setString(1, property);
            prepareStatement.setDouble(2, alpha / 10.0);
            prepareStatement.setDouble(3, beta / 10.0);
            prepareStatement.setDouble(4, threshold);

            prepareStatement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static double getThreshold(double alpha, double beta, String property) {
        String selectQuery = String.format("SELECT threshold from validating_threshold " +
                "where prop_uri = \"%s\" AND alpha = %.1f and beta = %.1f ;", property, alpha, beta);
        double threshold = 0.0;
        Statement statement = null;
        try {
            statement = Database.databaseInstance.conn.createStatement();
            java.sql.ResultSet rs = statement.executeQuery(selectQuery);

            while (rs.next()) {
                threshold = rs.getDouble("threshold");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return threshold;
    }

    public static void main(String[] args) {
        ValidatingThreshold vt = new ValidatingThreshold();
        vt.storeThresholds();
    }

}
