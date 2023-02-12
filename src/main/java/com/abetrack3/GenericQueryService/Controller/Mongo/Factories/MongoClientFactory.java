package com.abetrack3.GenericQueryService.Controller.Mongo.Factories;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientOptions;
import com.mongodb.MongoClientURI;
import com.mongodb.ServerAddress;
import org.bson.UuidRepresentation;

import static com.abetrack3.GenericQueryService.Controller.Mongo.Factories
        .DocumentCodecRegistryFactory.getDocumentCodecRegistry;

public class MongoClientFactory {

    private static final UuidRepresentation AGREED_REPRESENTATION;

    private static final String DB_HOSTNAME;
    private static final int DB_PORT;
    private static final String MONGO_DB_URL;

    static {
        AGREED_REPRESENTATION = UuidRepresentation.C_SHARP_LEGACY;
        DB_HOSTNAME = "localhost";
        DB_PORT = 27017;
        MONGO_DB_URL = String.format("mongodb://%s:%d", DB_HOSTNAME, DB_PORT);
    }

    public static MongoClient getClient() {

        MongoClientURI clientURI = new MongoClientURI(MONGO_DB_URL);

        MongoClientOptions clientOptions = MongoClientOptions.builder(clientURI.getOptions())
                .codecRegistry(getDocumentCodecRegistry(UuidRepresentation.PYTHON_LEGACY))
                .uuidRepresentation(UuidRepresentation.PYTHON_LEGACY)
                .build();

        ServerAddress serverAddress = new ServerAddress(DB_HOSTNAME, DB_PORT);

        return new MongoClient(serverAddress, clientOptions);

    }

}
