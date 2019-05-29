package nlp.corenlp.utils;

import edu.stanford.nlp.coref.CorefCoreAnnotations;
import edu.stanford.nlp.coref.data.CorefChain;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.IndexedWord;
import edu.stanford.nlp.ling.SentenceUtils;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.semgraph.SemanticGraph;
import edu.stanford.nlp.semgraph.SemanticGraphCoreAnnotations;
import edu.stanford.nlp.util.CoreMap;

import java.util.*;

/**
 * @author DANISH AHMED
 */
public class DependencyTreeUtils {
    /**
     *
     * @param sentence coreMap sentence (got from annotation)
     * @return semantic graph
     */
    public static SemanticGraph getDependencyParse(CoreMap sentence) {
        return sentence.get(SemanticGraphCoreAnnotations.EnhancedDependenciesAnnotation.class);
    }

    /**
     *
     * @param sentence coreMap sentence (got from annotation)
     * @return CoreLabels; all the tokens present in sentence
     */
    public static List<CoreLabel> getLabelsFromSentence(CoreMap sentence) {
        return sentence.get(CoreAnnotations.TokensAnnotation.class);
    }

    /**
     *
     * @param annotation annotated document
     * @return list of coreMap sentences present in annotation
     */
    public static List<CoreMap> getSentences(Annotation annotation) {
        return annotation.get(CoreAnnotations.SentencesAnnotation.class);
    }

    /**
     *
     * @param document annotated document
     * @return map of mention's cluster id and corefChain
     */
    public static Map<Integer, CorefChain> getClusterIdCorefChainMap(Annotation document) {
        return document.get(CorefCoreAnnotations.CorefChainAnnotation.class);
    }

    /**
     *
     * @param annotation annotated document
     * @return list of recovered sentences from annotated document
     */
    public static List<String> getStringSentences(Annotation annotation) {
        List<String> sentenceList = new LinkedList<>();
        List<CoreMap> sentences = getSentences(annotation);
        for(CoreMap sentence : sentences){
            List<CoreLabel> labels = getLabelsFromSentence(sentence);
            String sentenceString = SentenceUtils.listToOriginalTextString(labels);
            sentenceList.add(sentenceString);
        }
        return sentenceList;
    }

    /**
     *
     * @param clusterIdCorefChainMap corefClustId corefChainMap
     * @param corefLabelSet any specific lable that should be used if found in mention
     * @return sets mention (that will be replaced in sentence) against corefClustId
     */
    public static HashMap<Integer, String> setMentionToUseIfExistInCluster(Map<Integer, CorefChain> clusterIdCorefChainMap, Set<String> corefLabelSet) {
        HashMap<Integer, String> clusterIdResourceMentionMap = new HashMap<>();
        if (corefLabelSet != null && corefLabelSet.size() != 0) {
            for (Integer corefClustId : clusterIdCorefChainMap.keySet()) {
                CorefChain corefChain = clusterIdCorefChainMap.get(corefClustId);
                List<CorefChain.CorefMention> mentionList = corefChain.getMentionsInTextualOrder();

                for (CorefChain.CorefMention mention : mentionList) {
                    String mentionString = mention.mentionSpan;
                    if (corefLabelSet.contains(mentionString)) {
                        clusterIdResourceMentionMap.put(corefClustId, mentionString);
                        break;
                    }
                }
            }
        }
        return clusterIdResourceMentionMap;
    }

    /**
     *
     * @param semanticGraph semantic graph
     * @param label entity label
     * @return consecutive nodes that reflects the label in semantic graph
     */
    public static List<IndexedWord> getIndexedWordsFromString(SemanticGraph semanticGraph, String label) {
        List<IndexedWord> indexedWordList = new LinkedList<>();
        if (label == null || label.isEmpty())
            return indexedWordList;

        String[] labelSplit = label.trim().split(" ");
        List<IndexedWord> IWForSplit = semanticGraph.getAllNodesByWordPattern(labelSplit[0]);

        if (labelSplit.length == 1) {
            indexedWordList.addAll(IWForSplit);
            return indexedWordList;
        }

        for (IndexedWord iw : IWForSplit) {
            indexedWordList.add(iw);
            int position = iw.index();
            for (int i = 1; i < labelSplit.length; i++) {
                try {
                    IndexedWord indexedWord = semanticGraph.getNodeByIndex(position + 1);
                    String splitWord = labelSplit[i];
                    String currentWord = indexedWord.originalText();

                    if (currentWord.equals(splitWord))
                        indexedWordList.add(indexedWord);
                    else {
                        indexedWordList.clear();
                        break;
                    }
                    position++;
                } catch (IllegalArgumentException e) {
                    e.printStackTrace();
                    break;
                }
            }
            if (indexedWordList.size() == labelSplit.length)
                break;
        }
        return indexedWordList;
    }
}
