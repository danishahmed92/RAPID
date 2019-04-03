package nlp.corenlp.ner;

import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.util.CoreMap;

import java.util.HashMap;
import java.util.Set;

/**
 * @author DANISH AHMED
 */
interface NER {
    void setNerMap(Annotation annotation);
    void setSentenceNerMap(CoreMap sentence);

    HashMap<String, Set<String>> getNerMap();
}