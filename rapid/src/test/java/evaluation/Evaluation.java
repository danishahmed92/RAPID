package evaluation;

import rapid.rules.PropertyFiltrationRules;
import rapid.score.EmpiricalThresholdHelper;
import utils.Utils;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;

/**
 * @author DANISH AHMED on 3/24/2019
 */
public class Evaluation {
    private HashMap<Integer, HashMap<String, String>> patternIdEntitiesMap;
    private HashMap<Integer, HashMap<String, String>> patternPredictionMap = new HashMap<>();

    private HashMap<String, List<Double>> propertyScoreListMap = new HashMap<>();
    @Deprecated
    private HashMap<String, Double> propertyThresholdMap = new HashMap<>();

    private String resultFile;
    private double alpha = 0.0;
    private double beta = 0.0;

    public Evaluation(HashMap<Integer, HashMap<String, String>> patternIdEntitiesMap, String resultFile) {
        this.patternIdEntitiesMap = patternIdEntitiesMap;
        this.resultFile = resultFile;

        setPatternPredictionsFromResultFile();
//        setPropertyThresholdMap();
    }

    public HashMap<Integer, HashMap<String, String>> getPatternPredictionMap() {
        return patternPredictionMap;
    }

    @Deprecated
    private void setPropertyThresholdMap() {
        for (String property : propertyScoreListMap.keySet()) {
            List<Double> randomVariablesList = propertyScoreListMap.get(property);

            double threshold = EmpiricalThresholdHelper.calculateThreshold(randomVariablesList);
            propertyThresholdMap.put(property, threshold);
        }
    }

    private void setPatternPredictionsFromResultFile() {
        FileInputStream fstream = null;
        try {
            fstream = new FileInputStream(resultFile);
            BufferedReader br = new BufferedReader(new InputStreamReader(fstream));

            boolean firstLine = true;
            String strLine;
            while ((strLine = br.readLine()) != null) {
                if (firstLine) {
                    String[] split = strLine.split("\t");
                    alpha = Double.parseDouble(split[0]);
                    beta = Double.parseDouble(split[1]);

                    firstLine = false;
                    continue;
                }

                String[] result = strLine.split("\t");
                int patternId = Integer.parseInt(result[0]);
                String predictedProperty = result[1];
                String score = result[2];
                String embedding = result[3];

                if (patternId <= 0 || predictedProperty.equals(""))
                    continue;

                HashMap<String, String> predictionMap = new HashMap<>();
                predictionMap.put("property", predictedProperty);
                predictionMap.put("confidence", score);
                predictionMap.put("embedding", embedding);

                if (!patternIdEntitiesMap.containsKey(patternId))
                    continue;
                HashMap<String, String> entityMap = patternIdEntitiesMap.get(patternId);

                predictionMap.put("subj", entityMap.get("subj"));
                predictionMap.put("obj", entityMap.get("obj"));

                patternPredictionMap.put(patternId, predictionMap);

                List<Double> propertyScoreList;
                if (propertyScoreListMap.containsKey(predictedProperty)) {
                    propertyScoreList = propertyScoreListMap.get(predictedProperty);
                } else {
                    propertyScoreList = new ArrayList<>();
                }
                propertyScoreList.add(Double.valueOf(score));
                propertyScoreListMap.put(predictedProperty, propertyScoreList);
            }
        } catch (IOException e1) {
            e1.printStackTrace();
        }
    }

