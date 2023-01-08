package com.abetrack3.GenericQueryService.Controller;

import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
public class GETRequestHandler {

    @GetMapping(value="/")
    public String handleQuery(
        @RequestParam Map<String, String> queryParamsMap,
        @RequestHeader Map<String, String> requestHeaders
    ) {
        return queryParamsMap.toString() +
                "\n" +
                "x-service-id = " + requestHeaders.get("x-service-id");
    }

}
