package nlp.wordnet;

import config.IniConfig;
import edu.mit.jwi.Dictionary;
import edu.mit.jwi.IDictionary;
import edu.mit.jwi.item.*;
import utils.Utils;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author DANISH AHMED on 12/17/2018
 */
public class WordNet {
    public static WordNet wordNet;
    private IDictionary dict;
    static {
        try {
            wordNet = new WordNet();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Initializing wordnet provided location where it is saved
     * @throws IOException
     */
    public WordNet() throws IOException {
        String path = IniConfig.configInstance.wordNet;
        URL url = null;
        try {
            url = new URL("file", null, path);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        if (url == null)
            return;

        dict = new Dictionary(url);
        dict.open();
    }

    /**
     *
     * @param noun noun word
     * @return verbs against a noun if exist
     */
    public String getVerbForNoun(String noun) {
        IIndexWord idxWord = dict.getIndexWord(noun, POS.NOUN);
        try {
            IWordID wordID = idxWord.getWordIDs().get(0);
            IWord word = dict.getWord(wordID);
            String nounLemma = word.getLemma();

            for (IWordID iWordID : word.getRelatedWords()) {
                IWord relWord = dict.getWord(iWordID);
                if (relWord.getPOS() == POS.VERB) {
                    String verb = relWord.toString().split("-")[4];

                    IIndexWord iinWord = dict.getIndexWord(verb, POS.VERB);
                    IWordID iwordID = iinWord.getWordIDs().get(0);
                    IWord iword = dict.getWord(iwordID);

                    return iword.getLemma();
                }
            }
            return nounLemma;
        } catch (NullPointerException ne) {
            return noun;
        }
    }

    /**
     *
     * @param word word
     * @param n number of synonyms to be returned
     * @return n synonyms for a word
     */
    public Set<String> getNTopSynonyms(String word, int n) {
        Set<String> synonyms = new HashSet<>();
        POS pos = getBestPOS(word);
        if (pos == null)
            return synonyms;

        IIndexWord indexWord = dict.getIndexWord(word, pos);
        if (indexWord != null) {
            List<IWordID> wordIDs = indexWord.getWordIDs();
            for (IWordID wordID : wordIDs) {
                IWord iWord = dict.getWord(wordID);
                ISynset iSynset = iWord.getSynset();

                for (IWord synsetWord : iSynset.getWords()) {
                    if (synonyms.size() < n) {
                        synonyms.add(synsetWord.getLemma());
                        n++;
                    }
                }
            }
        }
        return synonyms;
    }

    /**
     * used whenever CoreNLP determines wrong POS
     * @param word
     * @return returns pos based on gloss frequency
     */
    public POS getBestPOS(String word) {
        try {
            IIndexWord iwN = dict.getIndexWord(word, POS.NOUN);
            int iwNSenseCount = (iwN == null || iwN.getWordIDs() == null || iwN.getWordIDs().size() == 0) ?
                    0 : iwN.getTagSenseCount();

            IIndexWord iwV = dict.getIndexWord(word, POS.VERB);
            int iwVSenseCount = (iwV == null || iwV.getWordIDs() == null || iwV.getWordIDs().size() == 0) ?
                    0 : iwV.getTagSenseCount();

            IIndexWord iwAdv = dict.getIndexWord(word, POS.ADVERB);
            int iwAdvSenseCount = (iwAdv == null || iwAdv.getWordIDs() == null || iwAdv.getWordIDs().size() == 0) ?
                    0 : iwAdv.getTagSenseCount();

            IIndexWord iwAdj = dict.getIndexWord(word, POS.ADJECTIVE);
            int iwAdjSenseCount = (iwAdj == null || iwAdj.getWordIDs() == null || iwAdj.getWordIDs().size() == 0) ?
                    0 : iwAdj.getTagSenseCount();

            int maxOccur = -1;
            POS pos = null;
            if (iwNSenseCount > maxOccur) {
                if (iwN != null)
                    pos = iwN.getPOS();
                maxOccur = iwNSenseCount;
            }
            if (iwVSenseCount > maxOccur) {
                if (iwV != null)
                    pos = iwV.getPOS();
                maxOccur = iwVSenseCount;
            }
            if (iwAdvSenseCount > maxOccur) {
                if (iwAdv != null)
                    pos = iwAdv.getPOS();
                maxOccur = iwAdvSenseCount;
            }
            if (iwAdjSenseCount > maxOccur) {
                if (iwAdj != null)
                    pos = iwAdj.getPOS();
                maxOccur = iwAdjSenseCount;
            }
            return pos;
        } catch (IllegalArgumentException iae) {
            System.out.println(word);
            return null;
        }
    }

    /**
     *
     * @param word word
     * @param pos part of speech
     * @return IWord - can be used to access synset
     */
    private IWord getIWord(String word, POS pos) {
        IIndexWord idxWord = dict.getIndexWord(word, pos);
        if (idxWord.getWordIDs() == null || idxWord.getWordIDs().size() == 0)
            return null;

        IWordID wordID = idxWord.getWordIDs().get(0);
        return dict.getWord(wordID);
    }

    /**
     *
     * @param word word
     * @param pos part of speech
     * @return lemma of word
     */
    public String getLemma(String word, POS pos) {
        IWord iword = getIWord(word, pos);
        if (iword == null)
            return word;

        return iword.getLemma().length() > 0 ? iword.getLemma() : word;
    }

    /**
     *
     * @param word word
     * @return lemma of word by selecting POS itself based on gloss frequency
     */
    public String getLemma(String word){
        POS pos = getBestPOS(word);
        if (pos == null)
            return word;
        return getLemma(word, pos);
    }

    /**
     *
     * @param word word
     * @param pos part of speech
     * @return definition of the word
     */
    public String getGloss(String word, POS pos) {
        IWord iword = getIWord(word, pos);
        if (iword == null)
            return null;

        return iword.getSynset().getGloss();
    }

    /**
     *
     * @param word word
     * @return definition of the word - selects POS itself based on gloss frequency
     */
    public String getGloss(String word) {
        POS pos = getBestPOS(word);
        if (pos == null)
            return null;

        return getGloss(word, pos);
    }

    /**
     *
     * @param wordsSpaceSeparated words string / sentence
     * @param removeStopWordsFromGloss removes stop words from the definition (if true)
     * @param exampleSentence flag to include example sentences from definition or not
     * @return map of words and it's definitions
     */
    public HashMap<String, String> getGlossFromString(String wordsSpaceSeparated,
                                                      boolean removeStopWordsFromGloss,
                                                      boolean exampleSentence) {
        String[] words = wordsSpaceSeparated.split(" ");
        HashMap<String, String> wordGlossMap = new HashMap<>();
        for (String word : words) {
            if (!wordGlossMap.containsKey(word)) {
                if (!removeStopWordsFromGloss)
                    wordGlossMap.put(word, getGloss(word));
                else {
                    Utils utils = new Utils();

                    String gloss = getGloss(word);
                    if (gloss != null) {
                        if (!exampleSentence) {
                            if (gloss.contains(";")) {
                                String[] glossSplit = gloss.split(";");
                                gloss = glossSplit[0];
                            }
                        }

                        gloss = Utils.filterAlphaNum(gloss);
                        gloss = utils.removeStopWordsFromString(gloss);
                        if (gloss.length() > 0)
                            wordGlossMap.put(word, gloss.trim());
                    }
                }
            }
        }
        return wordGlossMap;
    }

    public static void main(String[] args) {
        WordNet wordNet = WordNet.wordNet;
        String word = "debut";
        POS pos = wordNet.getBestPOS(word);

        System.out.println(word.toUpperCase());
        System.out.println("POS:\t" + wordNet.getBestPOS(word));
        System.out.println("Lemma:\t" + wordNet.getLemma(word, pos));
        System.out.println("Gloss:\t" + wordNet.getGloss(word, pos));
        System.out.println("Synonym:\t" + wordNet.getNTopSynonyms(word, 100));
    }
}
