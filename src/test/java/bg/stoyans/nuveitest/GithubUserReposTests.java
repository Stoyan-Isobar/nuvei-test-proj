package bg.stoyans.nuveitest;

import bg.stoyans.nuveitest.utils.TestsHelper;
import bg.stoyans.nuveitest.constants.RequestAttributes;
import bg.stoyans.nuveitest.constants.StatusCodes;
import bg.stoyans.nuveitest.constants.TestData;
import io.restassured.RestAssured;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import org.junit.jupiter.api.*;

import java.io.File;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class GithubUserReposTests {

    private static String uuid;

    @BeforeAll
    public static void setup() {
        RestAssured.baseURI = RequestAttributes.BASE_URI;
        uuid = TestsHelper.generateGUID();
    }

    @Test
    @Order(1)
    public void givenMandatoryKeysOkAuth_whenPost_thenCheckStatusCodeAndResponseBody() {
        //create request and change the name so that we are sure a repo with that name is not already present in the system under test
        Map<String, String> requestBody = TestsHelper.changeNameKeyInBody(uuid, TestData.REQUEST_BODY_HAPPY_PATH);
        Response response = TestsHelper.getResponse(requestBody, RequestAttributes.POST_REQUEST_PATH, RequestAttributes.BEARER_TOKEN);

        // print the request to check the logs for errors
        System.out.println(requestBody);

        // assertion for response status code
        Assertions.assertEquals(StatusCodes.CREATED, response.statusCode());

        //Assertions for "name" and "description" fields in the response body
        String responseBody = response.getBody().asString();
        String expectedName = String.format("test_repo_%s", uuid);

        assertTrue(responseBody.contains("\"name\":\"" + expectedName + "\""));
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

        //TODO: check if resource is not null before getting the file, else call an error that the file is missing
        File jsonSchemaFile = new File(classLoader.getResource("schemas/new_github_repo.json").getFile());
        assertTrue(TestsHelper.validateJsonSchema(responseBody, jsonSchemaFile));

        //delete created repo if status code = created
        TestsHelper.sendDeleteRequest(expectedName);
    }

    @Test
    @Order(2)
    public void givenWrongAuth_whenPost_thenCheckError() {
        //use the same value for name as in test 1 as it doesn't affect the test
        Map<String, String> requestBody = TestsHelper.changeNameKeyInBody(uuid, TestData.REQUEST_BODY_HAPPY_PATH);
        Response response = TestsHelper.getResponse(requestBody, RequestAttributes.POST_REQUEST_PATH, RequestAttributes.INCORRECT_BEARER_TOKEN);

        //print the request to check the logs for errors
        System.out.println(requestBody);

        // Assertion for response status code
        Assertions.assertEquals(StatusCodes.UNAUTORIZED, response.statusCode());

        // schema validation
        String responseBody = response.getBody().asString();
        ClassLoader classLoader = getClass().getClassLoader();
        File jsonSchemaFile = new File(classLoader.getResource("schemas/bad_credentials.json").getFile());
        assertTrue(TestsHelper.validateJsonSchema(responseBody, jsonSchemaFile));
    }

    @Test
    @Order(3)
    //As integrated system
    //In order to provide expected errors to clients
    //I want to receive status code and error/info msg about idempotency issues
    public void givenNameAlreadyCreated_whenPost_thenCheckStatusCodeAndErrorMSG() {
        //use the same value for name as in test1 as this is the test data we need to check idempotency handling
        Map<String, String> requestBody = TestsHelper.changeNameKeyInBody(uuid, TestData.REQUEST_BODY_IDEMPOTENCY1);
        Response testSetupResponse = TestsHelper.getResponse(requestBody, RequestAttributes.POST_REQUEST_PATH, RequestAttributes.BEARER_TOKEN);

        System.out.println(requestBody);
        Assertions.assertEquals(StatusCodes.CREATED, testSetupResponse.statusCode());

        String expectedName = String.format("test_repo_%s", uuid);
        String testSetupResponseBody = testSetupResponse.getBody().asString();
        assertTrue(testSetupResponseBody.contains("\"name\":\"" + expectedName + "\""));

        Response response = TestsHelper.getResponse(requestBody, RequestAttributes.POST_REQUEST_PATH, RequestAttributes.BEARER_TOKEN);

        System.out.println(requestBody);
        Assertions.assertEquals(StatusCodes.UNPROCESSABLE_CONTENT, response.statusCode());

        // schema validation
        String responseBody = response.getBody().asString();
        ClassLoader classLoader = getClass().getClassLoader();
        File jsonSchemaFile = new File(classLoader.getResource("schemas/idempotency.json").getFile());
        assertTrue(TestsHelper.validateJsonSchema(responseBody, jsonSchemaFile));

        //delete created repo if status code = created
        TestsHelper.sendDeleteRequest(expectedName);
    }

    @Test
    @Order(4)
    //As owner of create_repo API
    //In order to provide safe usage of special symbols
    //I want to validate for and substitute special symbols
    public void givenSpecialSymbolsInName_whenPost_thenCheckNameKeyValue() {
        //build a body with name value including special symbols
        Map<String, String> requestBody = TestsHelper.changeNameKeyInBody(uuid, TestData.REQUEST_BODY_SQL_INJ);

        //set expected results based on expectations for system behavior
        String expectedName = String.format("OR-1-1_%s", uuid);
        Response response = TestsHelper.getResponse(requestBody, RequestAttributes.POST_REQUEST_PATH, RequestAttributes.BEARER_TOKEN);

        // print the request to check the logs for errors
        System.out.println(requestBody);

        // assertion for response status code
        Assertions.assertEquals(StatusCodes.CREATED, response.statusCode());

        //Assertions for "name" and "description" fields in the response body
        String responseBody = response.getBody().asString();
        assertTrue(responseBody.contains("\"name\":\"" + expectedName + "\""));

        //delete created repo if status code = created
        TestsHelper.sendDeleteRequest(expectedName);
    }

    @Test
    @Order(5)
    //As architect of create_repo API
    //In order to apply to "decoupled collaboration"
    //I want to read only the keys that are specified in the contract, if same key is passed - read the last one

    public void givenNameKeySeveralTimes_whenPost_thenOnlyApplyLastKeyValue() {
        //we use 'magic' name for the repo so that we can clean it up later on
        String requestBody = String.format(
                """
                        {
                            "name": "test2_repo2_%1$s",
                            "name": "test2_repo_%1$s",
                            "test": "test2_repo_%1$s",
                            "description": "Creating an example repo",
                            "homepage": "https://github.com"
                        }""",
                uuid
        );

        Response response = given()
                .header("Authorization", "Bearer " + RequestAttributes.BEARER_TOKEN)
                .header("Accept", "application/vnd.github+json")
                .and()
                .body(requestBody)
                .when()
                .post(RequestAttributes.POST_REQUEST_PATH)
                .then()
                .extract().response();

        String expectedName = String.format("test2_repo_%s", uuid);
//        Response response = TestsHelper.getResponse(requestBody, RequestAttributes.POST_REQUEST_PATH, RequestAttributes.BEARER_TOKEN);

        // print the request to check the logs for errors
        System.out.println(requestBody);

        // assertion for response status code
        Assertions.assertEquals(StatusCodes.CREATED, response.statusCode());

        //Assertions for "name" and "description" fields in the response body
        String responseBody = response.getBody().asString();
        assertTrue(responseBody.contains("\"name\":\"" + expectedName + "\""));

        //delete created repo if status code = created
        TestsHelper.sendDeleteRequest(expectedName);
    }

}
