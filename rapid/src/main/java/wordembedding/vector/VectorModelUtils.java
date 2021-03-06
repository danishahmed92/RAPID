package wordembedding.vector;

import com.google.common.collect.Lists;
import org.deeplearning4j.models.embeddings.inmemory.InMemoryLookupTable;
import org.deeplearning4j.models.embeddings.loader.VectorsConfiguration;
import org.deeplearning4j.models.embeddings.reader.impl.BasicModelUtils;
import org.deeplearning4j.models.word2vec.Word2Vec;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.ops.transforms.Transforms;

import java.util.*;

/**
 * @author DANISH AHMED
 */
public class VectorModelUtils extends BasicModelUtils {
    /**
     *
     * @param vecModel Embedding Classifier Model (DL4j); initialized with (w2v/ft/glove)
     * @return vector configuration used to train embedding model
     */
    public static VectorsConfiguration getModelConfiguration(Word2Vec vecModel) {
        return vecModel.getConfiguration();
    }

    /**
     *
     * @param vecModel Embedding Classifier Model (DL4j); initialized with (w2v/ft/glove)
     * @param word word
     * @param n limit of nearest words
     * @return N nearest words based on similarity score
     */
    public static Collection<String> nNearestWords(Word2Vec vecModel, String word, int n) {
        return vecModel.wordsNearest(word, n);
    }

    /**
     *
     * @param vecModel Embedding Classifier Model (DL4j); initialized with (w2v/ft/glove)
     * @param vecArray word's vector row
     * @param n limit of nearest words
     * @return N nearest words based on similarity score
     */
    public static Collection<String> nNearestWords(Word2Vec vecModel, INDArray vecArray, int n) {
        return vecModel.wordsNearest(vecArray, n);
    }

    /**
     *
     * @param vecModel Embedding Classifier Model (DL4j); initialized with (w2v/ft/glove)
     * @param words list of words
     * @return means vector of words
     */
    public static INDArray getMeanVecFromWordList(Word2Vec vecModel, List<String> words) {
        return vecModel.getWordVectorsMean(words);
    }

    /**
     *
     * @param vec1 vector 1 (word's vector representation)
     * @param vec2 vector 2 (word's vector representation)
     * @return cosine similarity score between two vectors
     */
    public static double getSimilarity(INDArray vec1, INDArray vec2) {
        if (vec1 != null && vec2 != null) {
            if (vec1.equals(vec2)) {
                return 1.0D;
            }
            return Transforms.cosineSim(vec1, vec2);
        } else {
            return 0.0D / 0.0;
        }
    }

    /**
     *
     * @param vecModel Embedding Classifier Model (DL4j); initialized with (w2v/ft/glove)
     * @param word word
     * @param vec2 word's (different) vector representation
     * @return similarity score between string word and vector representation of a word
     */
    public static double getSimilarity(Word2Vec vecModel, String word, INDArray vec2) {
        INDArray vec1 = vecModel.getWordVectorMatrix(word);
        return getSimilarity(vec1, vec2);
    }

    /**
     *
     * @param vecModel Embedding Classifier Model (DL4j); initialized with (w2v/ft/glove)
     * @param word1 word 1
     * @param word2 word 2
     * @return similarity of two words
     */
    public static double getSimilarity(Word2Vec vecModel, String word1, String word2) {
        return vecModel.similarity(word1, word2);
    }

    /**
     *
     * @param vecModel Embedding Classifier Model (DL4j); initialized with (w2v/ft/glove)
     * @param words vector representation of word
     * @param top limit N nearest
     * @return nearest words and their similarity score w.r.t provided input word
     */
    public HashMap<String, Double> vectorNearest(Word2Vec vecModel, INDArray words, int top) {
        this.init(vecModel.getLookupTable());
        if (this.lookupTable instanceof InMemoryLookupTable) {
            InMemoryLookupTable l = (InMemoryLookupTable)this.lookupTable;
            INDArray syn0 = l.getSyn0();
            if (!this.normalized) {
                synchronized(this) {
                    if (!this.normalized) {
                        syn0.diviColumnVector(syn0.norm2(new int[]{1}));
                        this.normalized = true;
                    }
                }
            }

            INDArray similarity = Transforms.unitVec(words).mmul(syn0.transpose());
            List<Double> highToLowSimList = this.getTopN(similarity, top + 20);
            List<BasicModelUtils.WordSimilarity> result = new ArrayList();

            for(int i = 0; i < highToLowSimList.size(); ++i) {
                String word = this.vocabCache.wordAtIndex(((Double)highToLowSimList.get(i)).intValue());
                if (word != null && !word.equals("UNK") && !word.equals("STOP")) {
                    INDArray otherVec = this.lookupTable.vector(word);
                    double sim = Transforms.cosineSim(words, otherVec);
                    result.add(new BasicModelUtils.WordSimilarity(word, sim));
                }
            }

            Collections.sort(result, new BasicModelUtils.SimilarityComparator());
            return this.getNSimilarity(result, top);
        } else {
            List<BasicModelUtils.WordSimilarity> result = new ArrayList();
            Iterator var4 = this.vocabCache.words().iterator();

            while(var4.hasNext()) {
                String s = (String)var4.next();
                INDArray otherVec = this.lookupTable.vector(s);
                double sim = Transforms.cosineSim(words, otherVec);
                result.add(new BasicModelUtils.WordSimilarity(s, sim));
            }
            return this.getNSimilarity(result, top);
        }
    }

    /**
     *
     * @param results words and their similarity score
     * @param limit limit N nearest
     * @return only top N nearest words and eliminates the rest
     */
    protected HashMap<String, Double> getNSimilarity(List<BasicModelUtils.WordSimilarity> results, int limit) {
        HashMap<String, Double> wordSimilarityMap = new LinkedHashMap<>();
        for (int i = 0; i < results.size(); i++) {
            wordSimilarityMap.put(((BasicModelUtils.WordSimilarity)results.get(i)).getWord(),
                    ((BasicModelUtils.WordSimilarity)results.get(i)).getSimilarity());
            if (i >= (limit - 1))
                break;
        }
        return wordSimilarityMap;
    }

    /**
     *
     * @param vec vector representation of word
     * @param N Limit N nearest
     * @return socres list
     */
    protected List<Double> getTopN(INDArray vec, int N) {
        BasicModelUtils.ArrayComparator comparator = new BasicModelUtils.ArrayComparator();
        PriorityQueue<Double[]> queue = new PriorityQueue(vec.rows(), comparator);

        for(int j = 0; j < vec.length(); ++j) {
            Double[] pair = new Double[]{vec.getDouble(j), (double)j};
            if (queue.size() < N) {
                queue.add(pair);
            } else {
                Double[] head = (Double[])queue.peek();
                if (comparator.compare(pair, head) > 0) {
                    queue.poll();
                    queue.add(pair);
                }
            }
        }

        ArrayList lowToHighSimLst = new ArrayList();

        while(!queue.isEmpty()) {
            double ind = ((Double[])queue.poll())[1];
            lowToHighSimLst.add(ind);
        }

        return Lists.reverse(lowToHighSimLst);
    }
}
