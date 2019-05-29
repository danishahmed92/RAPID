package nlp.corenlp.annotator;

import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import nlp.corenlp.CoreNLP;

import java.util.Properties;

/**
 * @author DANISH AHMED
 */
public class ParseAnnotator implements CoreNLP {
    private StanfordCoreNLP pipelineParse;

    public static final ParseAnnotator PAInstance;
    static {
        PAInstance = new ParseAnnotator();
    }

    @Override
    /**
     * pipeline properties required for 2nd pass annotation
     */
    public Properties setProperties() {
        Properties props = new Properties();
        props.setProperty("annotators", "tokenize,ssplit,pos,lemma,ner,parse");
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
    private ParseAnnotator() {
        this.pipelineParse = setPipeLine(setProperties());
    }

    @Override
    /**
     * get coreNLP pipeline
     */
    public StanfordCoreNLP getPipeline() {
        return pipelineParse;
    }
}
