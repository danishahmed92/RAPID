package rapid;

import config.Database;
import edu.stanford.nlp.pipeline.Annotation;
import nlp.corenlp.annotator.CoreferenceAnnotator;
import nlp.corenlp.annotator.ParseAnnotator;
import nlp.corenlp.ner.StanfordNER;
import nlp.corenlp.utils.CoreNLPAnnotatorUtils;
import pattern.Pattern;
import pattern.PatternGenerator;
import rapid.rules.PropertyFiltrationRules;
import rapid.score.PatternScore;
import rapid.score.ValidatingThreshold;
import wordembedding.model.GeneratedModelClassification;

import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;

/**
 * @author DANISH AHMED on 2/26/2019
 */
public class RelationExtractor {
    private Set<String> CORENLP_MERGE_LOCATION_TYPE = new HashSet<String>(){{
        add("CITY");
        add("STATE_OR_PROVINCE");
        add("COUNTRY");
        add("NATIONALITY");
    }};

    private Set<String> CORENLP_KEEP_ENTITY_TYPE = new HashSet<String>() {{
        add("ORGANIZATION");
        add("PERSON");
        add("LOCATION");
    }};

    private double alpha = 0.0;
    private double beta = 0.0;

    private GeneratedModelClassification embeddingClassifier;

    private String context;
    private HashMap<String, HashMap<String, Set<String>>> corefSentenceNERMap = new HashMap<>();

    private Set<String> corefSentences = new LinkedHashSet<>();
    private Set<String> entitiesRecognized = new HashSet<>();

    public RelationExtractor(double alpha, double beta, GeneratedModelClassification embeddingClassifier,
                      String context) {
        this.alpha = alpha;
        this.beta = beta;
        this.context = context;
        this.embeddingClassifier = embeddingClassifier;

        this.corefSentenceNERMap = getCorefedSentenceNERMap(context);

        this.corefSentences = corefSentenceNERMap.keySet();
        setEntitiesRecognized();
    }

    public RelationExtractor(double alpha, double beta, GeneratedModelClassification embeddingClassifier,
                      String context, HashMap<String, Set<String>> entityTypeEntitiesMap) {
        this.alpha = alpha;
        this.beta = beta;
        this.context = context;
        this.embeddingClassifier = embeddingClassifier;

        this.corefSentenceNERMap = getCorefedSentenceNERMap(context, entityTypeEntitiesMap);

        this.corefSentences = corefSentenceNERMap.keySet();
        setEntitiesRecognized();
    }

    private void setEntitiesRecognized() {
        for (String corefSentence : corefSentenceNERMap.keySet()) {
            HashMap<String, Set<String>> nerMap = corefSentenceNERMap.get(corefSentence);
            entitiesRecognized.addAll(getAllEntities(nerMap));
        }
    }

    public Set<String> getEntitiesRecognized() {
        return entitiesRecognized;
    }

    public Set<String> getCorefSentences() {
        return corefSentences;
    }

    public void setCorefSentenceNERMap(HashMap<String, HashMap<String, Set<String>>> corefSentenceNERMap) {
        this.corefSentenceNERMap = corefSentenceNERMap;
    }

    private Set<String> getAllEntities(HashMap<String, Set<String>> entityTypeEntitiesMap) {
        Set<String> allEntities = new HashSet<>();
        for (String entityType : entityTypeEntitiesMap.keySet()) {
            allEntities.addAll(entityTypeEntitiesMap.get(entityType));
        }
        return allEntities;
    }

    private HashMap<String, HashMap<String, Set<String>>> getCorefedSentenceNERMap(String context, HashMap<String, Set<String>> entityTypeEntitiesMap) {
        HashMap<String, HashMap<String, Set<String>>> corefSentenceNERMap = new LinkedHashMap<>();

        Set<String> allEntities = getAllEntities(entityTypeEntitiesMap);

        List<String> corefedSentences = getCorefedSentences(allEntities);
        for (String corefedSentence : corefedSentences) {
            HashMap<String, Set<String>> nerMap = new HashMap<>();
            for (String entityType : entityTypeEntitiesMap.keySet()) {
                Set<String> entities = entityTypeEntitiesMap.get(entityType);
                Set<String> entitiesExist = filterCorefEntitiesExistence(corefedSentence, entities);

                if (entitiesExist.size() > 0)
                    nerMap.put(entityType, entitiesExist);
            }
            HashMap<String, Set<String>> transformedNERMap = transformEntityTypeByCategory(nerMap);
            corefSentenceNERMap.put(corefedSentence, transformedNERMap);
        }
        return corefSentenceNERMap;
    }

