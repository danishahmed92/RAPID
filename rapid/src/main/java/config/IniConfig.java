package config;

import org.ini4j.Ini;

import java.io.IOException;

/**
 * @author DANISH AHMED
 */
public class IniConfig {
    public static IniConfig configInstance;
    static {
        try {
            configInstance = new IniConfig();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String wikiFolder;

    public String esIp;
    public int esPort;
    public String esDataset;
    public String esDSType;

    public String sparql;
    public int numRelationsPerProperty;

    public String stopWords;
    public String wordNet;
    public String resultPath;

    public String word2vec;
    public String glove;
    public String fastText;

    public String propertyGlossW2V;
    public String propertyGlossGlove;
    public String propertyGlossFT;

    public String propertySynsetW2V;
    public String propertySynsetGlove;
    public String propertySynsetFT;

    public String firstPassAnnotationDir;
    public String secondPassAnnotationDir;

    /**
     * reading configuration from systemConfig.ini
     * and set variables that are globally required
     * @throws IOException
     */
    private IniConfig() throws IOException {
        String CONFIG_FILE = "systemConfig.ini";
        Ini configIni = new Ini(IniConfig.class.getClassLoader().getResource(CONFIG_FILE));

        wikiFolder = configIni.get("data", "wikiFolder");

        esIp = configIni.get("elasticSearch", "ip");
        esPort = Integer.parseInt(configIni.get("elasticSearch", "port"));
        esDataset = configIni.get("elasticSearch", "indexDataset");
        esDSType = configIni.get("elasticSearch", "indexType");

        sparql = configIni.get("environment", "sparql");
        numRelationsPerProperty = Integer.parseInt(configIni.get("environment", "numRelationsPerProperty"));

        stopWords = configIni.get("data", "stopWords");
        wordNet = configIni.get("data", "wordNet");
        resultPath = configIni.get("data", "resultPath");

        word2vec = configIni.get("sourceModel", "word2vec");
        glove = configIni.get("sourceModel", "glove");
        fastText = configIni.get("sourceModel", "fastText");

        secondPassAnnotationDir = configIni.get("data", "secondPassAnnotationDir");

        propertyGlossW2V = configIni.get("generatedModel", "propertyGlossW2V");
        propertyGlossGlove = configIni.get("generatedModel", "propertyGlossGlove");
        propertyGlossFT = configIni.get("generatedModel", "propertyGlossFT");

        propertySynsetW2V = configIni.get("generatedModel", "propertySynsetW2V");
        propertySynsetGlove = configIni.get("generatedModel", "propertySynsetGlove");
        propertySynsetFT = configIni.get("generatedModel", "propertySynsetFT");
    }
}
