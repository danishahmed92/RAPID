package pattern;

import edu.stanford.nlp.ling.IndexedWord;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.semgraph.SemanticGraph;
import edu.stanford.nlp.util.CoreMap;
import nlp.corenlp.annotator.ParseAnnotator;
import nlp.corenlp.parser.SemanticGraphParsing;
import nlp.corenlp.utils.CoreNLPAnnotatorUtils;
import nlp.corenlp.utils.DependencyTreeUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author DANISH AHMED on 12/7/2018
 */
public class PatternGenerator {
    private Annotation annotation;

    public PatternGenerator(Annotation annotation) {
        this.annotation = annotation;
    }

    public PatternGenerator(String sentence) {
        annotation = CoreNLPAnnotatorUtils.annotateDocument(ParseAnnotator.PAInstance.getPipeline(), sentence);
    }

    public List<Pattern> generatePatterns(String subject, String object) {
        assert annotation != null;

        SemanticGraphParsing sgp = new SemanticGraphParsing();
        Set<SemanticGraph> distinctGraphs = new HashSet<>();

        List<CoreMap> sentences = DependencyTreeUtils.getSentences(annotation);
        for (CoreMap sentence : sentences) {
            SemanticGraph semanticGraph = DependencyTreeUtils.getDependencyParse(sentence);
            List<IndexedWord> subjIWList = DependencyTreeUtils.getIndexedWordsFromString(semanticGraph, subject);
            List<IndexedWord> objIWList = DependencyTreeUtils.getIndexedWordsFromString(semanticGraph, object);

            for (IndexedWord subjIW : subjIWList) {
                for (IndexedWord objIW : objIWList) {
                    SemanticGraph patternGraph = sgp.getGraphBetweenNodes(semanticGraph, subjIW, objIW);
                    if (patternGraph != null)
                        distinctGraphs.add(patternGraph);
                }
            }
        }
        Set<SemanticGraph> nonDupSGs = new HashSet<>(sgp.removeDuplicatedGraphs(distinctGraphs));
        Set<SemanticGraph> prunedAndDRReplacedGraphs = pruneAndReplaceDomainRangeSG(nonDupSGs, subject, object);
        Set<SemanticGraph> nonRootContainSubGraphs = PatternSemanticGraphHelper.removeRootContainSubGraph(prunedAndDRReplacedGraphs);

        return generatePatternsFromSemanticGraphs(nonRootContainSubGraphs);
    }

    private List<Pattern> generatePatternsFromSemanticGraphs(Set<SemanticGraph> nonRootContainSubGraphs) {
        List<Pattern> patterns = new ArrayList<>();
        for (SemanticGraph sg : nonRootContainSubGraphs) {
            Pattern pattern = new Pattern(sg);
            patterns.add(pattern);
        }
        return patterns;
    }

    private Set<SemanticGraph> pruneAndReplaceDomainRangeSG(Set<SemanticGraph> nonDupSGs, String subj, String obj) {
        Set<SemanticGraph> prunedAndDRReplacedGraphs = new HashSet<>();
        for (SemanticGraph sg : nonDupSGs) {
            SemanticGraph prunedGraph = PatternSemanticGraphHelper.pruneGraph(sg, subj, obj);
            SemanticGraph domainRangeReplaced = PatternSemanticGraphHelper.replaceDomainRange(prunedGraph, subj, obj);

            prunedAndDRReplacedGraphs.add(domainRangeReplaced);
        }
        return  prunedAndDRReplacedGraphs;
    }
}
