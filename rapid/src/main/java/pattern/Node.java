package pattern;

import edu.stanford.nlp.ling.IndexedWord;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author DANISH AHMED on 2/26/2019
 */
public class Node {
    public IndexedWord indexedNode;
    public String pos;
    public String label;
    public String lemma;

    public boolean isMerged = false;
    public List<Integer> mergedIndexes = new ArrayList<>();
    public Set<String> mergedNodes = new HashSet<>();   // sourceLabel/POS typDep>targetLabel/POS

    public Node(IndexedWord indexedNode, String pos, String label) {
        this.indexedNode = indexedNode;
        this.pos = pos;
        this.label = label;
        this.lemma = label;
    }

    public Node(String label, String lemma) {
        this.label = label;
        this.lemma = lemma;
    }

    public void setLemma(String lemma) {
        this.lemma = lemma;
    }

    public void setLabel(String label) {
        this.label = label;
    }
}