    private HashMap<String, Set<HashMap<String, String>>> filterNonSymmetricProperties(
            HashMap<String, Set<HashMap<String, String>>> propertyPredicationDetailMap) {
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

    public static void main(String[] args) {
        double maxFMeasure = -1;
        double maxAlpha = -1;
        double maxBeta = -1;

        OKEEvalHelper evalHelper = OKEEvalHelper.evalHelper;
        HashMap<Integer, HashMap<String, String>> patternIdEntitiesMap =
                evalHelper.getPatternIdEntitiesMap();
        HashMap<Integer, HashMap<String, Set<HashMap<String, String>>>> sentIdPropertyEntitiesMap =
                evalHelper.getSentIdPropertyEntitiesMap();
        HashMap<Integer, Set<Integer>> sentIdPatternIdsMap =
                evalHelper.getSentIdPatternsMap();

        String embdTyp = "ft";
        String resultPath = "results/fssEvaluation_norm_my/ft/synset/";
        List<String> files = Utils.getFilesInDirectory(resultPath);

        for (String file : files) {
            HashMap<String, List<HashMap<String, Double>>> propTruePosi = new HashMap<>();
            HashMap<String, List<HashMap<String, Double>>> propFalsePosi = new HashMap<>();
            HashMap<String, Integer> propFalseNeg = new HashMap<>();

            int truePositive = 0;
            int falsePositive = 0;
            int falseNegative = 0;

            Evaluation eval = new Evaluation(patternIdEntitiesMap, resultPath + file);
            HashMap<String, Double> propertyThresholdMap = EmpiricalThresholdHelper.getThresholdMap(eval.alpha, eval.beta, embdTyp);

            HashMap<Integer, HashMap<String, String>> patternPredictionMap = eval.getPatternPredictionMap();
            for (int sentId : sentIdPropertyEntitiesMap.keySet()) {
                HashMap<String, Set<HashMap<String, String>>> propertyEntitiesMap =
                        sentIdPropertyEntitiesMap.get(sentId);
                Set<Integer> patternIdsForSentence = sentIdPatternIdsMap.get(sentId) == null ?
                        new HashSet<>() :
                        sentIdPatternIdsMap.get(sentId);

                HashMap<String, Set<HashMap<String, String>>> propertyPredictionMap = new HashMap<>();
                for (int patternId : patternIdsForSentence) {
                    HashMap<String, String> patternDetail = patternPredictionMap.get(patternId);

                    if (patternDetail == null)
                        continue;

                    Set<HashMap<String, String>>  predictionMapSet;
                    String patternProperty = patternDetail.get("property");
                    double score = Double.parseDouble(patternDetail.get("confidence"));

                    double threshold = 0.0;
//                    uncomment below check to get evaluation results after applying empirical threshold
//                    if (propertyThresholdMap.containsKey(patternProperty))
//                        threshold = propertyThresholdMap.get(patternProperty);

                    if (score <= threshold)
                        continue;

                    if (propertyPredictionMap.containsKey(patternProperty)) {
                        predictionMapSet = propertyPredictionMap.get(patternProperty);
                        predictionMapSet.add(patternDetail);
                    } else {
                        predictionMapSet = new HashSet<>();
                        predictionMapSet.add(patternDetail);
                    }
                    propertyPredictionMap.put(patternProperty, predictionMapSet);
                }
                propertyPredictionMap = eval.filterNonSymmetricProperties(propertyPredictionMap);

                for (String patternProp : propertyPredictionMap.keySet()) {
                    Set<HashMap<String, String>> okePropertyEntitiesForSentence = propertyEntitiesMap.get(patternProp);
                    if (okePropertyEntitiesForSentence == null || okePropertyEntitiesForSentence.size() == 0) {
                        falsePositive = falsePositive + propertyPredictionMap.get(patternProp).size();

                        // storing map for threshold calculation
                        for (HashMap<String, String> patternDetail : propertyPredictionMap.get(patternProp)) {
                            List<HashMap<String, Double>> fpScoreMapList;
                            if (propFalsePosi.containsKey(patternProp)) {
                                fpScoreMapList = propFalsePosi.get(patternProp);
                            } else {
                                fpScoreMapList = new ArrayList<>();
                            }

                            HashMap<String, Double> scoreMap = new HashMap<>();
                            scoreMap.put("confidence", Double.valueOf(patternDetail.get("confidence")));
                            scoreMap.put("embedding", Double.valueOf(patternDetail.get("embedding")));

                            fpScoreMapList.add(scoreMap);
                            propFalsePosi.put(patternProp, fpScoreMapList);
                        }
                        continue;
                    }

                    int localTP = 0;
                    int localFN = 0;
                    int localFP = 0;

                    Set<HashMap<String, String>> localTPPatternsSet = new HashSet<>();
                    for (HashMap<String, String> okeSubjObj : okePropertyEntitiesForSentence) {
                        String okeSubj = okeSubjObj.get("subj");
                        String okeObj = okeSubjObj.get("obj");

                        boolean tripleIdentified = false;
                        Set<HashMap<String, String>> predictionsMapSet = propertyPredictionMap.get(patternProp);
                        for (HashMap<String, String> patternDetail : predictionsMapSet) {
                            String patternSubj = patternDetail.get("subj");
                            String patternObj = patternDetail.get("obj");

                            if (okeSubj.equals(patternSubj) && okeObj.equals(patternObj)) {
                                localTP++;
                                tripleIdentified = true;
                                localTPPatternsSet.add(patternDetail);

                                List<HashMap<String, Double>> tpScoreMapList;
                                if (propTruePosi.containsKey(patternProp)) {
                                    tpScoreMapList = propTruePosi.get(patternProp);
                                } else {
                                    tpScoreMapList = new ArrayList<>();
                                }

                                HashMap<String, Double> scoreMap = new HashMap<>();
                                scoreMap.put("confidence", Double.valueOf(patternDetail.get("confidence")));
                                scoreMap.put("embedding", Double.valueOf(patternDetail.get("embedding")));

                                tpScoreMapList.add(scoreMap);
                                propTruePosi.put(patternProp, tpScoreMapList);
                                break;
                            }
                        }

                        if (!tripleIdentified) {
                            localFN++;

                            if (propFalseNeg.containsKey(patternProp)) {
                                int fn = propFalseNeg.get(patternProp);
                                propFalseNeg.put(patternProp, fn + 1);
                            } else {
                                propFalseNeg.put(patternProp, 1);
                            }
                        }
                    }
//                    if (propertyPredictionMap.get(patternProp).size() - localTP > 0)
//                        localFP = propertyPredictionMap.get(patternProp).size() - localTP - localFN;

                    for (HashMap<String, String> patternDetail : propertyPredictionMap.get(patternProp)) {
                        // Extra entities predicted
                        if (!localTPPatternsSet.contains(patternDetail)) {
                            localFP++;
                            List<HashMap<String, Double>> fpScoreMapList;
                            if (propFalsePosi.containsKey(patternProp)) {
                                fpScoreMapList = propFalsePosi.get(patternProp);
                            } else {
                                fpScoreMapList = new ArrayList<>();
                            }

                            HashMap<String, Double> scoreMap = new HashMap<>();
                            scoreMap.put("confidence", Double.valueOf(patternDetail.get("confidence")));
                            scoreMap.put("embedding", Double.valueOf(patternDetail.get("embedding")));

                            fpScoreMapList.add(scoreMap);
                            propFalsePosi.put(patternProp, fpScoreMapList);
                        }
                    }

                    truePositive = truePositive + localTP;
                    falsePositive = falsePositive + localFP;
                    falseNegative = falseNegative + localFN;
                }
            }

            double precision = EvaluationUtils.calculatePrecision(truePositive, falsePositive);
            double recall = EvaluationUtils.calculateRecall(truePositive, falseNegative);
            double fMeasure = EvaluationUtils.fMeasure(precision, recall);

            if (fMeasure > maxFMeasure) {
                maxFMeasure = fMeasure;
                maxAlpha = eval.alpha;
                maxBeta = eval.beta;
            }
            /*System.out.println("alpha: " + eval.alpha + ",\tbeta: " + eval.beta +
                    ",\tPrecision: " + precision + ",\tRecall: " + recall +
                    ",\tTP: " + truePositive + ",\tFP: " + falsePositive + ",\tFN: " + falseNegative +
                    ",\tF-measure: " + fMeasure);*/
            /*System.out.println(eval.alpha + " & " + eval.beta +
                    " & " + precision + " & " + recall +
                    " & " + fMeasure + "\\\\");*/
            System.out.println(eval.alpha + "\t" + eval.beta +
                    "\t" + precision + "\t" + recall +
                    "\t" + fMeasure);

//            System.out.println(eval.alpha + "\t" + eval.beta + "\t" + fMeasure);

            /*
            // for storing granular stats into db
            EmpiricalThresholdHelper.storePredictionScore("oke_prop_true_positive", eval.alpha, eval.beta, embdTyp, propTruePosi);
            EmpiricalThresholdHelper.storePredictionScore("oke_prop_false_positive", eval.alpha, eval.beta, embdTyp, propFalsePosi);
            EmpiricalThresholdHelper.storeFalseNegative(eval.alpha, eval.beta, embdTyp, propFalseNeg);*/
        }
        System.out.println();
        System.out.println("maxAlpha: " + maxAlpha + ",\tmaxBeta: " + maxBeta + ",\tmaxFMeasure: " + maxFMeasure);
    }
}
