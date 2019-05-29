package pattern;

import edu.stanford.nlp.ling.IndexedWord;
import edu.stanford.nlp.semgraph.SemanticGraph;
import edu.stanford.nlp.semgraph.SemanticGraphEdge;
import nlp.corenlp.parser.SemanticGraphParsing;
import nlp.corenlp.utils.DependencyTreeUtils;

import java.util.*;

/**
 * @author DANISH AHMED on 12/7/2018
 */
public class PatternSemanticGraphHelper {
    /**
     *
     * @param semanticGraph semantic graph
     * @param subj subject label
     * @param obj object label
     * @return replaces indexedWord's text with %D% and %R% if finds subj and obj label respectively
     */
    public static SemanticGraph replaceDomainRange(SemanticGraph semanticGraph, String subj, String obj) {
        SemanticGraph sg = new SemanticGraph();
        List<SemanticGraphEdge> allEdges = semanticGraph.edgeListSorted();
        if (allEdges ==  null || allEdges.isEmpty())
            return null;

        List<IndexedWord> subjIWList = DependencyTreeUtils.getIndexedWordsFromString(semanticGraph, subj);
        List<IndexedWord> objIWList = DependencyTreeUtils.getIndexedWordsFromString(semanticGraph, obj);
        for (SemanticGraphEdge edge : allEdges) {
            IndexedWord gov = edge.getGovernor();
            IndexedWord dep = edge.getDependent();

            if (!subjIWList.contains(gov) && !objIWList.contains(gov)
                    && !subjIWList.contains(dep) && !objIWList.contains(dep)) {
                sg.addEdge(edge);
            } else {
                if (subjIWList.contains(gov))
                    edge.getGovernor().setOriginalText("%D%");
                if (objIWList.contains(gov))
                    edge.getGovernor().setOriginalText("%R%");
                if (subjIWList.contains(dep))
                    edge.getDependent().setOriginalText("%D%");
                if (objIWList.contains(dep))
                    edge.getDependent().setOriginalText("%R%");

                sg.addEdge(edge);
            }
        }

        SemanticGraphParsing sgp = new SemanticGraphParsing();
        IndexedWord root = sgp.getRootWord(sg);
        sg.setRoot(root);

        return sg;
    }

    /**
     * removes edges and add new ones to link nodes and generalize SG
     * @param semanticGraph semantic graph
     * @param subj subject label
     * @param obj object label
     * @return generalized semantic subgraph
     */
    public static SemanticGraph pruneGraph(SemanticGraph semanticGraph, String subj, String obj) {
        List<IndexedWord> subjIWList = DependencyTreeUtils.getIndexedWordsFromString(semanticGraph, subj);
        List<IndexedWord> objIWList = DependencyTreeUtils.getIndexedWordsFromString(semanticGraph, obj);
        IndexedWord root = semanticGraph.getFirstRoot();

        SemanticGraphParsing sgp = new SemanticGraphParsing();
        HashMap<String, Set<SemanticGraphEdge>> subjRemoveAddMap = setEdgesRemovalAddition(semanticGraph, subjIWList);
        HashMap<String, Set<SemanticGraphEdge>> objRemoveAddMap = setEdgesRemovalAddition(semanticGraph, objIWList);

        for (SemanticGraphEdge edge : subjRemoveAddMap.get("add"))
            semanticGraph.addEdge(edge);

        for (SemanticGraphEdge edge : objRemoveAddMap.get("add"))
            semanticGraph.addEdge(edge);

        for (SemanticGraphEdge edge : subjRemoveAddMap.get("remove"))
            semanticGraph.removeEdge(edge);

        for (SemanticGraphEdge edge : objRemoveAddMap.get("remove"))
            semanticGraph.removeEdge(edge);

        Set<IndexedWord> rootLessNodes = sgp.getRootLessNodes(semanticGraph, root);
        for (IndexedWord node : rootLessNodes)
            semanticGraph.removeVertex(node);

        return semanticGraph;
    }

