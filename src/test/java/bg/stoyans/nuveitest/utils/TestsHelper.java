package bg.stoyans.nuveitest.utils;

import bg.stoyans.nuveitest.constants.RequestAttributes;
import bg.stoyans.nuveitest.constants.StatusCodes;
import io.restassured.module.jsv.JsonSchemaValidator;
import io.restassured.response.Response;
import org.junit.jupiter.api.Assertions;

import java.io.File;
import java.util.Map;
import java.util.UUID;

import static io.restassured.RestAssured.given;

public class TestsHelper {
    public static Response getResponse(Map<String, String> bodyParams, String requestPath, String bearerToken) {

        return given()
                .header("Authorization", "Bearer " + bearerToken)
                .header("Accept", "application/vnd.github+json")
                .and()
                .body(bodyParams)
                .when()
                .post(requestPath)
                .then()
                .extract().response();
    }
    public static boolean validateJsonSchema(String responseBody, File jsonSchemaFile) {
        return JsonSchemaValidator.matchesJsonSchema(jsonSchemaFile).matches(responseBody);
    }
    public static String generateGUID() {
        // Generate a random GUID (UUID)
        return UUID.randomUUID().toString();
    }
    //adding uuid to the name so that each repo has unique name, needed to make sure tests where repo creation is expected to be successful
    //TODO: change the map to work with object to solve case of nested objects in request body, to make the method usable in all cases
    public static Map<String, String> changeNameKeyInBody(String uuid, Map<String, String> body){
        body.put("name", body.get("name") + uuid);
        return  body;
    }
    public static void sendDeleteRequest (String repoName){
        String deleteRepoEndpoint = String.format("/repos/%s/%s", RequestAttributes.GITHUB_USER, repoName);
        Response response = given()
                .header("Authorization", "Bearer " + RequestAttributes.BEARER_TOKEN)
                .header("Accept", "application/vnd.github+json")
                .when()
                .delete(deleteRepoEndpoint);

        // Print the response to check for error message in case the delete request failed
        System.out.println(response.getBody().asString());
        Assertions.assertEquals(StatusCodes.NO_CONTENT, response.statusCode());
    }
}
