package pattern;

import config.Database;
import config.IniConfig;
import edu.stanford.nlp.pipeline.Annotation;
import nlp.corenlp.utils.CoreNLPAnnotatorUtils;
import utils.PropertyAnnotationUtils;
import utils.PropertyUtils;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.List;

public class PatternStorage {

    private void storePatternToDB(String annotationFile, Pattern pattern) {
        String[] split = annotationFile.split("_");
        String propertyUri = split[0];
        int idPropTriple = Integer.parseInt(split[1]);
        int idPsRefined = Integer.parseInt(split[2]);

        String dbQuery = "INSERT INTO `property_pattern` (`id_ps_coref`, `prop_uri`, `id_prop_triple`, `orig_root`, `root_lemma`, `pattern`, `pattern_extend`, `sg_pretty`, `sg_sentence`, `dist_nouns`, `dist_verbs`) " +
                "values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);";

        PreparedStatement prepareStatement = null;
        try {
            prepareStatement = Database.databaseInstance.conn.prepareStatement(dbQuery, Statement.RETURN_GENERATED_KEYS);
            prepareStatement.setInt(1, idPsRefined);
            prepareStatement.setString(2, propertyUri);
            prepareStatement.setInt(3, idPropTriple);

            try {
                prepareStatement.setString(4, pattern.root.label);
                prepareStatement.setString(5, (pattern.root.lemma).contains("%") ? null : pattern.root.lemma);
                prepareStatement.setString(6, pattern.mergePatternStr);
                prepareStatement.setString(7, pattern.mergePatternExt);
                prepareStatement.setString(8, pattern.sgPretty);
                prepareStatement.setString(9, pattern.sgToSentence);
                prepareStatement.setString(10, pattern.distinctNouns.toString());
                prepareStatement.setString(11, pattern.distinctVerbs.toString());
            } catch (NullPointerException npe) {
                npe.printStackTrace();
                return;
            }

            prepareStatement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void storePatternsForAllPropertiesAnnotations() {
        String annotationDirectory = IniConfig.configInstance.secondPassAnnotationDir;
        try {
            List<String> properties = PropertyUtils.getAllProperties();
            for (String property : properties) {
                System.out.println(property);
                HashMap<String, HashMap<String, String>> annotationsLabelMap = PropertyAnnotationUtils.getAnnotationLabelMap(property);
                List<String> annotationFiles = PropertyAnnotationUtils.getAnnotationFilesForProperty(property, annotationDirectory);

                if (annotationFiles != null && annotationFiles.size() > 0) {
                    for (String annotationFile : annotationFiles) {
                        if (annotationsLabelMap.containsKey(annotationFile)) {
                            String subjLabel = annotationsLabelMap.get(annotationFile).get("subjLabel");
                            String objLabel = annotationsLabelMap.get(annotationFile).get("objLabel");

                            Annotation annotation = CoreNLPAnnotatorUtils.readAnnotationFromFile(annotationDirectory + property + "/" + annotationFile);
                            PatternGenerator pg = new PatternGenerator(annotation);
                            List<Pattern> patternsForAnnotation = pg.generatePatterns(subjLabel, objLabel);

                            for (Pattern pattern : patternsForAnnotation) {
                                storePatternToDB(annotationFile, pattern);
                            }
                        }
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        PatternStorage patternStorage = new PatternStorage();
        patternStorage.storePatternsForAllPropertiesAnnotations();
    }
}
