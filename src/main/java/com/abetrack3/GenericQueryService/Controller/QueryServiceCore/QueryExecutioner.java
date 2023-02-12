package com.abetrack3.GenericQueryService.Controller.QueryServiceCore;

import com.abetrack3.GenericQueryService.Controller.Data.CommonCollections;
import com.abetrack3.GenericQueryService.Controller.Mongo.Factories.MongoClientFactory;
import com.abetrack3.GenericQueryService.Controller.QueryServiceCore.Exceptions.InsufficientQueryValuesException;
import com.mongodb.Cursor;
import com.mongodb.MongoClient;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import org.bson.BsonArray;
import org.bson.Document;
import org.bson.UuidRepresentation;

import java.util.LinkedList;
import java.util.stream.Collectors;

import static com.abetrack3.GenericQueryService.Controller.Data.Constants.*;
import static com.abetrack3.GenericQueryService.Controller.Mongo.Factories.DocumentCodecRegistryFactory.getDocumentCodec;
import static com.abetrack3.GenericQueryService.Controller.Mongo.Factories.JsonWriterSettingsFactory.getJsonWriterSettings;

public class QueryExecutioner {

    private static final int MINIMUM_NUMBER_OF_QUERY_VALUES = 2;

    private final String queryId;
    private final LinkedList<String> queryValues;
    private final String databaseName;
    private String result;
    private final MongoClient mongoClient;

    private QueryTemplate template;

//    private String resultCount

    public QueryExecutioner(String queryId, String queryValuesAsString, String databaseName) throws InsufficientQueryValuesException {
        this.queryId = queryId;
        this.databaseName = databaseName;
        this.queryValues = BsonArray
                .parse(queryValuesAsString)
                .stream()
                .map(val -> val.asString().getValue())
                .collect(Collectors
                        .toCollection(LinkedList::new));

        if (queryValues.size() < MINIMUM_NUMBER_OF_QUERY_VALUES) {
            throw new InsufficientQueryValuesException();
        }
        this.mongoClient = MongoClientFactory.getClient();
    }

    public String execute() {
        this.fetchQueryTemplate();
        this.queryData();
        this.mongoClient.close();
        return this.result;
    }

    private void fetchQueryTemplate() {

        MongoDatabase database = this.mongoClient.getDatabase(this.databaseName);
        MongoCollection<Document> queryTemplatesCollection = database.getCollection(
                CommonCollections
                        .QUERY_TEMPLATES
                        .name
        );

        Document search = new Document("_id", this.queryId);
        Document queryTemplate = queryTemplatesCollection.find(search).first();

        assert queryTemplate != null;
        result = queryTemplate.toJson();
        this.template = new QueryTemplate((Document) queryTemplate.get("Template"));

    }

    private void queryData() {

        int pageIndex = Integer.parseInt(this.queryValues.removeLast());
        int pageSize = getClampedPageSize(Integer.parseInt(this.queryValues.removeLast()));

        Document filter = buildFilter();
        Document projection = buildProjection();

        MongoDatabase database = mongoClient.getDatabase(this.databaseName);
        MongoCollection<Document> collection = database.getCollection(this.template.source);

        if (this.template.countOnly) {
            this.result = "[" + collection.countDocuments(filter) + "]";
            return;
        }

        StringBuilder queryResultStringBuilder = new StringBuilder();
        try (MongoCursor<Document> cursor = collection
                .find(filter)
                .projection(projection)
                .skip(pageIndex * pageSize)
                .limit(pageSize)
                .cursor()) {

            for (long index = 0; cursor.hasNext(); index++) {

                Document document = cursor.next();

                String convertedJson = document.toJson(
                        getJsonWriterSettings(UuidRepresentation.PYTHON_LEGACY),
                        getDocumentCodec(AGREED_UUID_REPRESENTATION)
                );

                if (index > 0) {
                    queryResultStringBuilder.append(',');
                }

                queryResultStringBuilder.append(convertedJson);

            }

        }
        this.result = "[" + queryResultStringBuilder.toString() + "]";


    }

    private int getClampedPageSize(int queryParamPageSize) {
        int result = Math.max(MIN_PAGE_SIZE, queryParamPageSize);
        result = Math.min(MAX_PAGE_SIZE, result);
        return result;
    }

    private Document buildFilter() {

        String valuePopulatedFilter = this.template.filter;

        if (valuePopulatedFilter == null) {
            return Document.parse("{}");
        }

        int replacePoint = valuePopulatedFilter.indexOf('~');
        while (this.queryValues.size() > 0 && replacePoint != -1) {

            String replaceValue = queryValues.removeFirst();
            valuePopulatedFilter = valuePopulatedFilter.replace("~", replaceValue);

            replacePoint = valuePopulatedFilter.indexOf('~');
        }

        return Document.parse(
                valuePopulatedFilter,
                getDocumentCodec(AGREED_UUID_REPRESENTATION)
        );

    }

    private Document buildProjection() {

        Document projection = Document.parse("{_id: 1}");

        if (template.fields == null || template.countOnly) {
            return projection;
        }

        this.template.fields.forEach(fieldName -> projection.append(fieldName, 1));

        return projection;

    }

}
