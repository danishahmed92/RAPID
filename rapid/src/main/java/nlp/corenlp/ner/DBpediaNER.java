package nlp.corenlp.ner;

import edu.stanford.nlp.ling.IndexedWord;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.semgraph.SemanticGraph;
import edu.stanford.nlp.util.CoreMap;
import nlp.corenlp.annotator.CoreferenceAnnotator;
import nlp.corenlp.annotator.ParseAnnotator;
import nlp.corenlp.utils.CoreNLPAnnotatorUtils;
import nlp.corenlp.utils.DependencyTreeUtils;
import org.apache.jena.query.*;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author DANISH AHMED
 */
@Deprecated
public class DBpediaNER implements NER {
    private final HashMap<String, Set<String>> nerMap = new HashMap<>();
    private Set<String> ignoreStanfordEntityType = new HashSet<String>() {{
        add("TITLE");
        add("PERSON");
        add("ORGANIZATION");
        add("LOCATION");

        add("MISC");

        add("CITY");
        add("COUNTRY");
        add("NATIONALITY");
        add("STATE_OR_PROVINCE");

        add("DATE");

        add("TIME");
        add("PERCENT");
        add("MONEY");
    }};

    /**
     *
     * @param annotation annotated document from which entities are recognized
     */
    public DBpediaNER(Annotation annotation) {
        StanfordNER ner = new StanfordNER(annotation);
        nerMap.putAll(ner.getNerMap());

        setNerMap(annotation);
    }

    /**
     *
     * @param sentence sentence to create annotation and get ner
     */
    public DBpediaNER(CoreMap sentence) {
        StanfordNER ner = new StanfordNER(sentence);
        nerMap.putAll(ner.getNerMap());

        setSentenceNerMap(sentence);
    }

    /**
     * for all the sentences in annotation, it sets the ner map
     * @param annotation annotated document
     */
    @Override
    public void setNerMap(Annotation annotation) {
        List<CoreMap> sentences = DependencyTreeUtils.getSentences(annotation);
        for (CoreMap sentence : sentences) {
            setSentenceNerMap(sentence);
        }
    }

    /**
     * indentifying entities for sentences
     * @param sentence coreMap sentence - annotated
     */
    @Override
    public void setSentenceNerMap(CoreMap sentence) {
        SemanticGraph semanticGraph = DependencyTreeUtils.getDependencyParse(sentence);
        List<IndexedWord> verticesSorted = semanticGraph.vertexListSorted();

        StringBuilder entity = new StringBuilder();
        for (IndexedWord vertex : verticesSorted) {
            String stanfordEntity = vertex.ner();
            if (ignoreStanfordEntityType.contains(stanfordEntity)) {
                if (!entity.toString().trim().equals("")) {
                    setEntityTypeFromDBpedia(entity.toString());
                }
                entity = new StringBuilder();
                continue;
            }

            if (vertex.tag().contains("NN")) {
                entity.append(vertex.backingLabel().word());
            } else {
                //get type of entity
                if (!entity.toString().trim().equals("")) {
                    setEntityTypeFromDBpedia(entity.toString());
                }
                entity = new StringBuilder();
            }
        }

        if (!entity.toString().equals("") && !entity.toString().equals(".")) {
            setEntityTypeFromDBpedia(entity.toString());
        }
    }

