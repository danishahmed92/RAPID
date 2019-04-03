package evaluation.oke;

import config.Database;
import config.IniConfig;
import pattern.Node;
import pattern.Pattern;
import rapid.score.PatternScore;
import wordembedding.model.GeneratedModelClassification;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;

public class OKEEntityPatternConfidence {
    private HashMap<Integer, Set<String>> patternIdPropertiesMap = new HashMap<>();

    public HashMap<Integer, HashMap<String, String>> getOKEPatternsMap() {
        String selectQuery = "SELECT p.id_oke_coref_pattern, p.orig_root, p.root_lemma, p.pattern, p.pattern_extend, p.dist_nouns, p.dist_verbs from oke_coref_entity_pattern p " +
                "order by p.id_oke_coref_pattern";
        Statement statement = null;
        HashMap<Integer, HashMap<String, String>> okePatternMap = new LinkedHashMap<>();
        try {
            statement = Database.databaseInstance.conn.createStatement();
            java.sql.ResultSet rs = statement.executeQuery(selectQuery);

            while (rs.next()) {
                int patternId = rs.getInt("id_oke_coref_pattern");

                String nouns = rs.getString("dist_nouns");
                nouns = nouns.substring(1, nouns.length()-1);
                nouns = nouns.replaceAll("-", ", ");
                nouns = nouns.replaceAll("_", ", ");

                String verbs = rs.getString("dist_verbs");
                verbs = verbs.substring(1, verbs.length()-1);
                verbs = verbs.replaceAll("-", ", ");
                verbs = verbs.replaceAll("_", ", ");

                HashMap<String, String> patternDetailMap = new HashMap<>();
                patternDetailMap.put("root", rs.getString("orig_root"));
                patternDetailMap.put("rootLemma", rs.getString("root_lemma"));
                patternDetailMap.put("pattern", rs.getString("pattern"));
                patternDetailMap.put("patternExtend", rs.getString("pattern_extend"));
                patternDetailMap.put("nouns", nouns);
                patternDetailMap.put("verbs", verbs);

                okePatternMap.put(patternId, patternDetailMap);
            }
            statement.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return okePatternMap;
    }

    public Set<String> commaStringToSet(String str) {
        str = str.toLowerCase().trim();
        Set<String> set = new HashSet<>();

        if (str != null && str.length() > 0) {
            String[] strSplit = str.split(", ");
            set.addAll(Arrays.asList(strSplit));
        }
        return set;
    }

    public Set<String> getCandidatePropertiesForPattern (int patternId) {
        if (patternIdPropertiesMap.containsKey(patternId))
            return patternIdPropertiesMap.get(patternId);

        String selectQuery = String.format("SELECT pc.prop_uri from oke_property_class pc\n" +
                "INNER JOIN oke_sent_coref_entity_comb e ON (e.subj_class = pc.subj_class AND e.obj_class = pc.obj_class)\n" +
                "INNER JOIN oke_coref_entity_pattern p ON p.id_oke_sent_entity = e.id_oke_sent_entity\n" +
                "WHERE p.id_oke_coref_pattern = %d;", patternId);
        Statement statement = null;
        Set<String> candidateProperties = new HashSet<>();
        try {
            statement = Database.databaseInstance.conn.createStatement();
            java.sql.ResultSet rs = statement.executeQuery(selectQuery);

            while (rs.next())
                candidateProperties.add(rs.getString("prop_uri"));

            patternIdPropertiesMap.put(patternId, candidateProperties);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return patternIdPropertiesMap.get(patternId);
    }

    public static void main(String[] args) {
        GeneratedModelClassification embeddingClassifier = null;
        String path = null;
        for (int i = 0; i < 3; i++) {
            switch (i) {
                case 0:
                    embeddingClassifier = GeneratedModelClassification.synsetW2VClassification;
                    path = IniConfig.configInstance.resultPath + "fssEvaluation/" + "w2v/synset/";
                    break;
                case 1:
                    embeddingClassifier = GeneratedModelClassification.synsetFTClassification;
                    path = IniConfig.configInstance.resultPath + "fssEvaluation/" + "ft/synset/";
                    break;
                case 2:
                    embeddingClassifier = GeneratedModelClassification.synsetGloveClassification;
                    path = IniConfig.configInstance.resultPath + "fssEvaluation/" + "glove/synset/";
                    break;

            }

            OKEEntityPatternConfidence okeConfidence = new OKEEntityPatternConfidence();
            HashMap<Integer, HashMap<String, String>> okePatternMap = okeConfidence.getOKEPatternsMap();

            for (double alpha = 0.1; alpha <= 0.9; alpha = alpha + 0.1) {
                for (double beta = 0.1; beta <= 0.9; beta = beta + 0.1) {
                    String outputFile = path + String.format("alpha%.1fbeta%.1f",
                            alpha,
                            beta);
                    try {
                        PrintWriter writer = new PrintWriter(outputFile, "UTF-8");
                        writer.println(String.format("%.1f\t%.1f", alpha, beta));

                        for (int patternId : okePatternMap.keySet()) {
                            HashMap<String, String> patternDetailMap = okePatternMap.get(patternId);

                            Node root = new Node(patternDetailMap.get("root"), patternDetailMap.get("rootLemma"));
                            Set<String> distinctNouns = okeConfidence.commaStringToSet(patternDetailMap.get("nouns"));
                            Set<String> distinctVerbs = okeConfidence.commaStringToSet(patternDetailMap.get("verbs"));

                            Pattern pattern = new Pattern(root,
                                    patternDetailMap.get("pattern"), patternDetailMap.get("patternExtend"),
                                    distinctNouns, distinctVerbs);

                            Set<String> candidateProperties = okeConfidence.getCandidatePropertiesForPattern(patternId);
                            PatternScore ps = new PatternScore(alpha, beta,
                                    pattern, candidateProperties, embeddingClassifier);
                            ps.calculateMaxPropertyFSS();

                            String predictedProperty = ps.getMaxMatchedProperty();
                            double confidence = Double.parseDouble(ps.getPredictionDetailMap().get("confidence"));
                            double embeddingWithPredictProperty = Double.parseDouble(ps.getPredictionDetailMap().get("embedding"));

                            writer.println(String.format("%d\t" +   // patternId
                                            "%s\t" +    // predicted property
                                            "%f\t%f",       // confidence value
                                    patternId,
                                    predictedProperty,
                                    confidence, embeddingWithPredictProperty));
                        }
                        writer.close();
                    } catch (FileNotFoundException | UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

    }
}
