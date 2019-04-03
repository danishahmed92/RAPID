package nlp.corenlp.annotator;

import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import nlp.corenlp.CoreNLP;

import java.util.Properties;

/**
 * @author DANISH AHMED
 */
public class WordAnnotator implements CoreNLP {
    private StanfordCoreNLP pipelineWord;

    public static final WordAnnotator WAInstance;
    static {
        WAInstance = new WordAnnotator();
    }

    @Override
    public Properties setProperties() {
        Properties props = new Properties();
        props.setProperty("annotators", "tokenize,ssplit,pos,lemma,ner");
        return props;
    }

    @Override
    public StanfordCoreNLP setPipeLine(Properties props) {
        return new StanfordCoreNLP(props);
    }

    private WordAnnotator() {
        this.pipelineWord = setPipeLine(setProperties());
    }

    @Override
    public StanfordCoreNLP getPipeline() {
        return pipelineWord;
    }
}
