package rapid.score;

import config.Database;

import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Set;

/**
 * @author DANISH AHMED on 2/26/2019
 */
public class ScoreHelper {
    private HashMap<String, Integer> propertyPatternCountMap = new LinkedHashMap<>();
    private HashMap<String, HashMap<String, HashMap<String, String>>> propertyPatternFreqSGMap = new HashMap<>();
    private HashMap<String, HashMap<String, String>> propertyExtendPatternMap = new HashMap<>();

    public static ScoreHelper scoreHelperInstance;
    static {
        scoreHelperInstance = new ScoreHelper();
    }

    private ScoreHelper() {
        setPropertyPatternCountMap();
        setPropertyPatternFreqSGMap();
        setPropertyPatternsExtendMap();
    }

    public HashMap<String, Integer> getPropertyPatternCountMap() {
        return propertyPatternCountMap;
    }

    public HashMap<String, HashMap<String, String>> getPatternsFreqAndSGPrettyForProperty(String property) {
        return propertyPatternFreqSGMap.get(property);
    }

    public HashMap<String, HashMap<String, String>> getPropertyExtendPatternMap() {
        return propertyExtendPatternMap;
    }

    private Set<String> getPropertiesHavingTrainedPatterns() {
        String selectQuery = "SELECT DISTINCT prop_uri from pattern_stats;";
        Set<String> properties = new HashSet<>();
        Statement statement = null;
        try {
            statement = Database.databaseInstance.conn.createStatement();
            java.sql.ResultSet rs = statement.executeQuery(selectQuery);

            while (rs.next()) {
                properties.add(rs.getString("prop_uri"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return properties;
    }

    private void setPropertyPatternsExtendMap() {
        Set<String> properties = getPropertiesHavingTrainedPatterns();
        for (String property : properties) {
            String selectQuery = String.format("select distinct pattern_extend, pattern from property_pattern where prop_uri = \"%s\";", property);

            HashMap<String, String> extendedPatterns = new HashMap<>();
            Statement statement = null;
            try {
                statement = Database.databaseInstance.conn.createStatement();
                java.sql.ResultSet rs = statement.executeQuery(selectQuery);

                while (rs.next()) {
                    extendedPatterns.put(rs.getString("pattern_extend"), rs.getString("pattern"));
                }
                statement.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
            propertyExtendPatternMap.put(property, extendedPatterns);
        }

    }

    private void setPropertyPatternFreqSGMap() {
        Set<String> properties = getPropertiesHavingTrainedPatterns();
        for (String property : properties) {
            if (!propertyPatternFreqSGMap.containsKey(property)) {
                String query = String.format("select pattern, sg_pretty, support, specificity, occur_prop, occur_pattern_freq from pattern_stats where prop_uri = \"%s\";", property);
                HashMap<String, HashMap<String, String>> patternFreqSGMap = new LinkedHashMap<>();
                Statement statement = null;
                try {
                    statement = Database.databaseInstance.conn.createStatement();
                    java.sql.ResultSet rs = statement.executeQuery(query);

                    while (rs.next()) {
                        HashMap<String, String> freqSGMap = new HashMap<>();

                        String sgPretty = rs.getString("sg_pretty");
                        sgPretty = removeWordsFromSGPretty(sgPretty);
                        int support = rs.getInt("support");
                        int specificity = rs.getInt("specificity");
                        int occurProp = rs.getInt("occur_prop");
                        int occurPatternFreq = rs.getInt("occur_pattern_freq");

                        freqSGMap.put("support", String.valueOf(support));
                        freqSGMap.put("specificity", String.valueOf(specificity));
                        freqSGMap.put("occurProp", String.valueOf(occurProp));
                        freqSGMap.put("occurPatternFreq", String.valueOf(occurPatternFreq));
                        freqSGMap.put("sgPretty", sgPretty);

                        patternFreqSGMap.put(rs.getString("pattern"), freqSGMap);
                    }
                    statement.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
                propertyPatternFreqSGMap.put(property, patternFreqSGMap);
            }
        }
    }

    private void setPropertyPatternCountMap() {
        String supportQuery = "SELECT prop_uri, count(id_prop_pattern) as pcount from property_pattern group by prop_uri;";
        Statement statement = null;
        try {
            statement = Database.databaseInstance.conn.createStatement();
            java.sql.ResultSet rs = statement.executeQuery(supportQuery);

            while (rs.next()) {
                propertyPatternCountMap.put(rs.getString("prop_uri"), rs.getInt("pcount"));
            }
            statement.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static String removeWordsFromSGPretty(String sgPretty) {
        String[] split = sgPretty.split("/");
        StringBuilder removedSG = new StringBuilder();
        for (String part : split) {
            if (part.contains(">")) {
                String[] partSplit = part.split(">");
                removedSG.append(partSplit[0]).append(">");
                if (partSplit[1] != null && partSplit[1].contains("["))
                    removedSG.append("[");
            } else if (part.contains("[")) {
                removedSG.append("[");
            } else if (part.contains("]")) {
                removedSG.append(part);
            }
        }
        return removedSG.toString();
    }
}
