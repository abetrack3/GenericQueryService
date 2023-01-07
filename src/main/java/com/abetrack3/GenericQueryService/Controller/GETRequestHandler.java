package com.abetrack3.GenericQueryService;

import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
public class GETRequestHandler {

    @GetMapping(value="/")
    public String handleQuery(@RequestParam Map<String, String> param) {
        System.out.println(param);
        System.out.println("param.size() = " + param.size());
        return "Request Received";
    }

}
