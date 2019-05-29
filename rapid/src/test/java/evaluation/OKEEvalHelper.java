package evaluation;

import config.Database;

import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

/**
 * @author DANISH AHMED on 3/24/2019
 */
public class OKEEvalHelper {
    private HashMap<Integer, HashMap<String, String>> patternIdEntitiesMap = new HashMap<>();
    private HashMap<Integer, HashMap<String, Set<HashMap<String, String>>>> sentIdPropertyEntitiesMap = new HashMap<>();
    private HashMap<Integer, Set<Integer>> sentIdPatternsMap = new HashMap<>();

    public static OKEEvalHelper evalHelper;
    static {
        evalHelper = new OKEEvalHelper();
    }

    public HashMap<Integer, HashMap<String, String>> getPatternIdEntitiesMap() {
        return patternIdEntitiesMap;
    }

    public HashMap<Integer, HashMap<String, Set<HashMap<String, String>>>> getSentIdPropertyEntitiesMap() {
        return sentIdPropertyEntitiesMap;
    }

    public HashMap<Integer, Set<Integer>> getSentIdPatternsMap() {
        return sentIdPatternsMap;
    }

    private OKEEvalHelper() {
        setPatternEntitiesMap();
        setOKESentencePropertyEntitiesMap();
        sentencePatternsMap();
    }

    private void setPatternEntitiesMap() {
        String selectQuery = "select ep.id_oke_coref_pattern, ec.subj, ec.obj FROM oke_sent_coref_entity_comb ec \n" +
                "                INNER JOIN oke_coref_entity_pattern ep ON ep.id_oke_sent_entity = ec.id_oke_sent_entity\n" +
                "                ORDER BY ep.id_oke_coref_pattern;";

        Statement statement = null;
        try {
            statement = Database.databaseInstance.conn.createStatement();
            java.sql.ResultSet rs = statement.executeQuery(selectQuery);

            while(rs.next()) {
                int patternId = rs.getInt("id_oke_coref_pattern");
                String subj = rs.getString("subj");
                String obj = rs.getString("obj");

                HashMap<String, String> entityMap = new HashMap<>();
                entityMap.put("subj", subj);
                entityMap.put("obj", obj);

                patternIdEntitiesMap.put(patternId, entityMap);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void setOKESentencePropertyEntitiesMap() {
        String selectQuery = "SELECT oks.id_oke_sent, okt.prop_uri, okt.subj_label, okt.obj_label from oke_triples okt\n" +
                "                INNER JOIN oke_sent oks ON oks.id_oke_sent = okt.id_oke_sent\n" +
                "                GROUP BY oks.id_oke_sent, okt.prop_uri, okt.subj_label, okt.obj_label\n" +
                "                ORDER BY oks.id_oke_sent;";

        Statement statement = null;
        try {
            statement = Database.databaseInstance.conn.createStatement();
            java.sql.ResultSet rs = statement.executeQuery(selectQuery);

            while (rs.next()) {
                int sentId = rs.getInt("id_oke_sent");
                String property = rs.getString("prop_uri");
                String subj = rs.getString("subj_label");
                String obj = rs.getString("obj_label");

                HashMap<String, String> propertyDetailMap = new HashMap<>();
                propertyDetailMap.put("subj", subj);
                propertyDetailMap.put("obj", obj);

                Set<HashMap<String, String>> entitiesSet;
                HashMap<String, Set<HashMap<String, String>>> propertyEntitiesMap;
                if (sentIdPropertyEntitiesMap.containsKey(sentId)) {
                    propertyEntitiesMap = sentIdPropertyEntitiesMap.get(sentId);
                    if (propertyEntitiesMap.containsKey(property)) {
                        entitiesSet = propertyEntitiesMap.get(property);
                        entitiesSet.add(propertyDetailMap);

                    } else {
                        entitiesSet = new HashSet<>();
                        entitiesSet.add(propertyDetailMap);
                    }
                    propertyEntitiesMap.put(property, entitiesSet);
                } else {
                    entitiesSet = new HashSet<>();
                    entitiesSet.add(propertyDetailMap);

                    propertyEntitiesMap = new HashMap<>();
                    propertyEntitiesMap.put(property, entitiesSet);
                }
                sentIdPropertyEntitiesMap.put(sentId, propertyEntitiesMap);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void sentencePatternsMap() {
        String selectQuery = "select ec.id_oke_sent, ep.id_oke_coref_pattern from oke_coref_entity_pattern ep \n" +
                "                INNER JOIN oke_sent_coref_entity_comb ec ON ec.id_oke_sent_entity = ep.id_oke_sent_entity\n" +
                "                ORDER BY ec.id_oke_sent, ep.id_oke_coref_pattern;";

        Statement statement = null;
        try {
            statement = Database.databaseInstance.conn.createStatement();
            java.sql.ResultSet rs = statement.executeQuery(selectQuery);

            while (rs.next()) {
                int sentId = rs.getInt("id_oke_sent");
                int patternId = rs.getInt("id_oke_coref_pattern");
                Set<Integer> patternIds;
                if (sentIdPatternsMap.containsKey(sentId)) {
                    patternIds = sentIdPatternsMap.get(sentId);
                } else {
                    patternIds = new HashSet<>();
                }
                patternIds.add(patternId);
                sentIdPatternsMap.put(sentId, patternIds);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}