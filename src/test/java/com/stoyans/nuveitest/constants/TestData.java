package com.stoyans.nuveitest.constants;

import java.util.HashMap;
import java.util.Map;

public class TestData {
	public static Map<String, String> REQUEST_BODY_HAPPY_PATH = new HashMap<>() {
        {
            put("name", "test_repo_");
            put("description", "Creating an example repo");
        }
    };
//    public static final String REQUEST_BODY_HAPPY_PATH = """
//        {
//	        "name": "test_repo_%s",
//	        "description": "Creating an example repo"
//        }
//        """;
}
