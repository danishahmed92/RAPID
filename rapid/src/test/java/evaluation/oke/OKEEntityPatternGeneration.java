package evaluation.oke;

import config.Database;
import edu.stanford.nlp.pipeline.Annotation;
import nlp.corenlp.annotator.ParseAnnotator;
import nlp.corenlp.utils.CoreNLPAnnotatorUtils;
import pattern.Pattern;
import pattern.PatternGenerator;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.List;

public class OKEEntityPatternGeneration {

    public HashMap<Integer, HashMap<String, String>> getOKEEntitySentence() {
        String selectQuery = "SELECT id_oke_sent_entity, id_oke_coref, " +
                "subj, obj, coref_sentence " +
                " FROM `oke_sent_coref_entity_comb` ;";
        Statement statement = null;
        HashMap<Integer, HashMap<String, String>> okeEntitySentenceMap = new HashMap<>();
        try {
            statement = Database.databaseInstance.conn.createStatement();
            ResultSet rs = statement.executeQuery(selectQuery);

            while (rs.next()) {
                int entityId = rs.getInt("id_oke_sent_entity");

                HashMap<String, String> entityDetailMap = new HashMap<>();
                entityDetailMap.put("idCoref", String.valueOf(rs.getInt("id_oke_coref")));
                entityDetailMap.put("subj", rs.getString("subj"));
                entityDetailMap.put("obj", rs.getString("obj"));
                entityDetailMap.put("corefSentence", rs.getString("coref_sentence"));

                okeEntitySentenceMap.put(entityId, entityDetailMap);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return okeEntitySentenceMap;
    }

    public void storeOKEPattern(int entitySentId, int corefSentId, Pattern pattern) {
        String dbQuery = "INSERT INTO `oke_coref_entity_pattern` (`id_oke_sent_entity`, `id_oke_coref`, " +
                "`orig_root`, `root_lemma`, " +
                "`pattern`, `pattern_extend`, `sg_pretty`, `sg_sentence`, " +
                "`dist_nouns`, `dist_verbs`) " +
                "values (?, ?, " +
                "?, ?, " +
                "?, ?, ?, ?, " +
                "?, ?);";

        PreparedStatement prepareStatement = null;
        try {
            prepareStatement = Database.databaseInstance.conn.prepareStatement(dbQuery, Statement.RETURN_GENERATED_KEYS);
            prepareStatement.setInt(1, entitySentId);
            prepareStatement.setInt(2, corefSentId);

            try {
                prepareStatement.setString(3, pattern.root.label);
                prepareStatement.setString(4, (pattern.root.lemma).contains("%") ? null : pattern.root.lemma);
                prepareStatement.setString(5, pattern.mergePatternStr);
                prepareStatement.setString(6, pattern.mergePatternExt);

                prepareStatement.setString(7, pattern.sgPretty);
                prepareStatement.setString(8, pattern.sgToSentence);
                prepareStatement.setString(9, pattern.distinctNouns.toString());
                prepareStatement.setString(10, pattern.distinctVerbs.toString());
            } catch (NullPointerException npe) {
                npe.printStackTrace();
                return;
            }

            prepareStatement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        OKEEntityPatternGeneration okeEntity = new OKEEntityPatternGeneration();

        HashMap<Integer, HashMap<String, String>> okeEntitySentenceMap = okeEntity.getOKEEntitySentence();
        for (int entityId : okeEntitySentenceMap.keySet()) {
            HashMap<String, String> entityDetailMap = okeEntitySentenceMap.get(entityId);

            int corefSentId = Integer.parseInt(entityDetailMap.get("idCoref"));
            String subject = entityDetailMap.get("subj");
            String object = entityDetailMap.get("obj");
            String corefSentence = entityDetailMap.get("corefSentence");

            ParseAnnotator parseAnnotator = ParseAnnotator.PAInstance;
            Annotation annotation = CoreNLPAnnotatorUtils.annotateDocument(parseAnnotator.getPipeline(), corefSentence);

            PatternGenerator pg = new PatternGenerator(annotation);
            List<Pattern> generatedPatterns = pg.generatePatterns(subject, object);
            for (Pattern pattern : generatedPatterns) {
                okeEntity.storeOKEPattern(entityId, corefSentId, pattern);
            }
        }
    }

}
