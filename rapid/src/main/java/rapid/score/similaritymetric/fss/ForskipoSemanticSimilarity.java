package rapid.score.similaritymetric.fss;


import org.deeplearning4j.models.word2vec.Word2Vec;
import org.simmetrics.StringMetric;
import org.simmetrics.metrics.StringMetrics;
import wordembedding.model.GeneratedModelClassification;

import java.util.HashMap;
import java.util.LinkedHashMap;

public class ForskipoSemanticSimilarity extends MatrixForwardSkipping{
    private String rootComp = "";
    private String rootIn = "";
    private String seqComp = "";
    private String seqIn = "";

    private boolean IGNORE_ENTITY_NNP = false;

    private HashMap<Integer, HashMap<String, String>> seqComDetailMap = new LinkedHashMap<>();
    private HashMap<Integer, HashMap<String, String>> seqInDetailMap = new LinkedHashMap<>();

    private double rowNodeCount = 0;

    private Word2Vec embeddingModel;
    private StringMetric similarityMetric = StringMetrics.cosineSimilarity();

    /**
     *
     * @param seqComp subsequence to be compared (subj/obj path from root); of pregenerated pattern
     * @param seqIn subsequence to be compared (subj/obj path from root); of pattern generated from input sentence
     * @param rootComp root of pregenerated pattern
     * @param rootIn root of input pattern
     * @param embeddingClassifier embedding classifier to use (w2v / glove / ft)
     */
    public ForskipoSemanticSimilarity(String seqComp, String seqIn, String rootComp, String rootIn, GeneratedModelClassification embeddingClassifier) {
        this.seqComp = seqComp;
        this.seqIn = seqIn;
        this.rootComp = rootComp;
        this.rootIn = rootIn;

        this.embeddingModel = embeddingClassifier.sourceModel;
    }

    /**
     * given 2 sequences (path of either subject or object), it calculates semantic similarity using forward matrix
     * @return returns similarity score of 2 sequences by using FSS algorithm
     */
    public double getSequenceSimilarity() {
        if (seqComp != null && seqIn != null) {
            seqComp = seqComp.replaceAll("\\{", "");
            seqComp = seqComp.replaceAll("\\}", "");

            seqIn = seqIn.replaceAll("\\{", "");
            seqIn = seqIn.replaceAll("\\}", "");
        }

        if (seqComp == null || seqIn  == null
                || seqComp.length() == 0 || seqIn.length() == 0) {
            return 0.0;
        } else {
            seqComDetailMap = parseSequence(seqComp);
            seqInDetailMap = parseSequence(seqIn);

            if (seqComDetailMap.size() == 0 || seqInDetailMap.size() == 0)
                return 0.0;

            double[][] similarityMatrix = formulateMatrix();
            double[][] sumMatrix = generateSummationMatrixOptimized(similarityMatrix);
            double rootSim;

            try {
                rootSim = embeddingModel.similarity(rootComp, rootIn);
                rootSim = normalizeEmbeddingScore(rootSim);
            } catch (Exception ignored) {
                rootSim = 0.0D / 0.0;
            }

            return (rootSim + getMaxValueMatrix(sumMatrix)) / (rowNodeCount + 1);
        }
    }

    /**
     *
     * @param embeddingScore score from embedding classifier
     * @return normalized score
     */
    private double normalizeEmbeddingScore(double embeddingScore) {
        return (embeddingScore + 1) / (2);
    }

    /**
     *
     * @param sequence pattern sequence; so it can be transformed for sequence alignment (matrix)
     * @return position - sequence detail map
     */
    private HashMap<Integer, HashMap<String, String>> parseSequence(String sequence) {
        HashMap<Integer, HashMap<String, String>> matrixIndexSeqDetailMap = new LinkedHashMap<>();

        if (sequence != null && sequence.length() > 0) {
            sequence = sequence.replaceAll("\\{", "");
            sequence = sequence.replaceAll("\\}", "");
        }

        if (sequence == null || sequence.length() == 0)
            return matrixIndexSeqDetailMap;

        int rootEndPosi = sequence.indexOf(")-"); // first position
        String remainSeq = sequence.substring(rootEndPosi + 1);
        String[] matrixAttrSplit = remainSeq.split("\\)-");

        int detailMapCounter = 0;
        for (int i = 0; i < matrixAttrSplit.length; i++) {
            String typDep = matrixAttrSplit[i].split(">")[0];
            String POSLabel = matrixAttrSplit[i].split(">")[1];

            if (typDep.charAt(0) == '-') {
                typDep = typDep.substring(1);
            }

            String pos = getPOSFromPOSSeq(POSLabel);
            String label = getLabelFromPOSSeq(POSLabel);

            if (pos == null || pos.length() == 0)
                continue;

            if (label.charAt(label.length() - 1) == ')') {
                if (!label.contains("("))
                    label = label.substring(0, label.length() - 1);
            }

            HashMap<String, String> seqDetailMap = new HashMap<>();
            seqDetailMap.put("typd", typDep);
            seqDetailMap.put("pos", pos);
            seqDetailMap.put("label", label);

            matrixIndexSeqDetailMap.put(detailMapCounter, seqDetailMap);
            detailMapCounter++;
        }
        return matrixIndexSeqDetailMap;
    }

