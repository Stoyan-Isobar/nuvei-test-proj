import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static io.restassured.RestAssured.given;

public class GithubUserReposTests {

//    private static final String REQUEST_BODY = """
//            {
//                "name": "test_repo_3",
//                "description": "Creating an example repo"
//            }""";
    private static final int STATUS_CODE_CREATED = 201;
    private static final int UNPROCESSABLE_CONTENT = 422;
    private static final String REQUEST_PATH = "/user/repos";

    private static String uuid = generateGUID();

    @BeforeAll
    public static void setup() {
        RestAssured.baseURI = "https://api.github.com";

    }

    @Test
    public void postBody1() {
        Map<String, String> map = new HashMap<>() {
            {
                put("name", String.format("test_repo_%s", uuid));
                put("description", "Creating an example repo");
                put("homepage", "https://github.com");
            }
        };

        Response response = getResponse(map, REQUEST_PATH);


        System.out.println(convertMapToString(map));
        Assertions.assertEquals(STATUS_CODE_CREATED, response.statusCode());
    }

    @Test
    public void postBody2() {
        Map<String, String> map = new HashMap<>() {
            {
                put("name", String.format("test_repo_%s", uuid));
                put("description", "Creating an example repo");
                put("homepage", "https://github.com");
            }
        };

        Response response = getResponse(map, REQUEST_PATH);


        System.out.println(convertMapToString(map));
        Assertions.assertEquals(UNPROCESSABLE_CONTENT, response.statusCode());
    }

    private Response getResponse(Map bodyParams, String requestPath) {

        return given()
                .header("Authorization", "Bearer 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")
                .header("Accept", "application/vnd.github+json")
                .and()
                .body(bodyParams)
                .when()
                .post(requestPath)
                .then()
                .extract().response();
    }


//    private static String generateRequestBody(boolean hasName, Map<String, String> params) {
//        // Ensure the "name" parameter exists and starts with "test_repo_"; append a GUID if needed
//
//        if (hasName) {
//            String name = "test_repo_" + generateGUID();
//
//            // Check when duplicated
//            params.put("name", name);
//        }
//
//        // Convert the Map to a JSON-like string format
//        StringBuilder requestBody = new StringBuilder("{");
//        for (Map.Entry<String, String> entry : params.entrySet()) {
//            requestBody.append("\"").append(entry.getKey()).append("\":\"").append(entry.getValue()).append("\",");
//        }
//        // Remove the trailing comma if there are parameters
//        if (requestBody.charAt(requestBody.length() - 1) == ',') {
//            requestBody.setLength(requestBody.length() - 1);
//        }
//        requestBody.append("}");
//
//        return requestBody.toString();
//    }

    private static String generateGUID() {
        // Generate a random GUID (UUID)
        return UUID.randomUUID().toString();
    }

    private String convertMapToString(Map map) {
        return StringUtils.join(map);
    }
}
