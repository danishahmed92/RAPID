package wordembedding.dbo;

import config.Database;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.util.CoreMap;
import nlp.corenlp.annotator.WordAnnotator;
import nlp.corenlp.utils.CoreNLPAnnotatorUtils;
import nlp.wordnet.WordNet;
import org.deeplearning4j.models.word2vec.Word2Vec;
import utils.Utils;
import wordembedding.model.GenerateModel;
import wordembedding.vector.fasttext.FastTextModel;
import wordembedding.vector.glove.GloveModel;
import wordembedding.vector.word2vec.Word2VecModel;

import java.io.FileNotFoundException;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;

/**
 * @author DANISH AHMED on 12/17/2018
 *
 * This class gets gloss / sysnet of a property,
 * and pass the words in it as a map against that property
 * and generate embedding model for it
 */
public class PropertyEmbeddingModelGenerator {
    private HashMap<String, List<String>> propertyWordsForMeanMap = new LinkedHashMap<>();
    /*public PropertyEmbeddingModelGenerator() {
        getWordsForProperty();
    }*/

    /**
     *
     * @param propertyWordsForMeanMap property, words reflecting property
     */
    public void setPropertyWordsForMeanMap(HashMap<String, List<String>> propertyWordsForMeanMap) {
        this.propertyWordsForMeanMap = propertyWordsForMeanMap;
    }

    /**
     *
     * @param outputModelFile name of property embedding model
     * @param sourceWordEmbeddingModel embedding classifier to use
     */
    public void generateVectorModel(String outputModelFile, Word2Vec sourceWordEmbeddingModel) {
        GenerateModel generateModel = new GenerateModel(sourceWordEmbeddingModel, propertyWordsForMeanMap);
        generateModel.generateVectorModel(outputModelFile);
    }

    /**
     *
     * @return get labels and comments of properties from DB
     * @throws SQLException
     */
    private HashMap<String, HashMap<String, String>> getPropLabelCommentMapDB() throws SQLException {
        String DISTINCT_PROPERTIES = "SELECT DISTINCT `prop_uri`, `prop_label`, `prop_comment` FROM property ORDER BY `prop_uri` ASC";
        Statement statement = Database.databaseInstance.conn.createStatement();
        java.sql.ResultSet rs = statement.executeQuery(DISTINCT_PROPERTIES);

        HashMap<String, HashMap<String, String>> propLabelCommentMap = new LinkedHashMap<>();
        while (rs.next()) {
            HashMap<String, String> labelCommentMap = new HashMap<>();
            labelCommentMap.put("label", rs.getString("prop_label"));

            String comment = rs.getString("prop_comment");
            if (comment != null && comment.length() > 0) {
                labelCommentMap.put("comment", rs.getString("prop_comment"));
            }
            propLabelCommentMap.put(rs.getString("prop_uri"), labelCommentMap);
        }
        statement.close();
        return propLabelCommentMap;
    }

    /**
     *
     * @param labelCommentNoStopWords
     * @return gloss of words found in label / comments of properties
     */
    @Deprecated
    private List<String> getGlossIncludedWordsForMeanVector(String labelCommentNoStopWords) {
        List<String> wordsForMean = new ArrayList<>();

//        Adding initially because we need repeated words
        String[] wordSplit = labelCommentNoStopWords.split(" ");
        wordsForMean.addAll(Arrays.asList(wordSplit));


        WordNet wordNet = WordNet.wordNet;
        HashMap<String, String> wordGlossMap = wordNet.getGlossFromString(labelCommentNoStopWords,
                true,
                false);

//            this map will only have repeated words in gloss but not in keys
        for (String word : wordGlossMap.keySet()) {
            String glossFiltered = wordGlossMap.get(word);
            String[] glossSplit = glossFiltered.split(" ");

            wordsForMean.addAll(Arrays.asList(glossSplit));
        }

        return wordsForMean;
    }

    /**
     *
     * @param labelCommentNoStopWords labels and comments for property without stop words
     * @return list of synonyms
     */
    private List<String> getSynonymsForMeanVector(String labelCommentNoStopWords) {
        List<String> wordsForMean = new ArrayList<>();

//        Adding initially because we need repeated words
        String[] wordSplit = labelCommentNoStopWords.split(" ");
        wordsForMean.addAll(Arrays.asList(wordSplit));

        WordNet wordNet = WordNet.wordNet;
        for (String word : wordSplit) {
            wordsForMean.addAll(wordNet.getNTopSynonyms(word, 1000));
        }
        return wordsForMean;
    }

