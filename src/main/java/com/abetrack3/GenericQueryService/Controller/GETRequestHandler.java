package com.abetrack3.GenericQueryService.Controller;

import com.abetrack3.GenericQueryService.Controller.Data.DatabaseNameProvider;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
public class GETRequestHandler {

    @GetMapping(value="/")
    public ResponseEntity<Object> onQueryRequestReceived(
        @RequestParam MultiValueMap<String, String> multiMap,
        @RequestHeader Map<String, String> requestHeaders
    ) {

        String serviceId = requestHeaders.get("x-service-id");

        if (serviceId == null) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body("x-service-id is mandatory");
        }

        String databaseName = DatabaseNameProvider.getDatabaseName(serviceId);

        if (databaseName == null) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body("provided x-service-id is not registered in the dictionary");
        }

        List<String> queryIds = multiMap.get("id");
        List<String> queryValues = multiMap.get("values");

        if (queryValues.size() != queryIds.size()) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body("Mismatch in number of query ids and queryValues");
        }


        return ResponseEntity
                .status(HttpStatus.OK)
                .body("Development in progress");
    }

}
