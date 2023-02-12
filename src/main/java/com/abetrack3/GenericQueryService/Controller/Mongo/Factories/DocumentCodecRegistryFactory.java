package com.abetrack3.GenericQueryService.Controller.Mongo.Factories;

import com.abetrack3.GenericQueryService.Controller.Mongo.Codecs.OverridableUuidRepresentationUuidCodecProvider;
import org.bson.UuidRepresentation;
import org.bson.codecs.BsonValueCodecProvider;
import org.bson.codecs.DocumentCodec;
import org.bson.codecs.DocumentCodecProvider;
import org.bson.codecs.ValueCodecProvider;
import org.bson.codecs.configuration.CodecRegistry;

import static java.util.Arrays.asList;
import static org.bson.codecs.configuration.CodecRegistries.fromProviders;

public class DocumentCodecRegistryFactory {

    public static DocumentCodec getDocumentCodec(UuidRepresentation uuidRepresentation) {
        return (DocumentCodec) new DocumentCodec(
                getDocumentCodecRegistry(uuidRepresentation)
        ).withUuidRepresentation(
                uuidRepresentation
        );
    }

    public static CodecRegistry getDocumentCodecRegistry(UuidRepresentation uuidRepresentation) {

        return fromProviders(asList(
                new OverridableUuidRepresentationUuidCodecProvider(uuidRepresentation),
                new ValueCodecProvider(),
                new BsonValueCodecProvider(),
                new DocumentCodecProvider()));

    }

}
