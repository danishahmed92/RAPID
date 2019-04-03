package wordembedding.vector.glove;

import config.IniConfig;
import org.deeplearning4j.models.word2vec.Word2Vec;
import wordembedding.vector.VectorModel;

import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * @author DANISH AHMED on 10/22/2018
 */
public class GloveModel extends VectorModel {
    public static GloveModel gloveInstance;

    static {
        try {
            gloveInstance = new GloveModel();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Word2Vec glove;

    public GloveModel() throws FileNotFoundException {
        glove = setVectorModel(IniConfig.configInstance.glove);
    }
}