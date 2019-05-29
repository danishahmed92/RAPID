package datacollection.wikiclean;

import config.IniConfig;
import datacollection.elasticsearch.ElasticSearch;
import datacollection.elasticsearch.Indexer;
import org.elasticsearch.action.index.IndexResponse;
import org.wikiclean.WikiClean;
import org.wikiclean.WikipediaArticlesDump;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;

/**
 * @author DANISH AHMED on 9/2/2018
 */
public class WikiCleaner {
    private List<String> wikiXMLFilesBz2 = new ArrayList<>();

    /**
     * initialize wiki dump xml file crawler
     * @throws IOException
     */
    public WikiCleaner() throws IOException {
        this.wikiXMLFilesBz2 = filesCrawler();
    }

    /**
     * iterates across all xml files of wiki dump
     * @return list of wiki dump files
     * @throws IOException
     */
    private static List<String> filesCrawler() throws IOException {
        List<String> wikiXMLFilesBz2 = new ArrayList<>();
        Path path = Paths.get(IniConfig.configInstance.wikiFolder + "xml/");
        if (Files.isDirectory(path)) {
            Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    wikiXMLFilesBz2.add(String.valueOf(file.getFileName()));
                    return FileVisitResult.CONTINUE;
                }
            });
        }
        return wikiXMLFilesBz2;
    }

    /**
     * Cleans wiki data and simultaneously creates ES index using multithreading
     * @param args
     * @throws IOException
     */
    public static void main(String[] args) throws IOException {
        Indexer elasticSearchIndexer = new Indexer(ElasticSearch.elasticSearchInstance);
        IniConfig config = IniConfig.configInstance;
        String index = config.esDataset;
        String indexType = config.esDSType;

        if (elasticSearchIndexer.createIndex()) {
            WikiCleaner wikiCleaner = new WikiCleaner();
            ExecutorService executor = Executors.newSingleThreadExecutor();

            for (String file : wikiCleaner.wikiXMLFilesBz2) {
                executor.execute(new Runnable() {
                    public void run() {
                        String wikiPath = IniConfig.configInstance.wikiFolder;
                        File input = new File(wikiPath + "xml/" + file);
                        final WikiClean cleaner = new WikiClean.Builder().withLanguage(WikiClean.WikiLanguage.EN)
                                .withTitle(false).withFooter(false).build();

                        WikipediaArticlesDump wikipedia = null;
                        try {
                            wikipedia = new WikipediaArticlesDump(input);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                        assert wikipedia != null;
                        wikipedia.stream()
                                .filter(page -> !page.contains("<ns>") || page.contains("<ns>0</ns>"))
                                .forEach(page -> {
                                    String title = cleaner.getTitle(page).replaceAll("\\n+", " ");
                                    String url = title.trim().replaceAll(" ", "_");
                                    String text = cleaner.clean(page).replaceAll("\\n+", " ");

                                    try {
                                        IndexResponse response = elasticSearchIndexer.getClient().prepareIndex(
                                                index,
                                                indexType,
                                                url)
                                                .setSource(jsonBuilder()
                                                        .startObject()
                                                        .field("title", title)
                                                        .field("text", text)
                                                        .endObject()
                                                )
                                                .get();
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                });
                    }
                });
            }
            executor.shutdown();
        }
    }
}