    /**
     * get possible entity types of entity names
     * using sparql query based on label's frequency
     * to obtain class of top label
     * @param entity entity label
     */
    private void setEntityTypeFromDBpedia(String entity) {
        System.out.println("Entity: " + entity + "\tEntity_END");
        final String queryBIF = "PREFIX owl: <http://www.w3.org/2002/07/owl#>\n" +
                "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n" +
                "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n" +
                "PREFIX ns: <http://www.domain.com/your/namespace/>\n" +
                "\n" +
                "SELECT ?superClass (count(distinct ?s) as ?count) WHERE { \n" +
                "      ?s rdfs:label ?label .\n" +
                "\n" +
                "      ?s rdf:type ?type .\n" +
                "      ?type rdfs:subClassOf ?superClass .\n" +
                "      ?label <bif:contains> \"%s\" . \n" +
                "\n" +
                "      Filter (regex (?type, \"dbpedia.org/ontology\") && regex (?superClass, \"dbpedia.org/ontology\")) .\n" +
                "  \t  FILTER (lang(?label) = 'en') .\n" +
                "}\n" +
                "group by ?superClass\n" +
                "order by desc(?count)\n" +
                "limit 5";

        final String queryRegex = "PREFIX owl: <http://www.w3.org/2002/07/owl#>\n" +
                "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n" +
                "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n" +
                "PREFIX ns: <http://www.domain.com/your/namespace/>\n" +
                "\n" +
                "SELECT ?superClass (count(distinct ?s) as ?count) WHERE { \n" +
                "      ?s rdfs:label ?label .\n" +
                "\n" +
                "      ?s rdf:type ?type .\n" +
                "      ?type rdfs:subClassOf ?superClass .\n" +
                "\n" +
                "      Filter (regex (?type, \"dbpedia.org/ontology\") && regex (?superClass, \"dbpedia.org/ontology\")) .\n" +
                "  \t  Filter (regex(?label, \"%s\", 'i')) .\n" +
                "  \t  FILTER (lang(?label) = 'en') .\n" +
                "}\n" +
                "group by ?superClass\n" +
                "order by desc(?count)\n" +
                "limit 5";

        String queryToExecute;
        if (entity.contains(" ") || entity.contains("'"))
            queryToExecute = String.format(queryRegex, entity);
        else
            queryToExecute = String.format(queryBIF, entity);

        Query query = QueryFactory.create(queryToExecute);
        String SPARQL_ENDPOINT = "http://dbpedia.org/sparql";

        double tolerance = 0.2;
        double highest = 0.0;
        boolean setTolerance = true;
        try (QueryExecution qexec = QueryExecutionFactory.sparqlService(SPARQL_ENDPOINT, query)) {
            ResultSet results = qexec.execSelect();
            while (results.hasNext()) {
                QuerySolution soln = results.nextSolution();
                String entityType = soln.getResource("superClass").toString();
                entityType = entityType.replaceAll("http://dbpedia.org/ontology/", "");
                entityType = entityType.toLowerCase();

                if (setTolerance) {
                    highest = Double.parseDouble(String.valueOf(soln.getLiteral("count").getInt()));
                    setTolerance = false;
                }

                double score = Double.parseDouble(String.valueOf(soln.getLiteral("count").getInt()));
                if (score >= (highest * tolerance)) {
                    if (entityType.contains("location")
                            || entityType.contains("place"))
                        setEntityForType(entity, "LOCATION");
                    if (entityType.contains("person"))
                        setEntityForType(entity, "PERSON");
                    if (entityType.contains("organisation")
                            || entityType.contains("group"))
                        setEntityForType(entity, "ORGANIZATION");
                }
            }
        }
    }

    /**
     *
     * @param entity entity label
     * @param entityType it's type / class
     */
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

    /**
     *
     * @return sentence ner map
     */
    @Override
    public HashMap<String, Set<String>> getNerMap() {
        return this.nerMap;
    }

    public static void main(String[] args) {
        String context = "Zayn Malik Leaves One Direction.";

        CoreferenceAnnotator coreference = CoreferenceAnnotator.CRInstance;
        Annotation document = CoreNLPAnnotatorUtils.annotateDocument(coreference.getPipeline(), context);

        Set<String> corefLabelSet = new HashSet<>();
        List<String> corefSentences = coreference.getCoreferenceReplacedSentences(document, corefLabelSet);
        String corefedContext = String.join(" ", corefSentences);

        ParseAnnotator parseAnnotator = ParseAnnotator.PAInstance;
        Annotation nerAnnotation = CoreNLPAnnotatorUtils.annotateDocument(parseAnnotator.getPipeline(), corefedContext);
        DBpediaNER ner = new DBpediaNER(nerAnnotation);
        System.out.println(ner.getNerMap());
    }
}
