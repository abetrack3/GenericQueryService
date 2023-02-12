package com.abetrack3.GenericQueryService.Controller.Mongo.Factories;

import org.bson.UuidRepresentation;
import org.bson.internal.Base64;
import org.bson.json.JsonMode;
import org.bson.json.JsonWriterSettings;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public class JsonWriterSettingsFactory {

    public static JsonWriterSettings getJsonWriterSettings(UuidRepresentation uuidRepresentation) {

        return JsonWriterSettings.builder()
                .outputMode(JsonMode.RELAXED)
                .objectIdConverter((value, writer) -> writer.writeString(value.toHexString()))
                .dateTimeConverter(
                        (value, writer) -> {
                            ZonedDateTime zonedDateTime = Instant.ofEpochMilli(value).atZone(ZoneOffset.UTC);
                            writer.writeString(DateTimeFormatter.ISO_DATE_TIME.format(zonedDateTime));
                        })
                .binaryConverter(
                        (value, writer) -> {
                            if (value.getType() == 3) {
                                writer.writeString(value.asUuid(uuidRepresentation).toString());
                            } else {
                                // Implementations copied from ExtendedJsonBinaryConverter
                                writer.writeStartObject();
                                writer.writeStartObject("$binary");
                                writer.writeString("base64", Base64.encode(value.getData()));
                                writer.writeString("subType", String.format("%02X", value.getType()));
                                writer.writeEndObject();
                                writer.writeEndObject();
                            }
                        })
                .build();

    }

}
