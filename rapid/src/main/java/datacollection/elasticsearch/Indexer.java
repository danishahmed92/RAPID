package datacollection.elasticsearch;

import config.IniConfig;
import org.elasticsearch.action.admin.indices.create.CreateIndexResponse;
import org.elasticsearch.client.transport.TransportClient;

import java.util.HashMap;
import java.util.Map;

/**
 * @author DANISH AHMED on 9/4/2018
 */
public class Indexer {
    private ElasticSearch elasticSearchInstance;

    public Indexer(ElasticSearch elasticSearch) {
        this.elasticSearchInstance = elasticSearch;
    }

    public TransportClient getClient() {
        return elasticSearchInstance.client;
    }

    /**
     * creates ES index / Dataset is created against which indexing will be done
     * @return returns true on success
     */
    public Boolean createIndex() {
        Boolean clusterHealthy = false;
        if (clusterHealthy) {
            IniConfig config = IniConfig.configInstance;
            String indexDataset = config.esDataset;
            String indexDataType = config.esDSType;

//            if (!indexExist(indexName)) {
            Map<String, Object> jsonMap = new HashMap<>();

            Map<String, Object> text = new HashMap<>();
            Map<String, Object> title = new HashMap<>();
            text.put("type", "text");
            title.put("type", "text");

            Map<String, Object> properties = new HashMap<>();
            properties.put("title", title);
            properties.put("text", text);

            Map<String, Object> datasetTypeUri = new HashMap<>();
            datasetTypeUri.put("properties", properties);
            jsonMap.put(indexDataType, datasetTypeUri);

            CreateIndexResponse createIndexResponse =
                    elasticSearchInstance.client.admin().indices().prepareCreate(indexDataset)
                            .addMapping(indexDataType, jsonMap)
                            .get();

            return createIndexResponse.isAcknowledged();
            //            }
//            return true;
        }
        return false;
    }

    public static void main(String[] args) {
        Indexer elasticSearch = new Indexer(ElasticSearch.elasticSearchInstance);
        if (elasticSearch.createIndex()) {
            System.out.println("indexCreated");
        } else {
            System.out.println("failed");
        }
    }
}
