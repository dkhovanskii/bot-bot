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

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.symphonyoss.client.SymphonyClient;
import org.symphonyoss.client.SymphonyClientFactory;
import org.symphonyoss.client.model.Room;
import org.symphonyoss.client.model.SymAuth;
import org.symphonyoss.client.util.MlMessageParser;
import org.symphonyoss.symphony.agent.model.*;
import org.symphonyoss.symphony.clients.AuthorizationClient;
import org.symphonyoss.symphony.clients.DataFeedClient;
import org.symphonyoss.symphony.pod.model.Stream;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.*;

public class Eliza {
    private final Logger logger = LoggerFactory.getLogger(Eliza.class);
    private SymphonyClient symClient;
    private Map<String, String> initParams = new HashMap<String, String>();
    private DataFeedClient dataFeedClient;
    private Datafeed datafeed;
    private AdmAccessToken token;

    static Set<String> initParamNames = new HashSet<String>();

    static {
        initParamNames.add("sessionauth.url");
        initParamNames.add("keyauth.url");
        initParamNames.add("pod.url");
        initParamNames.add("agent.url");
        initParamNames.add("truststore.file");
        initParamNames.add("truststore.password");
        initParamNames.add("keystore.password");
        initParamNames.add("certs.dir");
        initParamNames.add("bot.user.name");
        initParamNames.add("bot.user.email");
        initParamNames.add("translate.clientid");
        initParamNames.add("translate.clientsecret");
    }

    public static void main(String[] args) {
        new Eliza();
        System.exit(0);
    }

    public Eliza() {
        initParams();
        initToken();
        initAuth();
        initDatafeed();
        listenDatafeed();
    }

    private void initParams() {
        for (String initParam : initParamNames) {
            String systemProperty = System.getProperty(initParam);
            if (systemProperty == null) {
                throw new IllegalArgumentException("Cannot find system property; make sure you're using -D" + initParam + " to run Eliza");
            } else {
                initParams.put(initParam, systemProperty);
            }
        }
    }

    private void initToken() {
        token = AdmAccessToken.getAccessToken(initParams.get("translate.clientid"), initParams.get("translate.clientsecret"));
    }

    private void initAuth() {
        try {
            symClient = SymphonyClientFactory.getClient(SymphonyClientFactory.TYPE.BASIC);

            logger.debug("{} {}", System.getProperty("sessionauth.url"),
                    System.getProperty("keyauth.url"));


            AuthorizationClient authClient = new AuthorizationClient(
                    initParams.get("sessionauth.url"),
                    initParams.get("keyauth.url"));


            authClient.setKeystores(
                    initParams.get("truststore.file"),
                    initParams.get("truststore.password"),
                    initParams.get("certs.dir") + initParams.get("bot.user.name") + ".p12",
                    initParams.get("keystore.password"));

            SymAuth symAuth = authClient.authenticate();


            symClient.init(
                    symAuth,
                    initParams.get("bot.user.email"),
                    initParams.get("agent.url"),
                    initParams.get("pod.url")
            );
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String parseXml(String xml) {
        try {
            DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
            Document document = documentBuilder.parse(new InputSource(new ByteArrayInputStream(xml.getBytes("utf-8"))));
            return document.getDocumentElement().getChildNodes().item(0).getNodeValue();
        } catch (Exception e) {
            System.out.print(e.getLocalizedMessage());
        }
        return null;
    }

    private String getLanguage(String text) {
        try {
            text = URLEncoder.encode(text);
            URL url = new URL("http://api.microsofttranslator.com/V2/Http.svc/Detect?text=" + text);
            return parseXml(createRequest(url));
        } catch (Exception e) {

        }

        return null;
    }

    private String translate(String text) {
        return translate(text, "en");
    }

    private String translate(String text, String language) {
        try {
            text = URLEncoder.encode(text);
            URL url = new URL("http://api.microsofttranslator.com/V2/Http.svc/Translate?text=" + text + "&to=" + language);
            return parseXml(createRequest(url));
        } catch (Exception e) {

        }

        return null;
    }

    private String createRequest(URL url) {
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Authorization", "Bearer " + token.getAccessToken());

            connection.setUseCaches(false);
            connection.setDoOutput(true);

            InputStream is = connection.getInputStream();
            BufferedReader rd = new BufferedReader(new InputStreamReader(is));
            StringBuilder response = new StringBuilder(); // or StringBuffer if Java version 5+
            String line;
            while ((line = rd.readLine()) != null) {
                response.append(line);
                response.append('\r');
            }
            rd.close();
            String s = response.toString();
            return s;
        } catch (Exception e) {
            System.out.print(e.getLocalizedMessage());
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
        return null;
    }

    public void initDatafeed() {
        dataFeedClient = symClient.getDataFeedClient();
        try {
            datafeed = dataFeedClient.createDatafeed();
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private MessageSubmission createMessage(String message) {
        MessageSubmission aMessage = new MessageSubmission();
        aMessage.setFormat(MessageSubmission.FormatEnum.TEXT);
        aMessage.setMessage(message);
        return aMessage;
    }

    private void sendMessage(Room room, String message) {
        MessageSubmission messageSubmission = createMessage(message);
        try {
            symClient.getMessageService().sendMessage(room, messageSubmission);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void listenDatafeed() {
        while (true) {
            try {
                Thread.sleep(4000);
                MessageList messages = dataFeedClient.getMessagesFromDatafeed(datafeed);
                if (messages != null) {
                    for (Message m : messages) {
                        m.getMessage();
                        if (!m.getFromUserId().equals(symClient.getLocalUser().getId())) {
                            processMessage(m);
                        }
                    }
                }

            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    private void processMessage(Message message) {
        String messageString = message.getMessage();
        if (StringUtils.isNotEmpty(messageString) && StringUtils.isNotBlank(messageString)) {
            MlMessageParser messageParser = new MlMessageParser();
            try {
                messageParser.parseMessage(messageString);
                String text = messageParser.getText();

                String response = createResponse(text);
                if (StringUtils.isNotEmpty(response)) {
                    Room room = createRoom(message.getStreamId());
                    sendMessage(room, response);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private String createResponse(String message) {
        String result = null;
        if (message.startsWith("/")) {
            String[] parts = message.substring(1).split(" ", 2);
            result = processCommand(parts[0], parts[1]);
        } else {
            String language = getLanguage(message);
            if (!language.equals("en")) {
                result = translate(message);
            }
        }

        return result;
    }

    private String processCommand(String command, String message) {
        String result = null;
        message = message.trim();
        if (command.equals("lang")) {
            result = Languages.LANGUAGES;
        } else {
            result = translate(message, command);
        }
        return result;
    }

    private Room createRoom(String id) {
        Stream stream = new Stream();
        stream.setId(id);
        Room room = new Room();
        room.setStream(stream);
        room.setId(stream.getId());

        return room;
    }
}
    

