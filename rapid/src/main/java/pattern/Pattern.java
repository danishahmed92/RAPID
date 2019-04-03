package pattern;

import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.IndexedWord;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.semgraph.SemanticGraph;
import edu.stanford.nlp.semgraph.SemanticGraphEdge;
import edu.stanford.nlp.util.CoreMap;
import nlp.corenlp.annotator.WordAnnotator;
import nlp.corenlp.parser.SemanticGraphParsing;
import nlp.corenlp.utils.CoreNLPAnnotatorUtils;
import nlp.wordnet.WordNet;
import utils.Utils;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author DANISH AHMED on 12/10/2018
 */
public class Pattern {
    public Node root;
    public SemanticGraph semanticGraph;

    public List<SemanticGraphEdge> rootToSubjPath;
    public List<SemanticGraphEdge> rootToObjPath;

    public String subjPatternStr;
    public String objPatternStr;

    public String subjPatternExt;
    public String objPatternExt;

    public String mergePatternStr;
    public String mergePatternExt;

    public String sgPretty;
    public String sgToSentence;

    public Set<String> distinctNouns = new HashSet<>();
    public Set<String> distinctVerbs = new HashSet<>();

    public Pattern(Node root,
                      String mergePatternStr, String mergePatternExt,
                      Set<String> distinctNouns, Set<String> distinctVerbs) {
        this.root = root;

        this.mergePatternStr = mergePatternStr;
        this.mergePatternExt = mergePatternExt;

        this.distinctNouns = distinctNouns;
        this.distinctVerbs = distinctVerbs;
    }

    public Pattern(SemanticGraph semanticGraph) {
        try {
            this.semanticGraph = semanticGraph;
            this.sgPretty = semanticGraph.toCompactString(true);
            this.sgToSentence = semanticGraph.toRecoveredSentenceString();

            IndexedWord root = semanticGraph.getFirstRoot();
            String rootPOS = root.tag();
            String rootLabel = root.backingLabel().word();

            String rootLemma;
            if (rootPOS.contains("NN")) {
                rootLemma = WordNet.wordNet.getVerbForNoun(root.backingLabel().originalText());
                rootLemma = getWordLemma(rootLemma).toLowerCase();
            } else {
                rootLemma = root.lemma().toLowerCase();
            }

            this.root = new Node(root, rootPOS, rootLabel);
            this.root.setLemma(rootLemma);

            setSubjObjPath();

            subjPatternStr = String.valueOf(setPatternStr(rootToSubjPath));
            objPatternStr = String.valueOf(setPatternStr(rootToObjPath));

            subjPatternExt = String.valueOf(setPatternExtended(rootToSubjPath));
            objPatternExt = String.valueOf(setPatternExtended(rootToObjPath));

            setDistinctNouns();
            setDistinctVerbs();

            setMergePatternStr();
        } catch (NullPointerException npe) {
            npe.printStackTrace();

            this.root = null;
            this.semanticGraph = null;
            this.rootToSubjPath = null;
            this.rootToObjPath = null;
            this.subjPatternStr = null;
            this.objPatternStr = null;
            this.mergePatternStr = null;
            this.sgPretty = null;
            this.sgToSentence = null;
            this.distinctNouns = null;
        }
    }

    public String getWordLemma(String word) {
        String lemma;
        try {
            Annotation newLemmaAnno = CoreNLPAnnotatorUtils.annotateDocument(WordAnnotator.WAInstance.getPipeline(), word);
            List<CoreMap> sentences = newLemmaAnno.get(CoreAnnotations.SentencesAnnotation.class);
            lemma = sentences.get(0).get(CoreAnnotations.TokensAnnotation.class).get(0).lemma();
        } catch (NullPointerException npe) {
            lemma = word;
        }

        if (lemma == null)
            lemma = word;

        return lemma;
    }

    public void setMergePatternStr() {
        final String patternFormat = "{%s}%s{%s}";

        String rootLemma = root.lemma;
        if (rootLemma == null)
            rootLemma = root.label.toLowerCase();

        mergePatternStr = String.format(patternFormat,
                subjPatternStr,
                rootLemma,
                objPatternStr);

        mergePatternExt = String.format(patternFormat,
                subjPatternExt,
                rootLemma,
                objPatternExt);
    }

    public void addNoun(IndexedWord iw) {
        if (iw.tag().equals("NN"))
            distinctNouns.add(WordNet.wordNet.getVerbForNoun(iw.toString()));
    }

    private void setDistinctNouns() {
        Set<IndexedWord> vertexSet = semanticGraph.vertexSet();
        for (IndexedWord iw : vertexSet) {
            if (iw.tag().contains("NN")
                    && !iw.backingLabel().originalText().equals("%R%")
                    && !iw.backingLabel().originalText().equals("%D%")
                    && !iw.ner().toLowerCase().contains("person")
                    // handling 's case
                    && !iw.backingLabel().word().toLowerCase().equals("s") ) {
                String wordLemma = getWordLemma(iw.backingLabel().word()).toLowerCase();
                wordLemma = WordNet.wordNet.getVerbForNoun(wordLemma);

                distinctNouns.add(wordLemma);
            }
        }
    }

