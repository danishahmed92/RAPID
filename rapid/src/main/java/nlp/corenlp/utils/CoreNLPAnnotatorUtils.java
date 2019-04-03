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
    public static Annotation createAndWriteAnnotationToFile(String context, StanfordCoreNLP pipeline, String outputFile) throws IOException {
        Annotation document = new Annotation(context);
        pipeline.annotate(document);

        OutputStream out = new FileOutputStream(outputFile);

        ProtobufAnnotationSerializer serializer = new ProtobufAnnotationSerializer();
        serializer.write(document,out);
        out.close();

        return document;
    }

    public static Annotation readAnnotationFromFile(String annotationFile) {
        ProtobufAnnotationSerializer serializer = new ProtobufAnnotationSerializer();
        try {
            return serializer.readUndelimited(new File(annotationFile));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static Annotation annotateDocument(StanfordCoreNLP pipeline, String context) {
        Annotation document = new Annotation(context);
        pipeline.annotate(document);
        return document;
    }
}
