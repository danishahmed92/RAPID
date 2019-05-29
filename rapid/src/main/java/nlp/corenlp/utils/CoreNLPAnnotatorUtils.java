package nlp.corenlp.utils;

import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.ProtobufAnnotationSerializer;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * @author DANISH AHMED
 */
public class CoreNLPAnnotatorUtils {
    /**
     *
     * @param context sentence(s)
     * @param pipeline CoreNLP pipeline
     * @param outputFile file path to save annotation
     * @return annotation; also saves the serialized annotation to specified file
     * @throws IOException
     */
    public static Annotation createAndWriteAnnotationToFile(String context, StanfordCoreNLP pipeline, String outputFile) throws IOException {
        Annotation document = new Annotation(context);
        pipeline.annotate(document);

        OutputStream out = new FileOutputStream(outputFile);

        ProtobufAnnotationSerializer serializer = new ProtobufAnnotationSerializer();
        serializer.write(document,out);
        out.close();

        return document;
    }

    /**
     *
     * @param annotationFile serialized annotation file
     * @return coreNLP annotation
     */
    public static Annotation readAnnotationFromFile(String annotationFile) {
        ProtobufAnnotationSerializer serializer = new ProtobufAnnotationSerializer();
        try {
            return serializer.readUndelimited(new File(annotationFile));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     *
     * @param pipeline CoreNLP pipeline
     * @param context sentence(s)
     * @return annotation of context based on passed pipeline
     */
    public static Annotation annotateDocument(StanfordCoreNLP pipeline, String context) {
        Annotation document = new Annotation(context);
        pipeline.annotate(document);
        return document;
    }
}
