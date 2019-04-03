package rapid.service;

import nlp.corenlp.annotator.CoreferenceAnnotator;
import nlp.corenlp.annotator.ParseAnnotator;
import nlp.corenlp.annotator.WordAnnotator;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;
import wordembedding.model.GeneratedModelClassification;

/**
 * @author DANISH AHMED on 2/27/2019
 */
@Component
public class ApplicationStartup implements ApplicationListener<ApplicationReadyEvent> {
    @Override
    public void onApplicationEvent(ApplicationReadyEvent applicationReadyEvent) {

        // initialize source and generated embedding models
        GeneratedModelClassification synsetW2VClassification = GeneratedModelClassification.synsetW2VClassification;
        GeneratedModelClassification synsetGloveVClassification = GeneratedModelClassification.synsetGloveClassification;
        GeneratedModelClassification synsetFTClassification = GeneratedModelClassification.synsetFTClassification;

        // initialize Stanford annotation pipelines
        CoreferenceAnnotator corefAnnotator = CoreferenceAnnotator.CRInstance;
        ParseAnnotator parseAnnotator = ParseAnnotator.PAInstance;
        WordAnnotator wordAnnotator = WordAnnotator.WAInstance;

        System.out.println("All components loaded successfully.\n");
    }
}