    /**
     *
     * @return returns gloss words for each property
     */
    public HashMap<String, List<String>> getPropertyGlossMapFromDB() {
        String query = "SELECT DISTINCT `prop_uri`, `wordnet_gloss` FROM property ORDER BY `prop_uri` ASC";
        HashMap<String, List<String>> propGlossMap = new LinkedHashMap<>();
        Statement statement = null;
        try {
            statement = Database.databaseInstance.conn.createStatement();
            java.sql.ResultSet rs = statement.executeQuery(query);

            while (rs.next()) {
                String gloss = rs.getString("wordnet_gloss");
                String property = rs.getString("prop_uri");

                String[] glossSplit = gloss.split(", ");
                List<String> glossWords = new ArrayList<>();

                if (glossSplit.length == 0) {
                    glossWords.add(gloss);
                } else {
                    glossWords.addAll(Arrays.asList(glossSplit));
                }
                propGlossMap.put(property, glossWords);
            }
            statement.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return propGlossMap;
    }

    /**
     *
     * @return returns synonyms for each property from DB
     */
    public HashMap<String, List<String>> getPropertySynsetMapFromDB() {
        String query = "SELECT DISTINCT `prop_uri`, `wordnet_synset` FROM property ORDER BY `prop_uri` ASC";
        HashMap<String, List<String>> propSynsetMap = new LinkedHashMap<>();
        Statement statement = null;
        try {
            statement = Database.databaseInstance.conn.createStatement();
            java.sql.ResultSet rs = statement.executeQuery(query);

            while (rs.next()) {
                String synset = rs.getString("wordnet_synset");
                String property = rs.getString("prop_uri");

                String[] synsetSplit = synset.split(", ");
                List<String> synsetWords = new ArrayList<>();

                if (synsetSplit.length == 0) {
                    synsetWords.add(synset);
                } else {
                    for (String word : synsetSplit) {
                        String lemma;
                        try {
                            Annotation newLemmaAnno = CoreNLPAnnotatorUtils.annotateDocument(WordAnnotator.WAInstance.getPipeline(), word);
                            List<CoreMap> sentences = newLemmaAnno.get(CoreAnnotations.SentencesAnnotation.class);
                            lemma = sentences.get(0).get(CoreAnnotations.TokensAnnotation.class).get(0).lemma();
                        } catch (NullPointerException npe) {
                            lemma = word;
                        }
                        lemma = WordNet.wordNet.getVerbForNoun(lemma);
                        synsetWords.add(lemma);
                    }
                }
                propSynsetMap.put(property, synsetWords);
            }
            statement.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return propSynsetMap;
    }

    /**
     * function to gather words against properties
     */
    public void getWordsForProperty() {
        try {
            HashMap<String, HashMap<String, String>> propLabelCommentMap = getPropLabelCommentMapDB();
            for (String propUri : propLabelCommentMap.keySet()) {
                HashMap<String, String> labelCommentMap = propLabelCommentMap.get(propUri);
                String label = labelCommentMap.get("label").trim();
                String comment = labelCommentMap.get("comment");

                if (comment != null)
                    label = label.join(" ", comment);

                label = label.trim();
                label = Utils.filterAlphaNum(label);

                Utils utils = new Utils();
                label = utils.removeStopWordsFromString(label);

                //get gloss for each word
//                propertyWordsForMeanMap.put(propUri, getGlossIncludedWordsForMeanVector(label));
                propertyWordsForMeanMap.put(propUri, getSynonymsForMeanVector(label));
            }
            System.out.println(propertyWordsForMeanMap);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        PropertyEmbeddingModelGenerator pgvg = new PropertyEmbeddingModelGenerator();
        HashMap<String, List<String>> propSynsetMap = pgvg.getPropertySynsetMapFromDB();
        pgvg.setPropertyWordsForMeanMap(propSynsetMap);

        try {
            /*Word2Vec embeddingModel = new Word2VecModel().word2Vec;
            pgvg.generateVectorModel("propSynsetLemmaEmbedding_w2v.vec", embeddingModel);*/

            /*Word2Vec embeddingModel = new GloveModel().glove;
            pgvg.generateVectorModel("propSynsetLemmaEmbedding_glove.vec", embeddingModel);*/

            Word2Vec embeddingModel = new FastTextModel().fastText;
            pgvg.generateVectorModel("propSynsetLemmaEmbedding_ft.vec", embeddingModel);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
//        pgvg.getWordsForProperty();
    }
}