    /**
     *
     * @param semanticGraph semantic graph
     * @param iWList nodes reflecting entity (subject / object)
     * @return returns map of edges that has to be "add"ed or "remove"d
     */
    private static HashMap<String, Set<SemanticGraphEdge>> setEdgesRemovalAddition(SemanticGraph semanticGraph, List<IndexedWord> iWList) {
        HashMap<String, Set<SemanticGraphEdge>> edgeRemoveAddMap = new LinkedHashMap<>();

        Set<SemanticGraphEdge> toRemoveEdgeList = new HashSet<>();
        Set<SemanticGraphEdge> toAddEdgeList = new HashSet<>();

        IndexedWord root = semanticGraph.getFirstRoot();
        for (IndexedWord iW : iWList) {
            List<SemanticGraphEdge> rootToNodePath = semanticGraph.getShortestDirectedPathEdges(root, iW);
            if (rootToNodePath != null) {
                String prevRel = "";
                SemanticGraphEdge prevEdge = null;
                boolean isNmod = false;
                for (SemanticGraphEdge edge : rootToNodePath) {
                    String rel = edge.getRelation().getShortName();
                    String specific = edge.getRelation().getSpecific();
                    if (specific != null && specific.length() > 0)
                        rel = rel + ":" + specific;

                    if (rel.contains("nmod:of") && isNmod) {
                        IndexedWord gov = edge.getGovernor();
                        IndexedWord dep = edge.getDependent();
                        if (!iWList.contains(gov) && !iWList.contains(dep))
                            toRemoveEdgeList.add(prevEdge);
                        continue;
                    }
                    if (rel.contains("nmod:") && rel.equals(prevRel)) {
                        toRemoveEdgeList.add(prevEdge);

                        SemanticGraphEdge toAddEdge = new SemanticGraphEdge(
                                prevEdge.getGovernor(), edge.getDependent(),
                                prevEdge.getRelation(),
                                edge.getWeight(),
                                edge.isExtra()
                        );
                        toAddEdgeList.add(toAddEdge);
                    }

                    if (!rel.equals("nmod:of")) {
                        prevEdge = edge;
                        prevRel = rel;
                        isNmod = rel.contains("nmod:");
                    }
                }
            }
        }
        edgeRemoveAddMap.put("add", toAddEdgeList);
        edgeRemoveAddMap.put("remove", toRemoveEdgeList);

        return edgeRemoveAddMap;
    }

    /**
     *
     * @param sgPatterns set of semantic subgraphs generated using subj and obj nodes
     * @return all semantic subgraphs that do not contain a child node C if it is a root node of another subgraph
     */
    public static Set<SemanticGraph> removeRootContainSubGraph(Set<SemanticGraph> sgPatterns) {
        if (sgPatterns.size() <= 1)
            return sgPatterns;

        HashMap<IndexedWord, Set<SemanticGraph>> rootGraphsMap = new LinkedHashMap<>();
        for (SemanticGraph sg : sgPatterns) {
            IndexedWord root;
            try {
                root = sg.getFirstRoot();
            } catch (NullPointerException npe) {
                npe.printStackTrace();
                continue;
            }

            Set<SemanticGraph> graphs;
            if (!rootGraphsMap.containsKey(root)) {
                graphs = new HashSet<>();
                graphs.add(sg);
            } else {
                graphs = rootGraphsMap.get(root);
                graphs.add(sg);
            }
            rootGraphsMap.put(root, graphs);
        }

        Set<SemanticGraph> nonSubContainGraphs = new HashSet<>();
        for (IndexedWord root : rootGraphsMap.keySet()) {
            int maxEdges = -1;
            SemanticGraph maxGraph = null;
            for (SemanticGraph sg : rootGraphsMap.get(root)) {
                int edges = sg.edgeCount();
                if (edges >= maxEdges) {
                    maxEdges = edges;
                    maxGraph = sg;
                }
            }
            nonSubContainGraphs.add(maxGraph);
        }
        return nonSubContainGraphs;
    }
}
