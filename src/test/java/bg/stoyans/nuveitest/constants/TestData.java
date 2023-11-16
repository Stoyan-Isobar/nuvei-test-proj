package bg.stoyans.nuveitest.constants;

import java.util.HashMap;
import java.util.Map;

public class TestData {
	public static Map<String, String> REQUEST_BODY_HAPPY_PATH = new HashMap<>() {
        {
            put("name", "test_repo_");
            put("description", "Creating an example repo");
        }
    };
    public static Map<String, String> REQUEST_BODY_IDEMPOTENCY1 = new HashMap<>() {
        {
            put("name", "test_repo_");
            put("description", "Creating an example repo");
        }
    };

    public static Map<String, String> REQUEST_BODY_SQL_INJ = new HashMap<>(){
        {
            put("name", "OR 1=1_");
            put("description", "Creating an example repo");
            put("homepage", "https://github.com");
        }
    };

}
