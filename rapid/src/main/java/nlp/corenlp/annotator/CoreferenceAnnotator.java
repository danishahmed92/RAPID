package nlp.corenlp.annotator;

import edu.stanford.nlp.coref.CorefCoreAnnotations;
import edu.stanford.nlp.coref.data.CorefChain;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.CoreMap;
import nlp.corenlp.CoreNLP;
import nlp.corenlp.utils.CoreNLPAnnotatorUtils;
import nlp.corenlp.utils.DependencyTreeUtils;

import java.util.*;

import static nlp.corenlp.utils.DependencyTreeUtils.getSentences;

/**
 * @author DANISH AHMED
 */
public class CoreferenceAnnotator implements CoreNLP {
    private StanfordCoreNLP pipelineCoreference;

    public static final CoreferenceAnnotator CRInstance;
    static {
        CRInstance = new CoreferenceAnnotator();
    }

    @Override
    public Properties setProperties() {
        Properties props = new Properties();
        props.setProperty("annotators", "tokenize,ssplit,pos,lemma,ner,parse,mention,coref");
        return props;
    }

    @Override
    public StanfordCoreNLP setPipeLine(Properties props) {
        return new StanfordCoreNLP(props);
    }

    private CoreferenceAnnotator() {
        this.pipelineCoreference = setPipeLine(setProperties());
    }

    @Override
    public StanfordCoreNLP getPipeline() {
        return pipelineCoreference;
    }

    public List<String> getCoreferenceReplacedSentences(Annotation document, Set<String> corefLabelSet) {
        List<String> corefSentences = new ArrayList<>();
        Map<Integer, CorefChain> clusterIdCorefChainMap = DependencyTreeUtils.getClusterIdCorefChainMap(document);

        if (clusterIdCorefChainMap == null)
            return DependencyTreeUtils.getStringSentences(document);

        HashMap<Integer, String> clusterIdResourceMentionMap =
                DependencyTreeUtils.setMentionToUseIfExistInCluster(clusterIdCorefChainMap, corefLabelSet);

        int sentCount = 0;
        for (CoreMap sentence : getSentences(document)) {
            sentCount++;
            List<String> sentenceWords = new ArrayList<>();
            List<CoreLabel> tokens = sentence.get(CoreAnnotations.TokensAnnotation.class);

            for (int i = 0; i < tokens.size(); i++) {
                CoreLabel token = tokens.get(i);

                if (token.get(CoreAnnotations.PartOfSpeechAnnotation.class).equals("."))
                    continue;

                Integer corefClustId = token.get(CorefCoreAnnotations.CorefClusterIdAnnotation.class);
                CorefChain corefChain = clusterIdCorefChainMap.get(corefClustId);

                if (corefChain == null) {
                    sentenceWords.add(token.word());
                    continue;
                }

                String firstMention;
                if (clusterIdResourceMentionMap.containsKey(corefClustId))
                    firstMention = clusterIdResourceMentionMap.get(corefClustId);
                else
                    firstMention = corefChain.getMentionsInTextualOrder().get(0).mentionSpan;

                if (token.get(CoreAnnotations.PartOfSpeechAnnotation.class).contains("PRP")) {
                    if ((i + 1) < tokens.size()) {
                        CoreLabel nextToken = tokens.get(i + 1);
                        if (nextToken.get(CoreAnnotations.PartOfSpeechAnnotation.class).contains("NN")
                                || nextToken.get(CoreAnnotations.PartOfSpeechAnnotation.class).contains("JJ"))
                            sentenceWords.add(firstMention + "'s");
                        else
                            sentenceWords.add(firstMention);
                    }
                } else {
                    int sentenceCount = corefChain.getMentionsInTextualOrder().get(0).sentNum;
                    if (sentCount == sentenceCount) {
                        if (sentenceWords.size() - 1 >= 0) {
                            if (firstMention.contains(sentenceWords.get(sentenceWords.size() - 1)))
                                sentenceWords.add(token.word());
                            else
                                sentenceWords.add(firstMention);
                        } else
                            sentenceWords.add(token.word());
                    }
                    else
                        sentenceWords.add(firstMention);
                }
            }
            String corefSentence = String.join(" ", sentenceWords).trim().replaceAll(" 's", "'s");
            corefSentences.add(corefSentence + ".");
        }
        return corefSentences;
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
        System.out.println(corefSentences);
    }
}