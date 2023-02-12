package com.abetrack3.GenericQueryService.Controller.QueryServiceCore;

import com.abetrack3.GenericQueryService.Controller.Data.CommonCollections;
import com.abetrack3.GenericQueryService.Controller.Mongo.Factories.MongoClientFactory;
import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;

public class QueryExecutioner {

    private final String queryId;
    private final String queryValues;
    private final String databaseName;
    private String result;

    public QueryExecutioner(String queryId, String queryValues, String databaseName) {
        this.queryId = queryId;
        this.queryValues = queryValues;
        this.databaseName = databaseName;
    }

    public String execute() {
        this.fetchQueryTemplate();
        return result;
    }

    private void fetchQueryTemplate() {
        try (MongoClient mongoClient = MongoClientFactory.getClient()) {

            MongoDatabase database = mongoClient.getDatabase(databaseName);
            MongoCollection<Document> queryTemplatesCollection = database.getCollection(
                    CommonCollections
                            .QUERY_TEMPLATES
                            .name
            );

            Document search = new Document("_id", queryId);
            Document queryTemplate = queryTemplatesCollection.find(search).first();

            assert queryTemplate != null;
            result = queryTemplate.toJson();

        }
    }

}
