package nlp.corenlp.ner;

import edu.stanford.nlp.ling.IndexedWord;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.semgraph.SemanticGraph;
import edu.stanford.nlp.util.CoreMap;
import nlp.corenlp.annotator.CoreferenceAnnotator;
import nlp.corenlp.annotator.ParseAnnotator;
import nlp.corenlp.utils.CoreNLPAnnotatorUtils;
import nlp.corenlp.utils.DependencyTreeUtils;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author DANISH AHMED
 */
public class StanfordNER implements NER {
    private final HashMap<String, Set<String>> nerMap = new HashMap<>();

    public StanfordNER(Annotation annotation) {
        setNerMap(annotation);
    }

    public StanfordNER(CoreMap sentence) {
        setSentenceNerMap(sentence);
    }

    public HashMap<String, Set<String>> getNerMap() {
        return nerMap;
    }

    public void setNerMap(Annotation annotation) {
        List<CoreMap> sentences = DependencyTreeUtils.getSentences(annotation);
        for (CoreMap sentence : sentences) {
            setSentenceNerMap(sentence);
        }
    }

    public void setSentenceNerMap(CoreMap sentence) {
        SemanticGraph semanticGraph = DependencyTreeUtils.getDependencyParse(sentence);
        List<IndexedWord> verticesSorted = semanticGraph.vertexListSorted();

        StringBuilder entity = new StringBuilder();
        String entityType = "";
        for (IndexedWord vertex : verticesSorted) {
            if (vertex.ner().equals(entityType)) {
                entity.append(" ").append(vertex.backingLabel().word());
            } else {
                setEntityForType(entity.toString(), entityType);

                entity = new StringBuilder(vertex.backingLabel().word());
                entityType = vertex.ner();
            }
        }
        if (!entity.toString().equals("") && !entity.toString().equals(".")
                && !entityType.equals(""))
            setEntityForType(entity.toString(), entityType);

        nerMap.remove("O");
    }

    private void setEntityForType(String entity, String entityType) {
        Set<String> entitiesForType;
        if (nerMap.containsKey(entityType)) {
            entitiesForType = nerMap.get(entityType);
            entitiesForType.add(entity);
            nerMap.put(entityType, entitiesForType);
        } else {
            entitiesForType = new HashSet<>();
            if (!entity.equals("")) {
                entitiesForType.add(entity);
                nerMap.put(entityType, entitiesForType);
            }
        }
    }

    public static void main(String[] args) {
        String context = "Harvard University is a private Ivy League research university in Cambridge, Massachusetts. " +
                "Established in 1636 and named for its first benefactor clergyman John Harvard, Harvard is the United States' oldest institution of higher learning, and its history, influence, and wealth have made it one of the world's most prestigious universities. " +
                "The Harvard Corporation is its first chartered corporation. " +
                "Although never formally affiliated with any denomination, the early College primarily trained Congregational and Unitarian clergy. " +
                "Its curriculum and student body were gradually secularized during the 18th century, and by the 19th century, Harvard had emerged as the central cultural establishment among Boston elites. " +
                "Following the American Civil War, President Charles W. Eliot's long tenure (1869–1909) transformed the college and affiliated professional schools into a modern research university; " +
                "Harvard was a founding member of the Association of American Universities in 1900.";

        CoreferenceAnnotator coreference = CoreferenceAnnotator.CRInstance;
        Annotation document = CoreNLPAnnotatorUtils.annotateDocument(coreference.getPipeline(), context);

        Set<String> corefLabelSet = new HashSet<>();
        corefLabelSet.add("Harvard University");
        corefLabelSet.add("Association of American Universities");

        List<String> corefSentences = coreference.getCoreferenceReplacedSentences(document, corefLabelSet);
        String corefedContext = String.join(" ", corefSentences);

        ParseAnnotator parseAnnotator = ParseAnnotator.PAInstance;
        Annotation nerAnnotation = CoreNLPAnnotatorUtils.annotateDocument(parseAnnotator.getPipeline(), corefedContext);
        StanfordNER ner = new StanfordNER(nerAnnotation);
        System.out.println(ner.getNerMap());
    }
}
