package com.savereplaybufferforobs;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.Format;
import java.text.MessageFormat;
import java.util.Base64;
import java.util.Map;
import java.util.Objects;

@Slf4j
public class WebSocketListenerForObs extends WebSocketListener {
    private final String password;

    public static final Integer RPC_VERSION = 1;

    private final Gson gson;

    private final DisplaysExceptions exceptionsDisplay;

    private final WebSocketClientForObs client;
    // Replay Buffer Pro SaveClip error codes the player can act on.
    private static final Map<String, String> CLIP_ERROR_HELP = Map.of(
        "buffer-inactive", "The OBS replay buffer wasn't running; start it in OBS.",
        "save-refused", "OBS refused the save; check that recording isn't paused.",
        "unavailable", "OBS was shutting down.");

    public WebSocketListenerForObs(WebSocketClientForObs client, Gson gson, String password, DisplaysExceptions displaysExceptions) {
        this.client = client;
        this.gson = gson;
        this.password = password;
        this.exceptionsDisplay = displaysExceptions;
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

    private static class RequestResponse {
        public String requestId;
        public String requestType;
        private JsonObject responseData;
        private Status requestStatus;
    }

    private static class Status {
        boolean result;
        String comment;
    }

    // Replay Buffer Pro's SaveClip reply.
    private static class ClipResult {
        boolean accepted;
        String error;
        int durationSeconds;
        boolean clamped;
    }

    private static class HealthResponse {
        public boolean outputActive;
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
            client.setConnected(true);
        } else if (response.op == 7) { // Opcode 7: RequestResponse
            RequestResponse responseData = gson.fromJson(response.d, RequestResponse.class);
            if (Objects.equals(responseData.requestId, "runelite-duration-req")) {
                handleClipResponse(responseData);
                return;
            }
            if (Objects.equals(responseData.requestType, "GetReplayBufferStatus")) {
                // healthcheck response
                HealthResponse healthResponse = gson.fromJson(responseData.responseData.toString(), HealthResponse.class);
                if (healthResponse.outputActive) {
                    exceptionsDisplay.clearObsException();
                }
                else
                {
                    exceptionsDisplay.setObsException(new ObsException("OBS Replay Buffer is not active!"));
                }
            }
        }
    }

    private void handleClipResponse(RequestResponse response) {
        if (response.requestStatus == null || !response.requestStatus.result) {
            // obs-websocket itself refused, most often because the Replay Buffer Pro vendor is not registered.
            clipFailed(response.requestStatus == null ? null : response.requestStatus.comment,
                "Make sure the Replay Buffer Pro OBS plugin is installed and up to date.");
            return;
        }
        ClipResult result = gson.fromJson(response.responseData.get("responseData"), ClipResult.class);
        if (result == null || !result.accepted) {
            String error = result == null || result.error == null ? "" : result.error;
            clipFailed(error, CLIP_ERROR_HELP.getOrDefault(error, "Check that OBS and Replay Buffer Pro are running."));
            return;
        }
        if (result.clamped) {
            log.warn("Clip shortened to the {}s OBS replay buffer", result.durationSeconds);
            exceptionsDisplay.showChatMessage("Your replay clip was shortened to " + ActivityCapture.duration(result.durationSeconds)
                + ", the length of your OBS replay buffer. Increase it in OBS to capture whole activities.");
        }
    }

    private void clipFailed(String reason, String help) {
        log.warn("OBS refused the clip request: {}", reason);
        exceptionsDisplay.showChatMessage("OBS couldn't save your replay clip. " + help);
    }

    @Override
    public void onClosing(WebSocket webSocket, int code, String reason) {
        log.info("WebSocket closed with code: {}, reason: {}", code, reason);
        client.setConnected(false);

        if (code != 1000) {
            exceptionsDisplay.setObsException(new ObsException(
                MessageFormat.format("OBS WebSocket connection closed unexpectedly: {0}", reason)
            ));
        }
    }

    @Override
    public void onFailure(WebSocket webSocket, Throwable t, Response response) {
        client.setConnected(false);
        log.info("WebSocket failed: {}", t.getMessage());
        exceptionsDisplay.setObsException(new ObsException(
                "Unable to connect to the OBS WebSocket Server. Is OBS running and configured?"
        ));
    }


}
