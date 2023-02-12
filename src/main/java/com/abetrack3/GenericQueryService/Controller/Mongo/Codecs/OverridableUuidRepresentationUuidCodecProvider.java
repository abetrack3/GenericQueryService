package com.abetrack3.GenericQueryService.Controller.Mongo.Codecs;

import org.bson.UuidRepresentation;
import org.bson.codecs.Codec;
import org.bson.codecs.OverridableUuidRepresentationUuidCodec;
import org.bson.codecs.configuration.CodecProvider;
import org.bson.codecs.configuration.CodecRegistry;

import java.util.UUID;

public class OverridableUuidRepresentationUuidCodecProvider implements CodecProvider {

    private final UuidRepresentation uuidRepresentation;

    public OverridableUuidRepresentationUuidCodecProvider(UuidRepresentation uuidRepresentation) {

        this.uuidRepresentation = uuidRepresentation;

    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> Codec<T> get(Class<T> clazz, CodecRegistry registry) {

        if (UUID.class == clazz) {

            return (Codec<T>) new OverridableUuidRepresentationUuidCodec(this.uuidRepresentation);

        }

        return null;

    }
}