    private void setDistinctVerbs() {
        Set<IndexedWord> vertexSet = semanticGraph.vertexSet();
        for (IndexedWord iw : vertexSet) {
            if ((iw.tag().contains("VB") || iw.tag().contains("JJ"))
                    && !iw.backingLabel().originalText().equals("%R%")
                    && !iw.backingLabel().originalText().equals("%D%")

//                    Should not be a stop word
                    && !Utils.utilsInstance.stopWords.contains(iw.backingLabel().word().toLowerCase())) {
                String wordLemma = getWordLemma(iw.backingLabel().word()).toLowerCase();
                wordLemma = WordNet.wordNet.getVerbForNoun(wordLemma);

                distinctVerbs.add(wordLemma);
            }
        }
    }

    private StringBuilder setPatternStr(List<SemanticGraphEdge> path) {
        final String patternFormat = "(%s)-%s>%s";
        StringBuilder pattern = new StringBuilder();
        String lastTag = "";

        SemanticGraphParsing sgp = new SemanticGraphParsing();
        List<String> mergeRelList = sgp.MERGE_TYPED_DEPENDENCIES;
        for (SemanticGraphEdge edge : path) {
            IndexedWord gov = edge.getGovernor();
            IndexedWord dep = edge.getDependent();

            String depStr = (dep.originalText().equals("%D%") || dep.originalText().equals("%R%")) ?
                    dep.originalText() : "";
            String rel = edge.getRelation().getShortName();
            String specific = edge.getRelation().getSpecific();
            if (specific != null && specific.length() > 0)
                rel = rel + ":" + specific;

            if (mergeRelList.contains(rel) &&
                    ((gov.tag().equals(dep.tag()))
                            || gov.tag().equals("JJ")
                            || dep.tag().equals("JJ")))
                continue;

            pattern.append(String.format(patternFormat,
                    gov.tag(),
                    rel,
                    depStr));
            lastTag = "(" + dep.tag() + ")";
        }
        pattern.append(lastTag);
        return pattern;
    }

    private StringBuilder setPatternExtended(List<SemanticGraphEdge> path) {
        final String patternFormat = "(%s/%s)-%s>%s";
        StringBuilder pattern = new StringBuilder();
        String lastTag = "";

        for (SemanticGraphEdge edge : path) {
            IndexedWord gov = edge.getGovernor();
            IndexedWord dep = edge.getDependent();

            String depStr = (dep.originalText().equals("%D%") || dep.originalText().equals("%R%")) ?
                    dep.originalText() : "";
            String rel = edge.getRelation().getShortName();
            String specific = edge.getRelation().getSpecific();
            if (specific != null && specific.length() > 0)
                rel = rel + ":" + specific;

            if (gov.tag().equals(dep.tag()))
                continue;

            String govLabelLemma = getWordLemma(gov.backingLabel().word()).toLowerCase();
            pattern.append(String.format(patternFormat,
                    gov.tag(), govLabelLemma,
                    rel,
                    depStr));
            lastTag = "(" + (dep.tag() + "/" + getWordLemma(dep.backingLabel().word()).toLowerCase()) + ")";
        }
        pattern.append(lastTag);
        return pattern;
    }

    public void setSubjObjPath() {
        List<SemanticGraphEdge> allEdges = semanticGraph.edgeListSorted();
        if (allEdges ==  null || allEdges.isEmpty())
            return;

        Set<IndexedWord> subjIWList = new HashSet<>();
        Set<IndexedWord> objIWList = new HashSet<>();
        for (SemanticGraphEdge edge : allEdges) {
            IndexedWord gov = edge.getGovernor();
            IndexedWord dep = edge.getDependent();

            if (gov.originalText().equals("%D%"))
                subjIWList.add(gov);
            else if (gov.originalText().equals("%R%"))
                objIWList.add(gov);

            if (dep.originalText().equals("%D%"))
                subjIWList.add(dep);
            else if (dep.originalText().equals("%R%"))
                objIWList.add(dep);
        }

        IndexedWord maxSubj = null;
        int maxSubjDepth = -1;
        for (IndexedWord node : subjIWList) {
            int currentDepth = semanticGraph.getShortestUndirectedPathEdges(root.indexedNode, node).size();
            if (currentDepth > maxSubjDepth) {
                maxSubj = node;
                maxSubjDepth = currentDepth;
            }
        }
//        rootToSubjPath = semanticGraph.getShortestUndirectedPathEdges(root.indexedNode, maxSubj);
        rootToSubjPath = semanticGraph.getShortestDirectedPathEdges(root.indexedNode, maxSubj);

        IndexedWord maxObj = null;
        int maxObjDepth = -1;
        for (IndexedWord node : objIWList) {
            int currentDepth = semanticGraph.getShortestUndirectedPathEdges(root.indexedNode, node).size();
            if (currentDepth > maxObjDepth) {
                maxObj = node;
                maxObjDepth = currentDepth;
            }
        }
//        rootToObjPath = semanticGraph.getShortestUndirectedPathEdges(root.indexedNode, maxObj);
        rootToObjPath = semanticGraph.getShortestDirectedPathEdges(root.indexedNode, maxObj);
    }
}
