package com.abetrack3.GenericQueryService.Controller.QueryServiceCore;

import com.abetrack3.GenericQueryService.Controller.Data.CommonCollections;
import com.abetrack3.GenericQueryService.Controller.Mongo.Factories.MongoClientFactory;
import com.abetrack3.GenericQueryService.Controller.QueryServiceCore.Exceptions.*;
import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Aggregates;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Sorts;
import org.bson.BsonArray;
import org.bson.BsonInvalidOperationException;
import org.bson.Document;
import org.bson.UuidRepresentation;
import org.bson.conversions.Bson;

import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import static com.abetrack3.GenericQueryService.Controller.Data.Constants.*;
import static com.abetrack3.GenericQueryService.Controller.Mongo.Factories.DocumentCodecRegistryFactory.getDocumentCodec;
import static com.abetrack3.GenericQueryService.Controller.Mongo.Factories.JsonWriterSettingsFactory.getJsonWriterSettings;
import static com.mongodb.client.model.Sorts.ascending;
import static com.mongodb.client.model.Sorts.descending;

public class QueryExecutioner {

    private static final int MINIMUM_NUMBER_OF_QUERY_VALUES = 2;
    private static final String TOKEN_JSON_PAYLOAD_ROLE_KEY = "role";
    private static final String TOKEN_JSON_PAYLOAD_USER_ID_KEY = "sub";
    private static final String ANONYMOUS_ROLE = "anonymous";

    private final String queryId;
    private final LinkedList<String> queryValues;
    private final String databaseName;
    private final MongoClient mongoClient;
    private final String dynamicIndicesAsString;
    private String result;
    private QueryTemplate template;
    private List<String> rolesAndIds;

    public QueryExecutioner(
            String queryId,
            String queryValuesAsString,
            String dynamicIndicesAsString,
            String databaseName,
            String jwtTokenPayloadDecoded
    ) throws InsufficientQueryValuesException {

        this.queryId = queryId;
        this.databaseName = databaseName;
        this.dynamicIndicesAsString = dynamicIndicesAsString;
        this.queryValues = BsonArray
                .parse(queryValuesAsString)
                .stream()
                .map(val -> val.asString().getValue())
                .collect(Collectors
                        .toCollection(LinkedList::new));

        if (queryValues.size() < MINIMUM_NUMBER_OF_QUERY_VALUES) {
            throw new InsufficientQueryValuesException();
        }

        Document jwtTokenPayloadAsDocument = Document.parse(jwtTokenPayloadDecoded);
        this.rolesAndIds = jwtTokenPayloadAsDocument.getList(TOKEN_JSON_PAYLOAD_ROLE_KEY, String.class);

        if (this.rolesAndIds == null) {
            this.rolesAndIds = List.of(ANONYMOUS_ROLE);
        }

        this.rolesAndIds.add(jwtTokenPayloadAsDocument.getString(TOKEN_JSON_PAYLOAD_USER_ID_KEY));

        this.mongoClient = MongoClientFactory.getClient();
    }

