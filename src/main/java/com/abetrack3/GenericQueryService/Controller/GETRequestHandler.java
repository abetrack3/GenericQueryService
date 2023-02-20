package com.abetrack3.GenericQueryService.Controller;

import com.abetrack3.GenericQueryService.Controller.Data.DatabaseNameProvider;
import com.abetrack3.GenericQueryService.Controller.QueryServiceCore.Exceptions.*;
import com.abetrack3.GenericQueryService.Controller.QueryServiceCore.QueryExecutioner;
import org.apache.tomcat.util.codec.binary.Base64;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
public class GETRequestHandler {

    @GetMapping(value="/")
    public ResponseEntity<String> onQueryRequestReceived(
        @RequestParam MultiValueMap<String, String> multiMap,
        @RequestHeader Map<String, String> requestHeaders
    ) {

        String authorizationHeader = requestHeaders.get("authorization");

        if (authorizationHeader == null) {
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body("Jwt bearer token not found");
        }
        String tokenString = authorizationHeader.split(" ")[1];

        String jwtTokenPayloadEncoded = tokenString.split("\\.")[1];
        String jwtTokenPayloadDecoded = new String(Base64.decodeBase64(jwtTokenPayloadEncoded));

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

        List<String> dynamicIndices = multiMap.get("dynamicIndices");

        StringBuilder queryResultStringBuilder = new StringBuilder("[");
        try {
            for (int index = 0; index < queryIds.size(); index++) {

                String dynamicIndicesAsString = index < dynamicIndices.size() ? dynamicIndices.get(index) : null;

                QueryExecutioner executioner =  new QueryExecutioner(
                        queryIds.get(index),
                        queryValues.get(index),
                        dynamicIndicesAsString,
                        databaseName,
                        jwtTokenPayloadDecoded
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
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(queryResultStringBuilder.toString());
        }
        catch (InsufficientQueryValuesException exception) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body("Insufficient data in query param: \"values\"");
        } catch (InvalidSortFieldException exception) {
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(exception.getMessage());
        } catch (DynamicIndicesNotFoundException e) {
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(e.getClass().toString());
        } catch (DynamicIndicesParseFailureException e) {
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed parsing DynamicIndices");
        } catch (DynamicFilterAndIndicesLengthMismatchException e) {
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Dynamic indices length does not match with query template's dynamic filter");
        }

    }

}