    /**
     *
     * @param posSeq part of sequence having label
     * @return label (that was appended with POS in our pattern)
     */
    private String getLabelFromPOSSeq(String posSeq) {
        String[] split = posSeq.split("/");
        return split[1];
    }

    /**
     *
     * @param posSeq part of sequence having label / %D% / %R%
     * @return transformed string that is used for matrix population
     */
    private String getPOSFromPOSSeq(String posSeq) {
        String[] split = posSeq.split("/");

        // for handling %D%, %R%
        int firstBracketPosi = split[0].indexOf("(");
        String pos = split[0].substring(firstBracketPosi + 1);

        if (split[0].contains("%D%")) {
            if (!IGNORE_ENTITY_NNP)
                pos = "%D% " + pos;
            else {
                if (pos.contains("NNP"))
                    return null;
                else
                    pos = "%D% " + pos;
            }
        }
        else if (split[0].contains("%R%")) {
            if (!IGNORE_ENTITY_NNP)
                pos = "%R% " + pos;
            else {
                if (pos.contains("NNP"))
                    return null;
                else
                    pos = "%R% " + pos;
            }

        }
        return pos;
    }

    /**
     * populate matix using formula (double value for each block)
     * @return matrix with each block possessing similarity value of aligned comp-in pattern subpart
     */
    private double[][] formulateMatrix() {
        HashMap<Integer, HashMap<String, String>> columnMatrix;
        HashMap<Integer, HashMap<String, String>> rowMatrix;

        if (seqComDetailMap.size() > seqInDetailMap.size()) {
            columnMatrix = new LinkedHashMap<>(seqComDetailMap);
            rowMatrix = new LinkedHashMap<>(seqInDetailMap);
        } else {
            columnMatrix = new LinkedHashMap<>(seqInDetailMap);
            rowMatrix = new LinkedHashMap<>(seqComDetailMap);
        }

        int columnSize = columnMatrix.size();
        int rowSize = rowMatrix.size();

        rowNodeCount = rowSize / 1.0;

        double[][] matrix = new double[rowSize][columnSize];
        for (int i = 0; i < rowSize; i++) {
            HashMap<String, String> rowBlock = rowMatrix.get(i);

            String rowTypD = rowBlock.get("typd");
            String rowPOS = rowBlock.get("pos");
            String rowLabel = rowBlock.get("label");

            rowPOS = transformForCosine(rowPOS);
            rowTypD = transformForCosine(rowTypD);

            for (int j = 0; j < columnSize; j++) {
                HashMap<String, String> columnBlock = columnMatrix.get(j);

                String colTypD = columnBlock.get("typd");
                String colPOS = columnBlock.get("pos");
                String colLabel = columnBlock.get("label");

                double esLabel;
                try {
                    esLabel = embeddingModel.similarity(rowLabel, colLabel);
                } catch(Exception ignored) {
                    esLabel = 0.0D / 0.0;
                }

                colPOS = transformForCosine(colPOS);
                colTypD = transformForCosine(colTypD);

                double cosinePOS = similarityMetric.compare(colPOS, rowPOS);
                double cosineTypD = similarityMetric.compare(colTypD, rowTypD);

                double nodeScore = (0.5 + cosinePOS + esLabel);
                double edgeScore = (0.5 + cosineTypD);
                double nodeEdgeScore = nodeScore * edgeScore;

                matrix[i][j] = normalizeValue(nodeEdgeScore);
            }
        }
        return matrix;
    }

    /**
     *
     * @param matrixBlockValue similarity score of subpart pattern (comp, in)
     * @return normalized similarity value
     */
    private double normalizeValue(double matrixBlockValue) {
        return (matrixBlockValue + 0.25) / (3.75 + 0.25);  // since our values can range from -0.25 to +3.75
    }

    /**
     *
     * @param str typed dependency / POS
     * @return space separated typed dependency / POS so it can be transformed into vector for calculating cosine similarity
     */
    private String transformForCosine(String str) {
        if (str.contains(" ")) {
            String[] split = str.split(" ");
            split[1] = split[1].replace("", " ").trim();
            str = split[0] + " " + split[1];
        } else {
            str = str.replace("", " ").trim();
        }
        return  str;
    }

    public static void main(String[] args) {
        GeneratedModelClassification embeddingClassifier = GeneratedModelClassification.synsetW2VClassification;

        String seqCom = "(CD/one)-nmod:of>(NNS/deny)-numod:of>%R%(NNP/advisor)-nmod:of>(NNP/student)-case>%R%(IN/in)";
        String seqIn = "(CD/two)-nmod:of>(NNS/refuse)-numod:of>%R%(NNP/supervisor)-nmod:of>(NNP/advisor)";

        ForskipoSemanticSimilarity sss = new ForskipoSemanticSimilarity(seqCom, seqIn, "advise", "supervise", embeddingClassifier);
        double patternSemanticSimilarity = sss.getSequenceSimilarity();
        System.out.println(patternSemanticSimilarity);
    }
}
