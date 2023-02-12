package com.abetrack3.GenericQueryService.Controller.Data;

import java.util.Map;

import static java.util.Map.entry;

public class DatabaseNameProvider {

    private static final Map<String, String> xServiceIdToDbNameMap;

    static {
        xServiceIdToDbNameMap = Map.ofEntries(
                entry("product", "ProductReadDatabase"),
                entry("uam", "UserAccessManagementReadDatabase")
        );
    }

    public static String getDatabaseName(String xServiceId) {
        return xServiceIdToDbNameMap.get(xServiceId);
    }

}