    public String execute() throws InvalidSortFieldException, DynamicIndicesNotFoundException, DynamicIndicesParseFailureException, DynamicFilterAndIndicesLengthMismatchException {
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

    private void queryData() throws InvalidSortFieldException, DynamicIndicesNotFoundException, DynamicIndicesParseFailureException, DynamicFilterAndIndicesLengthMismatchException {

        int pageIndex = Integer.parseInt(this.queryValues.removeLast());
        int pageSize = getClampedPageSize(Integer.parseInt(this.queryValues.removeLast()));

        Document filter = template.isDynamicFilter? buildDynamicFilter() : buildFilter();
        Bson rowLevelSecurityFilter = Filters.in("RolesAndIdsAllowedToRead", rolesAndIds);
        Bson isMarkedToDeleteFilter = Filters.eq("IsMarkedToDelete", false);
        Bson securityFilter = Filters.and(
                rowLevelSecurityFilter,
                isMarkedToDeleteFilter
        );
        Bson finalFilter = Filters.and(securityFilter, filter);
        Bson sorter = buildSorter();
        Document projection = buildProjection();

        MongoDatabase database = mongoClient.getDatabase(this.databaseName);
        MongoCollection<Document> collection = database.getCollection(this.template.source);

        if (this.template.countOnly) {
            this.result = "[" + collection.countDocuments(finalFilter) + "]";
            return;
        }

        StringBuilder queryResultStringBuilder = new StringBuilder();
        List<Bson> aggregatesList = List.of(
                Aggregates.match(finalFilter),
                Aggregates.project(projection),
                Aggregates.sort(sorter),
                Aggregates.skip(pageIndex * pageSize),
                Aggregates.limit(pageSize)
        );
        try (MongoCursor<Document> cursor = collection
                .aggregate(aggregatesList)
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
        this.result = "[" + queryResultStringBuilder + "]";

    }

    private Bson buildSorter() throws InvalidSortFieldException {

        Bson ascendingSortById = Sorts.ascending("_id");

        if (this.queryValues.size() == 0) {
            return ascendingSortById;
        }

        if (this.template.allowedSorts == null || this.template.allowedSorts.size() == 0) {
            return ascendingSortById;
        }

        String fieldName = this.queryValues.getFirst();

        if (fieldName.equals("")) {
            return ascendingSortById;
        }

        char sortCharacter = fieldName.charAt(0);
        fieldName = fieldName.substring(1);

        if (!(sortCharacter == '+') && !(sortCharacter == ' ') && !(sortCharacter == '-')) {
            throw new InvalidSortFieldException(
                    "Provided field: \""
                            + fieldName
                            + "\" does not specify the order to be sorted in"
            );
        }

        if (!this.template.allowedSorts.contains(fieldName)) {
            throw new InvalidSortFieldException(
                    "Provided field: \""
                            + fieldName
                            + "\" is not included in Template's AllowedSorts"
            );
        }

        boolean ascending = sortCharacter == '+' || sortCharacter == ' ';
        Bson sortByField = ascending ? ascending(fieldName) : descending(fieldName);
        return Sorts.orderBy(sortByField, ascendingSortById);

    }

    private int getClampedPageSize(int queryParamPageSize) {
        int result = Math.max(MIN_PAGE_SIZE, queryParamPageSize);
        result = Math.min(MAX_PAGE_SIZE, result);
        return result;
    }

    private Document buildDynamicFilter() throws DynamicIndicesNotFoundException, DynamicIndicesParseFailureException, DynamicFilterAndIndicesLengthMismatchException {

        if (this.dynamicIndicesAsString == null) {
            throw new DynamicIndicesNotFoundException();
        }

        try {

            LinkedList<Integer> dynamicIndices = BsonArray
                    .parse(this.dynamicIndicesAsString)
                    .stream()
                    .map(value -> value.asInt32().getValue())
                    .collect(Collectors.toCollection(LinkedList::new));

            if (dynamicIndices.size() != this.template.dynamicFilter.size()) {
                throw new DynamicFilterAndIndicesLengthMismatchException();
            }

            StringBuilder blankFilterStringBuilder = new StringBuilder();
            for (int index = 0; index < dynamicIndices.size(); index++) {

                int enable = dynamicIndices.get(index);

                if (enable == 1) {
                    blankFilterStringBuilder.append(this.template.dynamicFilter.get(index));
                }

            }

            return buildFilter(blankFilterStringBuilder.toString());

        } catch (BsonInvalidOperationException e) {
            throw new DynamicIndicesParseFailureException();
        }

    }

    private Document buildFilter() {
        return this.buildFilter(this.template.filter);
    }

    private Document buildFilter(String blankFilter) {

        String valuePopulatedFilter = blankFilter;

        if (valuePopulatedFilter == null) {
            return Document.parse("{}");
        }

        int replacePoint = valuePopulatedFilter.indexOf('~');
        while (this.queryValues.size() > 0 && replacePoint != -1) {

            String replaceValue = queryValues.removeFirst();
            valuePopulatedFilter = valuePopulatedFilter.replaceFirst("~", replaceValue);

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
