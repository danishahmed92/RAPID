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
    /**
     * pipeline properties required for getting lemma and pos for patterns
     */
    public Properties setProperties() {
        Properties props = new Properties();
        props.setProperty("annotators", "tokenize,ssplit,pos,lemma,ner");
        return props;
    }

    @Override
    /**
     * initializing CoreNLP pipeline
     */
    public StanfordCoreNLP setPipeLine(Properties props) {
        return new StanfordCoreNLP(props);
    }

    /**
     * setting pipleline and properties
     */
    private WordAnnotator() {
        this.pipelineWord = setPipeLine(setProperties());
    }

    @Override
    /**
     * get coreNLP pipeline
     */
    public StanfordCoreNLP getPipeline() {
        return pipelineWord;
    }
}
