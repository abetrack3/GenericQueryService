package com.abetrack3.GenericQueryService.Controller;

import com.abetrack3.GenericQueryService.Controller.Data.DatabaseNameProvider;
import com.abetrack3.GenericQueryService.Controller.QueryServiceCore.Exceptions.InsufficientQueryValuesException;
import com.abetrack3.GenericQueryService.Controller.QueryServiceCore.QueryExecutioner;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@RestController
public class GETRequestHandler {

    @GetMapping(value="/")
    public ResponseEntity<String> onQueryRequestReceived(
        @RequestParam MultiValueMap<String, String> multiMap,
        @RequestHeader Map<String, String> requestHeaders
    ) {

        String serviceId = requestHeaders.get("x-service-id");

        if (serviceId == null) {
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body("x-service-id is mandatory");
        }

        String databaseName = DatabaseNameProvider.getDatabaseName(serviceId);

        if (databaseName == null) {
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body("provided x-service-id is not registered in the dictionary");
        }

        List<String> queryIds = multiMap.get("id");

        if (queryIds == null) {
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body("Missing query param: \"id\"");
        }

        List<String> queryValues = multiMap.get("values");

        if (queryValues == null) {
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body("Missing query param: \"values\"");
        }

        if (queryValues.size() != queryIds.size()) {
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body("Mismatch in number of query ids and queryValues");
        }

        StringBuilder queryResultStringBuilder = new StringBuilder("[");
        try {
            for (int index = 0; index < queryIds.size(); index++) {

                QueryExecutioner executioner =  new QueryExecutioner(
                        queryIds.get(index),
                        queryValues.get(index),
                        databaseName
                );

                String eachQueryResult = executioner.execute();

                if (index > 0) {
                    queryResultStringBuilder.append(',');
                }

                queryResultStringBuilder.append(eachQueryResult);

            }

            queryResultStringBuilder.append(']');

            return ResponseEntity
                    .status(HttpStatus.OK)
                    .body(queryResultStringBuilder.toString());
        }
        catch (InsufficientQueryValuesException exception) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Insufficient data in query param: \"values\"",
                    exception
            );
        }

    }

}
