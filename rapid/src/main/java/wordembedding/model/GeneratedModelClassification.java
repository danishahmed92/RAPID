package wordembedding.model;

import config.IniConfig;
import org.deeplearning4j.models.word2vec.Word2Vec;
import org.nd4j.linalg.api.ndarray.INDArray;
import wordembedding.vector.VectorModel;
import wordembedding.vector.VectorModelUtils;

import java.util.HashMap;
import java.util.List;

/**
 * @author DANISH AHMED on 1/27/2019
 */
public class GeneratedModelClassification {
    public Word2Vec sourceModel;
    public Word2Vec generatedModel;
    public String classifierName;

    public static GeneratedModelClassification synsetW2VClassification;
    public static GeneratedModelClassification synsetGloveClassification;
    public static GeneratedModelClassification synsetFTClassification;

    static {
        IniConfig config = IniConfig.configInstance;

        synsetW2VClassification = new GeneratedModelClassification(config.word2vec, config.propertySynsetW2V, "w2v");
        synsetGloveClassification = new GeneratedModelClassification(config.glove, config.propertySynsetGlove, "glove");
        synsetFTClassification = new GeneratedModelClassification(config.fastText, config.propertySynsetFT, "ft");
    }

    private GeneratedModelClassification(String sourceModelPath, String generatedModelPath, String classifierName) {
        VectorModel vectorModelSource = new VectorModel();
        VectorModel vectorModelGenerated = new VectorModel();

        sourceModel = vectorModelSource.setVectorModel(sourceModelPath);
        generatedModel = vectorModelGenerated.setVectorModel(generatedModelPath);
        this.classifierName = classifierName;
    }

    public HashMap<String, Double> getNearestProperties(List<String> wordList, int limit) {
        try {
            INDArray wordListVecMean = VectorModelUtils.getMeanVecFromWordList(sourceModel, wordList);

            // the above calculated mean vector will be compared against generated model to get nearest property
            VectorModelUtils modelUtils = new VectorModelUtils();
            return modelUtils.vectorNearest(generatedModel, wordListVecMean, limit);
        } catch (NullPointerException | IllegalStateException e) {
            e.printStackTrace();
            return new HashMap<>();
        }
    }

    public double getSimilarityOfWordsWithProperty(List<String> wordList, String property) {
        try {
            INDArray wordListVecMean = VectorModelUtils.getMeanVecFromWordList(sourceModel, wordList);
            return VectorModelUtils.getSimilarity(generatedModel, property, wordListVecMean);
        } catch (NullPointerException | IllegalStateException e) {
            e.printStackTrace();
            return 0.0D / 0.0;
        }
    }

    public HashMap<String, Double> getNearestPropertiesFromWord(String word, int limit) {
        INDArray wordVec = sourceModel.getWordVectorMatrix(word);
        if (wordVec == null)
            return null;

        VectorModelUtils modelUtils = new VectorModelUtils();
        return modelUtils.vectorNearest(generatedModel, wordVec, limit);
    }
}
