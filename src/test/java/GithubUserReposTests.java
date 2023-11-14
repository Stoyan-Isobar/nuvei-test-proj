import io.restassured.RestAssured;
import io.restassured.response.Response;
import io.restassured.module.jsv.JsonSchemaValidator;
import io.restassured.path.json.JsonPath;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static io.restassured.RestAssured.form;
import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class GithubUserReposTests {

    private static final String GITHUB_USER = "Stoyan_Isobar";
    private static final String BASE_URI = "https://api.github.com";
    //private static final String DELETE_REQUEST_PATH ="/repos/";
    private static final String POST_REQUEST_PATH = "/user/repos";
    private static final String BEARER_TOKEN = "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";
    private static final int SC_CREATED = 201;
    private static final int SC_NO_CONTENT = 204;
    private static final int SC_UNPROCESSABLE_CONTENT = 422;



    private static String uuid;

    @BeforeAll
    public static void setup() {
        RestAssured.baseURI = BASE_URI;
        uuid = generateGUID();
    }

    @Test
    public void givenMandatoryKeys_whenPost_thenCheckStatusCodeAndResponseBody() {
        Map<String, String> map = new HashMap<>() {
            {
                put("name", String.format("test_repo_%s", uuid));
                put("description", "Creating an example repo");
            }
        };

        Response response = getResponse(map, POST_REQUEST_PATH);

        //print the request to check the logs for errors
        System.out.println(map);

        // Assertions for response status code
        Assertions.assertEquals(SC_CREATED, response.statusCode());

        // Assertions for "name" and "description" fields in the response body
        String responseBody = response.getBody().asString();
        assertTrue(responseBody.contains("\"name\":\"" + map.get("name") + "\""));
        assertTrue(responseBody.contains("\"description\":\"" + map.get("description") + "\""));

        // Assertions for some expected response keys according to the response contract
        assertTrue(responseBody.contains("\"id\":"));
        assertTrue(responseBody.contains("\"node_id\":"));
        assertTrue(responseBody.contains("\"full_name\":"));

        // Assertions for the "owner" object in the response body
        assertTrue(responseBody.contains("\"owner\":"));

        // Assertions for keys in the "owner" object
        assertNotNull(JsonPath.from(responseBody).get("owner.login"));
        assertNotNull(JsonPath.from(responseBody).get("owner.id"));
        assertNotNull(JsonPath.from(responseBody).get("owner.node_id"));
        assertNotNull(JsonPath.from(responseBody).get("owner.url"));

        // whole JSON schema validation
        ClassLoader classLoader = getClass().getClassLoader();
        File jsonSchemaFile = new File(classLoader.getResource("new_github_repo.json").getFile());
        assertTrue(validateJsonSchema(responseBody, jsonSchemaFile));
    }
    @Test
    public void givenMandatoryKeys_whenPost_thenCheckSchema () {

    }
    @Test
    //As integrated system
    //In order to provide expected errors to clients
    //I want to receive status code and error/info msg about idempotency issues
    public void givenNameAlreadyCreated_whenPost_thenCheckStatusCodeAndErrorMSG() {
        Map<String, String> map = new HashMap<>() {
            {
                put("name", String.format("test_repo_%s", uuid));
                put("description", "Creating an example repo");
                put("homepage", "https://github.com");
            }
        };

        Response response = getResponse(map, POST_REQUEST_PATH);


        System.out.println(map);
        Assertions.assertEquals(SC_UNPROCESSABLE_CONTENT, response.statusCode());

    }

    @AfterAll
    public static void cleanup () {
        String repoName = "test_repo_" + uuid;
        String deleteRepoEndpoint = String.format("/repos/%s/%s", GITHUB_USER, repoName);

        Response response = given()
                .header("Authorization", "Bearer " + BEARER_TOKEN)
                .header("Accept", "application/vnd.github+json")
                .when()
                .delete(deleteRepoEndpoint);

        // Print the response to check for error message in case the delete request failed
        System.out.println(response.getBody().asString());
        Assertions.assertEquals(SC_NO_CONTENT, response.statusCode());
    }
    private Response getResponse(Map bodyParams, String requestPath) {

        return given()
                .header("Authorization", "Bearer " + BEARER_TOKEN)
                .header("Accept", "application/vnd.github+json")
                .and()
                .body(bodyParams)
                .when()
                .post(requestPath)
                .then()
                .extract().response();
    }
    private boolean validateJsonSchema(String responseBody, File jsonSchemaFile) {
        return JsonSchemaValidator.matchesJsonSchema(jsonSchemaFile).matches(responseBody);
    }
    private static String generateGUID() {
        // Generate a random GUID (UUID)
        return UUID.randomUUID().toString();
    }

}
