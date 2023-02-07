package com.abetrack3.GenericQueryService.Controller;

import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
public class GETRequestHandler {

    @GetMapping(value="/")
    public String onQueryRequestReceived(
        @RequestParam MultiValueMap<String, String> multiMap,
        @RequestHeader Map<String, String> requestHeaders
    ) {

        System.out.println("multiMap = " + multiMap);
        System.out.println("requestHeaders = " + requestHeaders);


        return "multiMap = " + multiMap.toString()
                + "\n" +
                "x-service-id = " + requestHeaders.get("x-service-id");
    }

}
