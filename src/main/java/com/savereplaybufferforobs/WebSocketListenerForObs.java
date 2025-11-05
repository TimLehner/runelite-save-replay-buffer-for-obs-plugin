package com.savereplaybufferforobs;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

@Slf4j
public class WebSocketListenerForObs extends WebSocketListener {
    private final String password;

    public static final Integer RPC_VERSION = 1;

    private final Gson gson;

    public WebSocketListenerForObs(Gson gson, String password) {
        this.gson = gson;
        this.password = password;
    }


    private static class ObsV5Message {
        public int op;
        public JsonElement d;
    }

    private static class HelloData {
        public String obsStudioVersion;
        public String obsWebSocketVersion;
        public int rpcVersion;
        public AuthenticationData authentication; // This object holds salt/challenge

        private static class AuthenticationData {
            public String challenge;
            public String salt;
        }
    }

    private static class IdentifyRequest {
        private final int op = 1;
        private final IdentifyData d;

        public IdentifyRequest(String authenticationResponse) {
            this.d = new IdentifyData(authenticationResponse);
        }

        private static class IdentifyData {
            public final int rpcVersion = RPC_VERSION;
            public String authentication;

            public IdentifyData(String authenticationResponse) {
                // Only include the authentication field if a response is provided
                if (authenticationResponse != null && !authenticationResponse.isEmpty()) {
                    this.authentication = authenticationResponse;
                }
            }
        }
    }

    private String computeAuthentication(String salt, String challenge) {
        // this function is copied from obs-websocket-java
        // https://github.com/obs-websocket-community-projects/obs-websocket-java/blob/2ff769d4819935aac44fbf38e003773934ddbb55/client/src/main/java/io/obswebsocket/community/client/authenticator/AuthenticatorImpl.java#L20

        //        MIT License
        //
        //        Copyright (c) 2020 Twasi
        //        Copyright (c) 2021 Christophe Carvalho Vilas-Boas
        //        Copyright (c) 2021 TinaTiel
        //        Copyright (c) 2021 Pjiesco
        //
        //        Permission is hereby granted, free of charge, to any person obtaining a copy
        //        of this software and associated documentation files (the "Software"), to deal
        //        in the Software without restriction, including without limitation the rights
        //        to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
        //        copies of the Software, and to permit persons to whom the Software is
        //        furnished to do so, subject to the following conditions:
        //
        //        The above copyright notice and this permission notice shall be included in all
        //        copies or substantial portions of the Software.
        //
        //                THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
        //        IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
        //        FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
        //        AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
        //        LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
        //                OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
        //        SOFTWARE.

        // Sanitize
        if (salt == null || challenge == null) {
            throw new IllegalArgumentException("Password, salt, and challenge are required");
        }

        // Compute
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            String secretString = password + salt;
            byte[] secretHash = digest.digest(secretString.getBytes(StandardCharsets.UTF_8));
            String encodedSecret = Base64.getEncoder().encodeToString(secretHash);

            String resultString = encodedSecret + challenge;
            byte[] resultHash = digest.digest(resultString.getBytes(StandardCharsets.UTF_8));

            return Base64.getEncoder().encodeToString(resultHash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Could not find expected message digest to compute auth", e);
        }
    }


    @Override
    public void onOpen(WebSocket webSocket, Response response) {
        log.debug("WebSocket opened: {}", response.message());
    }

    @Override
    public void onMessage(WebSocket webSocket, String text) {
        log.debug("Received text: {}", text);
        ObsV5Message response = gson.fromJson(text, ObsV5Message.class);
        if (response.op == 0) {
            HelloData helloData = gson.fromJson(response.d, HelloData.class);
            String authResponse = computeAuthentication(helloData.authentication.salt, helloData.authentication.challenge);
            log.debug("Sending Identify request");
            IdentifyRequest identifyRequest = new IdentifyRequest(authResponse);
            webSocket.send(gson.toJson(identifyRequest));
        } else if (response.op == 2) { // Opcode 2: Identified (Success after Identify/Auth)
            log.info("OBS successfully Identified. Connection ready.");
        }
    }

    @Override
    public void onClosing(WebSocket webSocket, int code, String reason) {
        log.info("WebSocket closed with code: {}, reason: {}", code, reason);
    }
}
