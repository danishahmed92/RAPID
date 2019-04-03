package nlp.corenlp;

import edu.stanford.nlp.pipeline.StanfordCoreNLP;

import java.util.Properties;

/**
 * @author DANISH AHMED
 */
public interface CoreNLP {
    Properties setProperties();
    StanfordCoreNLP setPipeLine(Properties props);
    StanfordCoreNLP getPipeline();
}
