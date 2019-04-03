package wordembedding.vector.fasttext;

import config.IniConfig;
import org.deeplearning4j.models.word2vec.Word2Vec;
import wordembedding.vector.VectorModel;

import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * @author DANISH AHMED on 10/22/2018
 */
public class FastTextModel extends VectorModel {
    public static FastTextModel fastTextInstance;
    static {
        try {
            fastTextInstance = new FastTextModel();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Word2Vec fastText;
    public FastTextModel() throws FileNotFoundException {
        fastText = setVectorModel(IniConfig.configInstance.fastText);
    }
}
