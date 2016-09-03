/*
 *
 *
 * Copyright 2016 Symphony Communication Services, LLC
 *
 * Licensed to Symphony Communication Services, LLC under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *
 */

package org.symphonyoss.simplebot;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Timer;
import java.util.TimerTask;

import com.google.gson.Gson;

//Class to get access token from Azure Data market
public class AdmAccessToken {
    private static class AdmAccessTokenTemplate {
        public String access_token;
        private String token_type;
        private String expires_in;
        private String scope;
    }

    public static AdmAccessToken getAccessToken(String clientId, String clientSecret) {
        AdmAccessTokenTemplate template = getAccessTokenTemplate(clientId, clientSecret);

        AdmAccessToken token = new AdmAccessToken();
        token.template = template;
        token.clientId = clientId;
        token.clientSecret = clientSecret;
        token.triggerRefresh();

        return token;
    }

    private static AdmAccessTokenTemplate getAccessTokenTemplate(String clientId, String clientSecret) {
        AdmAccessTokenTemplate accessToken = null;
        try {
            String charset = StandardCharsets.UTF_8.name();
            String dataMarketUrl = "https://datamarket.accesscontrol.windows.net/v2/OAuth2-13";
            String params = "grant_type=client_credentials&scope=http://api.microsofttranslator.com"
                    + "&client_id=" + URLEncoder.encode(clientId, charset)
                    + "&client_secret=" + URLEncoder.encode(clientSecret, charset);
            URL url = new URL(dataMarketUrl);

            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=" + charset);
            connection.setRequestProperty("Accept-Charset", charset);
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            try (OutputStreamWriter wr = new OutputStreamWriter(connection.getOutputStream())) {
                wr.write(params);
                wr.flush();
            }
            if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                // OK
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), charset))) {
                    StringBuffer res = new StringBuffer();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        res.append(line);
                    }

                    //Json deserialize the access token
                    Gson gson = new Gson();
                    accessToken = gson.fromJson(res.toString(), AdmAccessTokenTemplate.class);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return accessToken;
    }

    private AdmAccessTokenTemplate template;
    private String clientId;
    private String clientSecret;

    public String getAccessToken() {
        return template.access_token;
    }

    private void triggerRefresh() {
        try {
            int expirationTime = Integer.parseInt(template.expires_in);
            int refreshTime = ((Double) (expirationTime * .9)).intValue();

            Timer timer = new Timer();
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    template = getAccessTokenTemplate(clientId, clientSecret);
                    triggerRefresh();
                }
            }, refreshTime);
        } catch (NumberFormatException e) {

        }
    }
}