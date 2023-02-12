package com.abetrack3.GenericQueryService.Controller.QueryServiceCore;

import org.bson.Document;

import java.util.List;

public class QueryTemplate {

    String source;
    String filter;
    String locale;
    Boolean countOnly;
    List<String> fields;
    Boolean isDynamicFilter;
    List<String> allowedSorts;
    List<String> dynamicFilter;

    QueryTemplate(Document template) {

        this.source = template.getString("Source");
        this.filter = template.getString("Filter");
        this.countOnly = template.getBoolean("CountOnly");
        this.locale = template.get("Locale", "en");
        this.isDynamicFilter = template.getBoolean("IsDynamicFilter", false);
        this.dynamicFilter = template.getList("DynamicFilter", String.class);
        this.allowedSorts = template.getList("AllowedSorts", String.class);
        this.fields = template.getList("Fields", String.class);

    }

}
