package com.softwareco.intellij.plugin.user;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.softwareco.intellij.plugin.SoftwareCoSessionManager;
import com.softwareco.intellij.plugin.SoftwareCoUtils;
import com.softwareco.intellij.plugin.SoftwareResponse;
import org.apache.http.client.methods.HttpGet;

public class UserSessionManager {

    public static class User {
        public String jwt;
        public String email;
    }

    private static User getUser() {
        User user = null;
        String pluginjwt = SoftwareCoSessionManager.getItem("jwt");

        String api = "/users/plugin/state";
        SoftwareResponse resp = SoftwareCoUtils.makeApiCall(api, HttpGet.METHOD_NAME, null, pluginjwt);
        if (resp.isOk()) {
            // check if we have the data and jwt
            // resp.data.jwt and resp.data.user
            // then update the session.json for the jwt
            JsonObject data = resp.getJsonObj();
            String state = (data != null && data.has("state")) ? data.get("state").getAsString() : "UNKNOWN";
            // check if we have any data
            if (state.equals("OK")) {
                String dataJwt = data.get("jwt").getAsString();
                SoftwareCoSessionManager.setItem("jwt", dataJwt);
                String dataEmail = data.get("email").getAsString();
                if (dataEmail != null) {
                    SoftwareCoSessionManager.setItem("name", dataEmail);
                }

                user = new User();
                user.email = dataEmail;
                user.jwt = dataJwt;

            } else if (state.equals("NOT_FOUND")) {
                SoftwareCoSessionManager.setItem("jwt", null);
            }
        }

        return user;
    }
    
}
