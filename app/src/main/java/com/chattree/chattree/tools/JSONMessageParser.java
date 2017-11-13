package com.chattree.chattree.tools;

import org.json.JSONException;
import org.json.JSONObject;

public class JSONMessageParser {

    static private final String DATA_KEY    = "data";
    static private final String SUCCESS_KEY = "success";
    static private final String MESSAGE_KEY = "success";

    private String     JSONString;
    private JSONObject object;

    public JSONMessageParser(String JSONString) throws JSONException {
        this.JSONString = JSONString;
        this.object = new JSONObject(JSONString);
    }

    public JSONObject getData() throws JSONException {
        return object.getJSONObject(DATA_KEY);
    }

    public boolean getSuccess() throws JSONException {
        return object.getBoolean(SUCCESS_KEY);
    }

    public String getMessage() throws JSONException {
        return object.getString(MESSAGE_KEY);
    }

    public JSONObject getObject() {
        return object;
    }

    public String getJSONString() {
        return JSONString;
    }
}
