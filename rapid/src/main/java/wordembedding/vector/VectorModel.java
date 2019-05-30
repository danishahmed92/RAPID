package wordembedding.vector;

import org.deeplearning4j.models.embeddings.loader.WordVectorSerializer;
import org.deeplearning4j.models.word2vec.Word2Vec;

import java.io.File;

/**
 * @author DANISH AHMED on 10/22/2018
 */
public class VectorModel {
    private Word2Vec vectorModel;

    /**
     *
     * @param textModelFile embedding model file name
     * @return Embedding Classifier Model (DL4j)
     */
    public Word2Vec setVectorModel(String textModelFile) {
        vectorModel = readVectorModel(textModelFile);
        return vectorModel;
    }

    /**
     *
     * @param modelFile Java.io.File
     * @return Embedding Classifier Model (DL4j)
     */
    public Word2Vec setVectorModel(File modelFile) {
        vectorModel = readVectorModel(modelFile);
        return vectorModel;
    }

    /**
     *
     * @param modelFilePath Java.io.File
     * @return Embedding Classifier Model (DL4j)
     */
    private Word2Vec readVectorModel(File modelFilePath) {
        return WordVectorSerializer.readWord2VecModel(modelFilePath);
    }

    /**
     *
     * @param modelFilePath embedding model file name
     * @return Embedding Classifier Model (DL4j)
     */
    private Word2Vec readVectorModel(String modelFilePath) {
        return WordVectorSerializer.readWord2VecModel(modelFilePath);
    }
}