    private Set<String> filterCorefEntitiesExistence(String corefSent, Set<String> entities) {
        Set<String> entitiesExist = new HashSet<>();
        for (String entity : entities) {
            if (corefSent.contains(entity))
                entitiesExist.add(entity);
        }
        return entitiesExist;
    }

    private HashMap<String, HashMap<String, Set<String>>> getCorefedSentenceNERMap(String context) {
        HashMap<String, HashMap<String, Set<String>>> corefSentenceNERMap = new LinkedHashMap<>();

        // passing no entities if only text is passed as input
        Set<String> entitySet = new HashSet<>();
        List<String> corefedSentences = getCorefedSentences(entitySet);
        for (String corefedSentence : corefedSentences) {
            HashMap<String, Set<String>> stanfordNERMap = nerMapRemovedExcess(corefedSentence);
            if (stanfordNERMap.size() == 0)
                continue;

            HashMap<String, Set<String>> transformedNERMap = transformEntityTypeByCategory(stanfordNERMap);
            corefSentenceNERMap.put(corefedSentence, transformedNERMap);
        }
        return corefSentenceNERMap;
    }

    private HashMap<String, Set<String>> nerMapRemovedExcess(String corefedSentence) {
        ParseAnnotator parseAnnotator = ParseAnnotator.PAInstance;
        Annotation nerAnnotation = CoreNLPAnnotatorUtils.annotateDocument(parseAnnotator.getPipeline(), corefedSentence);
        StanfordNER ner = new StanfordNER(nerAnnotation);

        HashMap<String, Set<String>> stanfordNERMap = ner.getNerMap();
        Set<String> toRemoveEntityTypes = new HashSet<>();
        for (String entityType : stanfordNERMap.keySet()) {
            if (CORENLP_MERGE_LOCATION_TYPE.contains(entityType)) {
                Set<String> entities = stanfordNERMap.get(entityType);
                if (stanfordNERMap.containsKey("LOCATION")) {
                    Set<String> locationEntities = stanfordNERMap.get("LOCATION");
                    locationEntities.addAll(entities);
                    stanfordNERMap.put("LOCATION", locationEntities);
                } else {
                    stanfordNERMap.put("LOCATION", entities);
                }
            }

            // since we have merged location, we can remove unnecessary entity types
            if (!CORENLP_KEEP_ENTITY_TYPE.contains(entityType)) {
                toRemoveEntityTypes.add(entityType);
            }
        }

        for (String toRemove : toRemoveEntityTypes)
            stanfordNERMap.remove(toRemove);

        return stanfordNERMap;
    }

    private List<String> getCorefedSentences(Set<String> entities) {
        CoreferenceAnnotator coreference = CoreferenceAnnotator.CRInstance;
        Annotation document = CoreNLPAnnotatorUtils.annotateDocument(coreference.getPipeline(), context);

        return coreference.getCoreferenceReplacedSentences(document, entities);
    }

    private HashMap<String, Set<String>> transformEntityTypeByCategory(HashMap<String, Set<String>> nerMap) {
        HashMap<String, Set<String>> transformedNERMap = new HashMap<>();
        for (String entityType : nerMap.keySet()) {
            String type = entityType.trim().toLowerCase();
            switch (type) {
                case "organization":
                    type = "Organisation";
                    break;
                case "location":
                    type = "Place";
                    break;
                case "person":
                    type = "Person";
                    break;
            }
            transformedNERMap.put(type, nerMap.get(entityType));
        }
        return transformedNERMap;
    }

    private HashMap<String, String> getEntityEntityTypeMap(HashMap<String, Set<String>> nerMap) {
        HashMap<String, String> entityEntityTypeMap = new HashMap<>();
        for (String entityType : nerMap.keySet()) {
            Set<String> entities = nerMap.get(entityType);
            for (String entity : entities) {
                if (!entityEntityTypeMap.containsKey(entity))
                    entityEntityTypeMap.put(entity, entityType);
            }
        }
        return entityEntityTypeMap;
    }

