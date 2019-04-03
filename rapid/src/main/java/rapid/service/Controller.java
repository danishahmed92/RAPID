package rapid.service;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import rapid.RelationExtractor;
import wordembedding.model.GeneratedModelClassification;

import java.util.*;

@RestController
@RequestMapping("/rapid/api/")
public class Controller {
    @PostMapping("re/")
    public HashMap<String, Object> extractRelations(@RequestParam(value = "context") String context,
                                        @RequestParam(value = "alpha") double alpha, @RequestParam(value = "beta") double beta,
                                        @RequestParam(value = "embeddingClassifier") String embeddingClassifier) {
        GeneratedModelClassification embeddingModel = null;

        switch (embeddingClassifier) {
            case "w2v":
                embeddingModel = GeneratedModelClassification.synsetW2VClassification;
                break;
            case "ft":
                embeddingModel = GeneratedModelClassification.synsetFTClassification;
                break;
            case "glove":
                embeddingModel = GeneratedModelClassification.synsetGloveClassification;
                break;
        }

        if (embeddingModel == null)
            embeddingModel = GeneratedModelClassification.synsetW2VClassification;

        RelationExtractor re = new RelationExtractor(alpha, beta, embeddingModel, context);
        HashMap<String, Set<HashMap<String, String>>> propertyPredicationDetailMap = re.extractRelationsFSS();
//        HashMap<String, Set<HashMap<String, String>>> propertyPredicationDetailMap = re.extractRelationsStringSimilarity();

        Set<String> entitiesRecognized = re.getEntitiesRecognized();
        Set<String> corefedSentences = re.getCorefSentences();
        List<HashMap<String, String>> relations = new ArrayList<>();

        for (String property : propertyPredicationDetailMap.keySet()) {
            for (HashMap<String, String> relationMap : propertyPredicationDetailMap.get(property)) {
                HashMap<String, String> triple = new HashMap<>();
                triple.put("subj", relationMap.get("subj"));
                triple.put("obj", relationMap.get("obj"));
                triple.put("property", property);

                relations.add(triple);
            }
        }

        System.out.println(relations);

        HashMap<String, Object> results = new HashMap<>();
        results.put("relations", relations);
        results.put("entities", entitiesRecognized);
        results.put("corefSentences", corefedSentences);

        return results;
    }
}
