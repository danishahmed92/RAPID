package rapid.score;

import config.Database;
import org.apache.commons.text.similarity.JaroWinklerDistance;
import pattern.Pattern;
import rapid.score.similaritymetric.PatternSemanticSimilarity;
import wordembedding.model.GeneratedModelClassification;

import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;

/**
 * @author DANISH AHMED on 2/26/2019
 */
public class PatternScore {
    private double alpha = 0.0;
    private double beta = 0.0;

    private Pattern pattern;
    private Set<String> candidateProperties = new HashSet<>();
    private GeneratedModelClassification embeddingClassifier;
    private ScoreHelper scoreHelper = ScoreHelper.scoreHelperInstance;

    private double embeddingMinThreshold = 0.5;
    private double embeddingWildCardInclusion = 0.8;
    private boolean embeddingThresholdRestriction = true;
    private boolean embeddingWildCard = true;

    private double confidence = -1;
    private String maxMatchedProperty = "";

    private JaroWinklerDistance similarityMetric = new JaroWinklerDistance();
    private HashMap<String, Double> propLowerThreshold = new HashMap<>();

    private HashMap<String, String> predictionDetailMap = new HashMap<>();

    /**
     *
     * @param alpha alpha
     * @param beta beta
     * @param pattern pattern to score using params
     * @param candidateProperties properties that verifies domain and range
     * @param embeddingClassifier embedding classifier to use for scoring
     */
    public PatternScore(double alpha, double beta,
                        Pattern pattern, Set<String> candidateProperties,
                        GeneratedModelClassification embeddingClassifier) {
        this.alpha = alpha;
        this.beta = beta;

        this.pattern = pattern;
        this.candidateProperties.addAll(candidateProperties);

        this.embeddingClassifier = embeddingClassifier;

//        setThresholdMap();
    }

//    TODO: replace by alpha and beta after training all thresholds
    @Deprecated
    private void setThresholdMap() {
        /*String selectQuery = String.format("select prop_uri_lower, mean, sd, variance from property_threshold where alpha = %.1f and beta = %.1f;",
                alpha, beta);*/
        String selectQuery = String.format("select prop_uri_lower, mean, sd, variance from property_threshold where alpha = %.1f and beta = %.1f;",
                0.4, 0.7);

        Statement statement = null;
        try {
            statement = Database.databaseInstance.conn.createStatement();
            java.sql.ResultSet rs = statement.executeQuery(selectQuery);

            while (rs.next()) {
                double mean = rs.getDouble("mean");
                double sd = rs.getDouble("sd");
                double variance = rs.getDouble("variance");

                double threshold = mean - (sd + variance);
                propLowerThreshold.put(rs.getString("prop_uri_lower"), threshold);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public HashMap<String, String> getPredictionDetailMap() {
        return predictionDetailMap;
    }

    public double getConfidence() {
        return confidence;
    }

    public String getMaxMatchedProperty() {
        return maxMatchedProperty;
    }

    /**
     *
     * @return all the verbs and nouns that were encountered while traversing a pattern
     */
    private List<String> getPatternNounVerbList() {
        List<String> nounsVerbList = new ArrayList<>();
        if (pattern.distinctNouns != null && pattern.distinctNouns.size() > 0) {
            List<String> nouns = new ArrayList<>(pattern.distinctNouns);
            nouns.replaceAll(String::toLowerCase);

            nounsVerbList.addAll(nouns);
        }

        if (pattern.distinctVerbs != null && pattern.distinctVerbs.size() > 0) {
            List<String> verbs = new ArrayList<>(pattern.distinctVerbs);
            verbs.replaceAll(String::toLowerCase);

            nounsVerbList.addAll(verbs);
        }
        return nounsVerbList;
    }

    /**
     *
     * @param property ontology
     * @return subcalculation of confidence that relies on embedding w.r.t property embedding model
     */
    private double patternEmbeddingSimilarityAgainstProperty(String property) {
        List<String> roots = new ArrayList<>();
        roots.add(pattern.root.label.toLowerCase());
        if (pattern.root.lemma != null && !roots.contains((pattern.root.lemma).toLowerCase()))
            roots.add(pattern.root.lemma.toLowerCase());

        List<String> nounsVerbList = getPatternNounVerbList();

        double rootCosine = embeddingClassifier.getSimilarityOfWordsWithProperty(roots, property);
        double nounsVerbCosine = embeddingClassifier.getSimilarityOfWordsWithProperty(nounsVerbList, property);

        return rootCosine + nounsVerbCosine;
    }

    /**
     *
     * @param property property
     * @return means similarity of traversed node words; from original embedding
     */
    private double patternWordsPropertyEmbeddingSimilarity(String property) {
        List<String> words = new ArrayList<>();
        words.add(pattern.root.label.toLowerCase());
        if (pattern.root.lemma != null && !words.contains((pattern.root.lemma).toLowerCase()))
            words.add(pattern.root.lemma.toLowerCase());

        words.addAll(getPatternNounVerbList());
        return embeddingClassifier.getSimilarityOfWordsWithProperty(words, property);

    }

    /**
     * Time-Efficient approach
     * @param property property
     * @param comparisonPatternMap pattern to compare with
     * @param embeddingSimilarity score of embedding similarity
     * @return confidence value of a pattern for property
     */
    @Deprecated
    private double getConfidenceOfPatternAgainstProperty(String property, HashMap<String, String> comparisonPatternMap,
                                                         double embeddingSimilarity) {
        double support = Double.parseDouble(comparisonPatternMap.get("support"));
        try {
            support = support / scoreHelper.getPropertyPatternCountMap().get(property);
            if (Double.isNaN(support))
                support = 0;
        } catch (NullPointerException e) {
            e.printStackTrace();
            System.out.println("no patterns for property: " + property);
        }

        double specificity = Double.parseDouble(comparisonPatternMap.get("specificity"));
        double occurProp = Double.parseDouble(comparisonPatternMap.get("occurProp"));
        double occurPatternFreq = Double.parseDouble(comparisonPatternMap.get("occurPatternFreq"));
        specificity = (specificity * occurProp) / occurPatternFreq;
        if (Double.isNaN(specificity))
            specificity = 0;

        double alphaCalculation = ((alpha * support) + (1 - alpha) * specificity);
        String inputSentencePattern = pattern.mergePatternStr;
        String compareWithPattern = comparisonPatternMap.get("pattern");
        String sentenceSGPretty = pattern.sgPretty;
        sentenceSGPretty = ScoreHelper.removeWordsFromSGPretty(sentenceSGPretty);

        double patternSimilarity = similarityMetric.apply(inputSentencePattern, compareWithPattern);
        double sgSimilarity = similarityMetric.apply(sentenceSGPretty, comparisonPatternMap.get("sgPretty"));

        double betaCalculation = beta * patternSimilarity * sgSimilarity;

//        double confidence = ((alphaCalculation * betaCalculation) + embeddingSimilarity);
//        return (confidence - 0) / (3 - 0);  // normalizing value
        double confidence = (beta * alphaCalculation * patternSimilarity) + ((1 - beta) * embeddingSimilarity);
        return (confidence - 0) / (3 - 0);  // normalizing value
    }

    /**
     * Accuracy Efficient approach
     * Uses FSS algorithm and extended labels
     * @param property property
     * @param comparisonPatternMap pattern to compare with
     * @param embeddingSimilarity embedding score
     * @return confidence value of a pattern for property
     */
    private double getExtendedPatternConfidenceForProperty(String property, HashMap<String, String> comparisonPatternMap,
                                                         double embeddingSimilarity) {
        double support = Double.parseDouble(comparisonPatternMap.get("support"));
        try {
            support = support / scoreHelper.getPropertyPatternCountMap().get(property);
            if (Double.isNaN(support))
                support = 0;
        } catch (NullPointerException e) {
            e.printStackTrace();
            System.out.println("no patterns for property: " + property);
        }

        double specificity = Double.parseDouble(comparisonPatternMap.get("specificity"));
        double occurPatternFreq = Double.parseDouble(comparisonPatternMap.get("occurPatternFreq"));
        specificity = specificity / occurPatternFreq;
        if (Double.isNaN(specificity))
            specificity = 0;

        String compPatternExtend = comparisonPatternMap.get("patternExtend");
        String inputPatternExtend = pattern.mergePatternExt;

        PatternSemanticSimilarity pss = new PatternSemanticSimilarity(compPatternExtend, inputPatternExtend, embeddingClassifier);
        double patternSemanticSimilarity = pss.getPatternSemanticSimilarity();

        double alphaCalculation = ((alpha * support) + (1 - alpha) * specificity);
        double betaCalculation = embeddingSimilarity;
//        double betaCalculation = (1 - beta) * embeddingSimilarity;    // Micha
//        double confidence = (beta * alphaCalculation * patternSemanticSimilarity) + betaCalculation;  // Micha
        double confidence = (alphaCalculation * patternSemanticSimilarity) + (beta * betaCalculation);
//      double confidence = (alphaCalculation) + (beta * (betaCalculation + patternSemanticSimilarity) / 2.0);
        return confidence;  // return without normalization
    }

    /**
     * Time efficient approach to select which property resulted in max confidence
     */
    public void calculateMaxProperty() {
        confidence = -1;
        maxMatchedProperty = "";
        double maxPropertyEmbeddingSimilarity = 0;

        for (String property : candidateProperties) {
            double embeddingSimilarity = patternEmbeddingSimilarityAgainstProperty(property);
            HashMap<String, HashMap<String, String>> patternFreqSGMap =
                    scoreHelper.getPatternsFreqAndSGPrettyForProperty(property);

            // compare candidate property with patterns of trained property
            for (String trainPattern : patternFreqSGMap.keySet()) {
                HashMap<String, String> comparisonPatternMap = patternFreqSGMap.get(trainPattern);
                comparisonPatternMap.put("pattern", trainPattern);
                double score = getConfidenceOfPatternAgainstProperty(property, comparisonPatternMap, embeddingSimilarity);

                if (score > confidence) {
                    confidence = score;
                    maxMatchedProperty = property;
                    maxPropertyEmbeddingSimilarity = embeddingSimilarity;
                }
            }
        }

        predictionDetailMap.put("property", maxMatchedProperty);
        predictionDetailMap.put("confidence", String.valueOf(confidence));
        predictionDetailMap.put("embedding", String.valueOf(maxPropertyEmbeddingSimilarity));

        if (embeddingThresholdRestriction && maxPropertyEmbeddingSimilarity < embeddingMinThreshold)
            confidence = -1;
        else {
            if (!(embeddingWildCard && maxPropertyEmbeddingSimilarity >= embeddingWildCardInclusion)) {
                if (propLowerThreshold.containsKey(maxMatchedProperty.toLowerCase())) {
                    double threshold = propLowerThreshold.get(maxMatchedProperty.toLowerCase());
                    if (confidence < threshold) {
                        confidence = -1;
                    }
                }
            }
        }
    }

    private double normalizeEmbeddingScore(double embeddingScore) {
        return (embeddingScore + 1) / (2);
    }

    /**
     * Accuracy Efficient approach to select which property resulted in max confidence
     */
    public void calculateMaxPropertyFSS() {
        confidence = -2;
        maxMatchedProperty = "";
        double maxPatternWordsEmbeddingSimilarity = 0;

        for (String property : candidateProperties) {
            double pwEmbeddingSimilarity = patternWordsPropertyEmbeddingSimilarity(property);
            pwEmbeddingSimilarity = normalizeEmbeddingScore(pwEmbeddingSimilarity);

            /*if (pattern.equals("{}%{}")) {
                double betaCalculation = (1 - beta) * pwEmbeddingSimilarity;
                double score = betaCalculation;

                if (score > confidence) {
                    confidence = score;
                    maxMatchedProperty = property;
                    maxPatternWordsEmbeddingSimilarity = pwEmbeddingSimilarity;
                }
                continue;
            }*/

            HashMap<String, String> trainedExtendedPatterns = scoreHelper.getPropertyExtendPatternMap().get(property);
            HashMap<String, HashMap<String, String>> patternFreqSGMap =
                    scoreHelper.getPatternsFreqAndSGPrettyForProperty(property);

            for (String extendedPattern : trainedExtendedPatterns.keySet()) {
                String narrowPattern = trainedExtendedPatterns.get(extendedPattern);
                if (narrowPattern.equals("{}%{}")) {
                    double betaCalculation = (1 - beta) * pwEmbeddingSimilarity;
                    double score = betaCalculation;

                    if (score > confidence) {
                        confidence = score;
                        maxMatchedProperty = property;
                        maxPatternWordsEmbeddingSimilarity = pwEmbeddingSimilarity;
                    }
                    continue;
                }

                HashMap<String, String> comparisonPatternMap = patternFreqSGMap.get(narrowPattern);
                comparisonPatternMap.put("pattern", narrowPattern);
                comparisonPatternMap.put("patternExtend", extendedPattern);

                double score = getExtendedPatternConfidenceForProperty(property, comparisonPatternMap, pwEmbeddingSimilarity);
                if (score > confidence) {
                    confidence = score;
                    maxMatchedProperty = property;
                    maxPatternWordsEmbeddingSimilarity = pwEmbeddingSimilarity;
                }
            }

        }

        predictionDetailMap.put("property", maxMatchedProperty);
        predictionDetailMap.put("confidence", String.valueOf(confidence));
        predictionDetailMap.put("embedding", String.valueOf(maxPatternWordsEmbeddingSimilarity));
    }
}
