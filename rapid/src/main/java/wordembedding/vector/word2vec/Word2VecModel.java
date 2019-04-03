package wordembedding.vector.word2vec;

import config.IniConfig;
import org.deeplearning4j.models.word2vec.Word2Vec;
import wordembedding.vector.VectorModel;

import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * @author DANISH AHMED on 10/22/2018
 */
public class Word2VecModel extends VectorModel {
    public static Word2VecModel word2vecInstance;
    static {
        try {
            word2vecInstance = new Word2VecModel();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public Word2Vec word2Vec;
    public Word2VecModel() throws FileNotFoundException {
        word2Vec = setVectorModel(IniConfig.configInstance.word2vec);
    }
}