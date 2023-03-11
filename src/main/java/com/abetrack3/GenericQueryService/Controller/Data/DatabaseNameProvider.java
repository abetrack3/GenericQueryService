package com.abetrack3.GenericQueryService.Controller.Data;

import com.abetrack3.GenericQueryService.Controller.AppRuntimeConfiguration;
import org.bson.Document;

public class DatabaseNameProvider {

    private static final String READ_DATABASE_NAME = "readDatabaseName";

    public static String getDatabaseName(String xServiceId) {

        Document registeredService = AppRuntimeConfiguration.getServiceInfo().get(xServiceId, Document.class);

        if (registeredService == null) {
            return null;
        }

        return registeredService.getString(READ_DATABASE_NAME);

    }

}