    private Set<String> getPropertiesValidatingDomainRange(String domain, String range) {
        String query = String.format("select prop_uri from oke_property_class where subj_class = \"%s\" and obj_class = \"%s\";",
                domain, range);
        Set<String> properties = new HashSet<>();
        Statement statement = null;
        try {
            statement = Database.databaseInstance.conn.createStatement();
            java.sql.ResultSet rs = statement.executeQuery(query);

            while (rs.next()) {
                properties.add(rs.getString("prop_uri"));
            }
            statement.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return properties;
    }

    private List<Pattern> generatePatternFromSentence(String corefSentence, String subj, String obj) {
        PatternGenerator pg = new PatternGenerator(corefSentence);
        return pg.generatePatterns(subj, obj);
    }

    private HashMap<String, Set<HashMap<String, String>>> filterNonSymmetricProperties(HashMap<String, Set<HashMap<String, String>>> propertyPredicationDetailMap) {
        HashMap<String, Set<HashMap<String, String>>> refinedPropertyPredictionMap = new HashMap<>();
        for (String property : propertyPredicationDetailMap.keySet()) {
            if (PropertyFiltrationRules.NON_SYMMETRIC_PROPERTIES.contains(property)) {
                Set<HashMap<String, String>> refinedPrediction = new HashSet<>();

                Set<HashMap<String, String>> predictionSet = propertyPredicationDetailMap.get(property);
                for (HashMap<String, String> predictionMap : predictionSet) {
                    String subj = predictionMap.get("subj");
                    String obj = predictionMap.get("obj");

                    for (HashMap<String, String> comparePredictMap : predictionSet) {
                        String compSubj = comparePredictMap.get("subj");
                        String compObj = comparePredictMap.get("obj");

                        if (subj.equals(compObj) && obj.equals(compSubj)) {
                            double predictionSum = Double.parseDouble(predictionMap.get("confidence"))
                                    + Double.parseDouble(predictionMap.get("embedding"));
                            double compPredictionSum = Double.parseDouble(comparePredictMap.get("confidence"))
                                    + Double.parseDouble(comparePredictMap.get("embedding"));
                            if (compPredictionSum > predictionSum)
                                refinedPrediction.add(comparePredictMap);
                            else
                                refinedPrediction.add(predictionMap);
                        }
                    }
                }
                refinedPropertyPredictionMap.put(property, refinedPrediction);
            } else {
                Set<HashMap<String, String>> predictionSet = propertyPredicationDetailMap.get(property);
                refinedPropertyPredictionMap.put(property, predictionSet);
            }
        }
        return refinedPropertyPredictionMap;
    }

    public HashMap<String, Set<HashMap<String, String>>> extractRelationsStringSimilarity() {
        HashMap<String, Set<HashMap<String, String>>> propertyPredicationDetailMap = new HashMap<>();

        // if for a corefed sentence, all entities count < 2
        // do not call pattern generation as relation cannot exist with just one entity
        for (String corefSentence : corefSentenceNERMap.keySet()) {
            Set<String> allEntities = getAllEntities(corefSentenceNERMap.get(corefSentence));
            if (allEntities.size() < 2)
                continue;

            HashMap<String, String> entityEntityTypeMap = getEntityEntityTypeMap(corefSentenceNERMap.get(corefSentence));
            for (String subjEntity : entityEntityTypeMap.keySet()) {
                for (String objEntity : entityEntityTypeMap.keySet()) {
                    if (subjEntity.equals(objEntity))
                        continue;

                    String domain = entityEntityTypeMap.get(subjEntity);
                    String range = entityEntityTypeMap.get(objEntity);
                    Set<String> candidateProperties = getPropertiesValidatingDomainRange(domain, range);
                    if (candidateProperties.size() == 0)
                        continue;

                    // if subject(domain) and object(range) have properties count > 0
                    // generate pattern for that subj-obj combination
                    List<Pattern> patterns = generatePatternFromSentence(corefSentence, subjEntity, objEntity);
                    for (Pattern pattern : patterns) {
                        PatternScore ps = new PatternScore(alpha, beta,
                                pattern, candidateProperties, embeddingClassifier);
                        ps.calculateMaxProperty();
                        String predictedProperty = ps.getMaxMatchedProperty();

                        HashMap<String, String> predictionDetailMap = new HashMap<>();
                        predictionDetailMap.put("subj", subjEntity);
                        predictionDetailMap.put("obj", objEntity);
                        predictionDetailMap.put("confidence", ps.getPredictionDetailMap().get("confidence"));
                        predictionDetailMap.put("embedding", ps.getPredictionDetailMap().get("embedding"));

                        double confidence = ps.getConfidence();
                        if (confidence != -1) {
                            Set<HashMap<String, String>> predictionDetailList;
                            if (propertyPredicationDetailMap.containsKey(predictedProperty)) {
                                predictionDetailList = propertyPredicationDetailMap.get(predictedProperty);
                                predictionDetailList.add(predictionDetailMap);
                            } else {
                                predictionDetailList = new HashSet<>();
                                predictionDetailList.add(predictionDetailMap);
                            }
                            propertyPredicationDetailMap.put(predictedProperty, predictionDetailList);
                        }
                    }
                }
            }
        }
        return filterNonSymmetricProperties(propertyPredicationDetailMap);
    }

    public HashMap<String, Set<HashMap<String, String>>> extractRelationsFSS() {
        HashMap<String, Set<HashMap<String, String>>> propertyPredicationDetailMap = new HashMap<>();

        // if for a corefed sentence, all entities count < 2
        // do not call pattern generation as relation cannot exist with just one entity
        for (String corefSentence : corefSentenceNERMap.keySet()) {
            Set<String> allEntities = getAllEntities(corefSentenceNERMap.get(corefSentence));
            if (allEntities.size() < 2)
                continue;

            HashMap<String, String> entityEntityTypeMap = getEntityEntityTypeMap(corefSentenceNERMap.get(corefSentence));
            for (String subjEntity : entityEntityTypeMap.keySet()) {
                for (String objEntity : entityEntityTypeMap.keySet()) {
                    if (subjEntity.equals(objEntity))
                        continue;

                    String domain = entityEntityTypeMap.get(subjEntity);
                    String range = entityEntityTypeMap.get(objEntity);
                    Set<String> candidateProperties = getPropertiesValidatingDomainRange(domain, range);
                    if (candidateProperties.size() == 0)
                        continue;

                    // if subject(domain) and object(range) have properties count > 0
                    // generate pattern for that subj-obj combination
                    List<Pattern> patterns = generatePatternFromSentence(corefSentence, subjEntity, objEntity);
                    for (Pattern pattern : patterns) {
                        PatternScore ps = new PatternScore(alpha, beta,
                                pattern, candidateProperties, embeddingClassifier);
                        ps.calculateMaxPropertyFSS();
                        String predictedProperty = ps.getMaxMatchedProperty();

                        HashMap<String, String> predictionDetailMap = new HashMap<>();
                        predictionDetailMap.put("subj", subjEntity);
                        predictionDetailMap.put("obj", objEntity);
                        predictionDetailMap.put("confidence", ps.getPredictionDetailMap().get("confidence"));
                        predictionDetailMap.put("embedding", ps.getPredictionDetailMap().get("embedding"));

                        double confidence = Double.parseDouble(ps.getPredictionDetailMap().get("confidence"));
                        double threshold = ValidatingThreshold.getThreshold(alpha, beta, predictedProperty);

                        if (confidence < threshold)
                            continue;

                        Set<HashMap<String, String>> predictionDetailList;
                        if (propertyPredicationDetailMap.containsKey(predictedProperty)) {
                            predictionDetailList = propertyPredicationDetailMap.get(predictedProperty);
                            predictionDetailList.add(predictionDetailMap);
                        } else {
                            predictionDetailList = new HashSet<>();
                            predictionDetailList.add(predictionDetailMap);
                        }
                        propertyPredicationDetailMap.put(predictedProperty, predictionDetailList);
                    }
                }
            }
        }
        return filterNonSymmetricProperties(propertyPredicationDetailMap);
    }

    public static void main(String[] args) {
        String context = "Ahmed was born in Pakistan. " +
                "He raised his son named Ali. " +
                "Ali graduated from the University of Saarland. " +
                "He now works at Microsoft as a software engineer.";

        double alpha = 0.5;
        double beta = 0.5;
        GeneratedModelClassification embeddingClassifier = GeneratedModelClassification.synsetW2VClassification;

        RelationExtractor re = new RelationExtractor(alpha, beta, embeddingClassifier, context);
//        HashMap<String, Set<HashMap<String, String>>> propertyPredicationDetailMap = re.extractRelationsStringSimilarity();
        HashMap<String, Set<HashMap<String, String>>> propertyPredicationDetailMap = re.extractRelationsFSS();

        System.out.println(propertyPredicationDetailMap);
    }
}
