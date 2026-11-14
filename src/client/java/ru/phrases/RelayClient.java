package ru.phrases;

import net.minecraft.client.Minecraft;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionStage;

final class RelayClient {
    private static final Path CONFIG = Path.of("config", "phrases-relay.properties");
    private static final String DEFAULT_URL = "wss://phrases-relay.phrases-relay.workers.dev";
    private static final HttpClient HTTP = HttpClient.newHttpClient();
    private static String url = DEFAULT_URL;
    private static WebSocket socket;
    private static long lastAttempt;
    private static boolean configLoaded;
    private static boolean connecting;
    private static String pendingPhrase;

    private RelayClient() {}

    static void tick(Minecraft client) {
        loadConfig();
        if (url.isBlank() || client.player == null || socket != null || connecting) return;
        long now = System.currentTimeMillis();
        if (now - lastAttempt < 5000L) return;
        lastAttempt = now;
        connecting = true;
        try {
            HTTP.newWebSocketBuilder().buildAsync(URI.create(url), new Listener(client))
                    .exceptionally(error -> {
                        connecting = false;
                        return null;
                    });
        } catch (IllegalArgumentException ignored) {
            url = "";
            connecting = false;
        }
    }

    static void sendPhrase(String id) {
        Minecraft client = Minecraft.getInstance();
        String name = client.player == null ? "" : client.player.getName().getString();
        String message = "{\"type\":\"phrase\",\"id\":\"" + jsonEscape(id)
                + "\",\"name\":\"" + jsonEscape(name) + "\"}";
        if (socket != null && !socket.isOutputClosed()) {
            socket.sendText(message, true);
        } else {
            // Не теряем нажатие, если WebSocket ещё устанавливается.
            pendingPhrase = message;
        }
    }

    private static String jsonEscape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static void loadConfig() {
        if (configLoaded) return;
        configLoaded = true;
        if (!Files.exists(CONFIG)) {
            try {
                Files.createDirectories(CONFIG.getParent());
                Files.writeString(CONFIG, "url=" + DEFAULT_URL + System.lineSeparator());
            } catch (IOException ignored) {
                // Конфиг необязателен: адрес relay уже зашит в мод.
            }
            return;
        }
        Map<String, String> values = new HashMap<>();
        try {
            List<String> lines = Files.readAllLines(CONFIG);
            for (String line : lines) {
                int separator = line.indexOf('=');
                if (separator > 0) values.put(line.substring(0, separator).trim(), line.substring(separator + 1).trim());
            }
            String configuredUrl = values.get("url");
            url = configuredUrl == null || configuredUrl.isBlank() ? DEFAULT_URL : configuredUrl;
        } catch (IOException ignored) {
            // Сетевой relay необязателен: локальные звуки продолжают работать без него.
        }
    }

    private static final class Listener implements WebSocket.Listener {
        private final Minecraft client;

        private Listener(Minecraft client) {
            this.client = client;
        }

        @Override
        public void onOpen(WebSocket webSocket) {
            socket = webSocket;
            connecting = false;
            webSocket.sendText("{\"type\":\"join\"}", true);
            if (pendingPhrase != null) {
                webSocket.sendText(pendingPhrase, true);
                pendingPhrase = null;
            }
            webSocket.request(1);
        }

        @Override
        public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
            String message = data.toString();
            if (message.startsWith("{\"type\":\"phrase\"") && message.endsWith("}")) {
                String id = field(message, "id");
                String name = field(message, "name");
                client.execute(() -> PhrasesClient.playRemotePhrase(id, name));
            }
            webSocket.request(1);
            return null;
        }

        private static String field(String json, String name) {
            String marker = "\"" + name + "\":\"";
            int start = json.indexOf(marker);
            if (start < 0) return "";
            start += marker.length();
            int end = json.indexOf('"', start);
            return end < 0 ? "" : json.substring(start, end);
        }

        @Override
        public void onError(WebSocket webSocket, Throwable error) {
            socket = null;
            connecting = false;
            lastAttempt = 0L;
        }

        @Override
        public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
            socket = null;
            connecting = false;
            lastAttempt = 0L;
            return null;
        }
    }
}
