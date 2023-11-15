package com.stoyans.nuveitest;

import com.stoyans.nuveitest.constants.RequestAttributes;
import com.stoyans.nuveitest.constants.StatusCodes;
import com.stoyans.nuveitest.constants.TestData;
import com.stoyans.nuveitest.utils.TestsHelper;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import io.restassured.path.json.JsonPath;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import static io.restassured.RestAssured.form;
import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.*;

public class GithubUserReposTests {

    private static String uuid;

    @BeforeAll
    public static void setup() {
        RestAssured.baseURI = RequestAttributes.BASE_URI;
        uuid = TestsHelper.generateGUID();
    }

    @Test
    public void givenMandatoryKeysOkAuth_whenPost_thenCheckStatusCodeAndResponseBody() {

        Map<String, String> requestBody = TestsHelper.changeNameKeyInBody(uuid, TestData.REQUEST_BODY_HAPPY_PATH);
        Response response = TestsHelper.getResponse(requestBody, RequestAttributes.POST_REQUEST_PATH, RequestAttributes.BEARER_TOKEN);

        // print the request to check the logs for errors
        System.out.println(requestBody);

        // assertion for response status code
        Assertions.assertEquals(StatusCodes.CREATED, response.statusCode());

        //Assertions for "name" and "description" fields in the response body
        String responseBody = response.getBody().asString();
        assertTrue(responseBody.contains("\"name\":\"" + requestBody.get("name") + "\""));
        assertTrue(responseBody.contains("\"description\":\"" + requestBody.get("description") + "\""));

        //assertions for some expected response keys according to the response contract
        assertTrue(responseBody.contains("\"id\":"));
        assertTrue(responseBody.contains("\"node_id\":"));
        assertTrue(responseBody.contains("\"full_name\":"));

        // Assertion for the "owner" object in the response body
        assertTrue(responseBody.contains("\"owner\":"));

        // Assertions for keys in the "owner" object
        assertNotNull(JsonPath.from(responseBody).get("owner.login"));
        assertNotNull(JsonPath.from(responseBody).get("owner.id"));
        assertNotNull(JsonPath.from(responseBody).get("owner.node_id"));
        assertNotNull(JsonPath.from(responseBody).get("owner.url"));

        // whole JSON schema validation
        ClassLoader classLoader = getClass().getClassLoader();
        File jsonSchemaFile = new File(classLoader.getResource("schemas/new_github_repo.json").getFile());
        assertTrue(TestsHelper.validateJsonSchema(responseBody, jsonSchemaFile));
    }
    @Test
    public void givenWrongAuth_whenPost_thenCheckError () {
        Map<String, String> map = new HashMap<>() {
            {
                put("name", String.format("test_repo_%s", uuid));
                put("description", "Creating an example repo");
            }
        };
        Response response = TestsHelper.getResponse(map, RequestAttributes.POST_REQUEST_PATH, RequestAttributes.INCORRECT_BEARER_TOKEN);

        //print the request to check the logs for errors
        System.out.println(map);

        // Assertion for response status code
        Assertions.assertEquals(StatusCodes.UNAUTORIZED, response.statusCode());

        // schema validation
        String responseBody = response.getBody().asString();
        ClassLoader classLoader = getClass().getClassLoader();
        File jsonSchemaFile = new File(classLoader.getResource("schemas/bad_credentials.json").getFile());
        assertTrue(TestsHelper.validateJsonSchema(responseBody, jsonSchemaFile));
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

        Response response = TestsHelper.getResponse(map, RequestAttributes.POST_REQUEST_PATH, RequestAttributes.BEARER_TOKEN);

        System.out.println(map);
        Assertions.assertEquals(StatusCodes.UNPROCESSABLE_CONTENT, response.statusCode());

        // schema validation
        String responseBody = response.getBody().asString();
        ClassLoader classLoader = getClass().getClassLoader();
        File jsonSchemaFile = new File(classLoader.getResource("schemas/idempotency.json").getFile());
        assertTrue(TestsHelper.validateJsonSchema(responseBody, jsonSchemaFile));
    }
    @Test
    //As owner of create_repo API
    //In order to provide safe usage of special symbols
    //I want to validate for and substitute special symbols
    public void givenSpecialSymbolsInName_whenPost_thenCheckNameKeyValue(){
        Map<String, String> map = new HashMap<>() {
            {
                put("name", String.format("OR 1=1_%s", uuid));
                put("description", "Creating an example repo");
                put("homepage", "https://github.com");
            }
        };

        String expectedName = String.format("OR-1-1_%s", uuid);
        Response response = TestsHelper.getResponse(map, RequestAttributes.POST_REQUEST_PATH, RequestAttributes.BEARER_TOKEN);

        // print the request to check the logs for errors
        System.out.println(map);

        // assertion for response status code
        Assertions.assertEquals(StatusCodes.CREATED, response.statusCode());

        //Assertions for "name" and "description" fields in the response body
        String responseBody = response.getBody().asString();
        assertTrue(responseBody.contains("\"name\":\"" + expectedName + "\""));
    }

    @Test
    //As architect of create_repo API
    //In order to apply to "decoupled collaboration"
    //I want to read only the keys that are specified in the contract, if same key is passed - read the last one

    public void givenNameKeySeveralTimes_whenPost_thenOnlyApplyLastKeyValue(){
        Map<String, String> map = new HashMap<>() {
            {
                put("name", String.format("test_repo1_%s", uuid));
                put("name", String.format("test_repo2_%s", uuid));
                put("test", String.format("test_repo3_%s", uuid));
                put("OR 1=1", "test");
                put("description", "Creating an example repo");
                put("homepage", "https://github.com");
            }
        };

        String expectedName = String.format("test_repo2_%s", uuid);
        Response response = TestsHelper.getResponse(map, RequestAttributes.POST_REQUEST_PATH, RequestAttributes.BEARER_TOKEN);

        // print the request to check the logs for errors
        System.out.println(map);

        // assertion for response status code
        Assertions.assertEquals(StatusCodes.CREATED, response.statusCode());

        //Assertions for "name" and "description" fields in the response body
        String responseBody = response.getBody().asString();
        assertTrue(responseBody.contains("\"name\":\"" + expectedName + "\""));
    }
    @AfterAll
    public static void cleanup () {
        String repoName = "test_repo_" + uuid;
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
