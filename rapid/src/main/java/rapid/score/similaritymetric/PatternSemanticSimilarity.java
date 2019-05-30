package rapid.score.similaritymetric;

import rapid.score.similaritymetric.fss.ForskipoSemanticSimilarity;
import wordembedding.model.GeneratedModelClassification;

public class PatternSemanticSimilarity {
    private GeneratedModelClassification embeddingClassifier;

    private String sequenceComparision;
    private String sequenceInput;

    private String seqCompSubj;
    private String seqCompObj;
    private String rootComp;

    private String seqInSubj;
    private String seqInObj;
    private String rootIn;

    /**
     *
     * @param sequenceComparision complete pregenerated pattern
     * @param sequenceInput complete pattern generated from input
     * @param embeddingClassifier embedding classifier (w2v / ft/ glove) that FSS will use
     */
    public PatternSemanticSimilarity(String sequenceComparision, String sequenceInput, GeneratedModelClassification embeddingClassifier) {
        this.sequenceComparision = sequenceComparision;
        this.sequenceInput = sequenceInput;
        this.embeddingClassifier = embeddingClassifier;

        setSequenceAttributes();
    }

    /**
     *
     * @return returns similarity between 2 complete pattern sequences; using FSS algorithm
     */
    public double getPatternSemanticSimilarity() {
        double subjSimilarity = getSubjPathSemanticSimilarity();
        double objSimilarity = getObjPathSemanticSimilarity();

        return (subjSimilarity + objSimilarity) / 2;
    }

    /**
     *
     * @return similarity between comp and input pattern's subject path (from root)
     */
    private double getSubjPathSemanticSimilarity() {
        ForskipoSemanticSimilarity fssSubj = new ForskipoSemanticSimilarity(seqCompSubj, seqInSubj,
                rootComp, rootIn,
                embeddingClassifier);
        return fssSubj.getSequenceSimilarity();
    }

    /**
     *
     * @return similarity between comp and input pattern's object path (from root)
     */
    private double getObjPathSemanticSimilarity() {
        ForskipoSemanticSimilarity fssObj = new ForskipoSemanticSimilarity(seqCompObj, seqInObj,
                rootComp, rootIn,
                embeddingClassifier);
        return fssObj.getSequenceSimilarity();
    }

    /**
     * collecting details from pattern sequence
     */
    private void setSequenceAttributes() {
        seqCompSubj = sequenceComparision.substring(1, sequenceComparision.indexOf("}"));
        rootComp = sequenceComparision.substring(sequenceComparision.indexOf("}") + 1, sequenceComparision.lastIndexOf("{"));
        seqCompObj = sequenceComparision.substring(sequenceComparision.lastIndexOf("{") + 1, sequenceComparision.length() - 1);

        seqInSubj = sequenceInput.substring(1, sequenceInput.indexOf("}"));
        rootIn = sequenceInput.substring(sequenceInput.indexOf("}") + 1, sequenceInput.lastIndexOf("{"));
        seqInObj = sequenceInput.substring(sequenceInput.lastIndexOf("{") + 1, sequenceInput.length() - 1);
    }

    public static void main(String[] args) {
        GeneratedModelClassification embeddingClassifier = GeneratedModelClassification.synsetW2VClassification;
        String patternComp = "{(NNS/sign)-compound>%D%(NN/numericable)}sign{(NNS/sign)-compound>%R%(NNP/altice)}";
        String patternIn = "{(NNS/contract)-compound>%D%(NN/numericable)}contract{(NNS/contract)-compound>%R%(NNP/altice)}";

        PatternSemanticSimilarity pss = new PatternSemanticSimilarity(patternComp, patternIn, embeddingClassifier);
        double patternSimilarity = pss.getPatternSemanticSimilarity();
        System.out.println(patternSimilarity);
    }
}
