package demo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import LCE.LCEngine;

/**
 * DemoServer (API ONLY)
 *
 * Exposes:
 *   GET /v1/search?q=
 *   GET /v1/autocomplete?prefix=
 *   GET /v1/doc?id=
 *   GET /v1/health
 *
 * Designed to run behind:
 *   Nginx → API Gateway → This service
 */
public class DemoServer {

    // ================= CONFIG =================

    private static final int PORT = Integer.parseInt(
            System.getenv().getOrDefault("PORT", "9000"));

    private static final int TOP_K = 10;
    private static final int TITLE_WORD_LIMIT = 12;
    private static final int PREVIEW_LINE_LIMIT = 3;
    private static final int PREVIEW_CHAR_LIMIT = 350;
    private static final int MAX_QUERY_LENGTH = 500;

    private static final Path INDEX_PATH = Paths.get("data/index.lce");
    private static final Path DOC_DIR = Paths.get("data/fineweb_subset");

    private final LCEngine engine;
    private final Map<Integer, Path> docMap;

    // ================= CONSTRUCTOR =================

    public DemoServer() throws Exception {
        System.out.println("Starting DemoServer (API_ONLY) on port " + PORT);

        System.out.println("Loading index...");
        long start = System.nanoTime();
        this.engine = LCEngine.load(INDEX_PATH);
        long end = System.nanoTime();

        System.out.printf(
                "Index loaded in %.2f seconds%n",
                (end - start) / 1_000_000_000.0
        );

        this.docMap = loadDocMap();
        this.engine.rebuildAutocompleteTrie();
    }

    private Map<Integer, Path> loadDocMap() throws IOException {
        Map<Integer, Path> map = new HashMap<>();

        if (!Files.exists(DOC_DIR) || !Files.isDirectory(DOC_DIR)) {
            System.out.println(
                "Warning: document directory not found at " + DOC_DIR +
                ". /v1/doc endpoint will be limited."
            );
            return map;
        }

        try (var files = Files.list(DOC_DIR)) {
            for (Path p : files.toList()) {
                String name = p.getFileName().toString();
                if (!name.endsWith(".txt")) continue;

                try {
                    int docId = Integer.parseInt(name.replace(".txt", ""));
                    map.put(docId, p);
                } catch (NumberFormatException ignored) {}
            }
        }

        return map;
    }

    // ================= SERVER START =================

    public void start() throws IOException {
        HttpServer server = HttpServer.create(
                new InetSocketAddress(PORT), 0);

        server.createContext("/v1/search", this::handleSearch);
        server.createContext("/v1/autocomplete", this::handleAutocomplete);
        server.createContext("/v1/doc", this::handleDoc);
        server.createContext("/v1/health", this::handleHealth);

        server.setExecutor(null);
        server.start();

        System.out.println("API server running on port " + PORT);
    }

    // ================= HANDLERS =================

    private void handleHealth(HttpExchange exchange) throws IOException {
        addCORS(exchange);
        if (handleOptions(exchange)) return;

        sendJson(exchange, 200,
            new JSONObject()
                .put("status", "ok")
                .put("mode", "API_ONLY")
                .toString()
        );
    }

    private void handleSearch(HttpExchange exchange) throws IOException {
        addCORS(exchange);
        if (handleOptions(exchange)) return;

        String query = getParam(exchange.getRequestURI(), "q");

        if (query == null || query.isBlank()) {
            sendError(exchange, 400, "Missing query parameter 'q'");
            return;
        }

        if (query.length() > MAX_QUERY_LENGTH) {
            sendError(exchange, 400, "Query too long");
            return;
        }

        long start = System.nanoTime();
        List<Integer> docIds = engine.search(query, TOP_K);
        long end = System.nanoTime();

        JSONArray results = new JSONArray();

        for (int docId : docIds) {
            Path file = docMap.get(docId);

            String title = "(missing document)";
            String preview = "";

            if (file != null) {
                try {
                    title = extractTitle(file);
                    preview = extractPreview(file);
                } catch (IOException ignored) {}
            }

            results.put(
                new JSONObject()
                    .put("docId", docId)
                    .put("title", title)
                    .put("preview", preview)
            );
        }

        sendJson(exchange, 200,
            new JSONObject()
                .put("query", query)
                .put("latencyMs",
                        String.format("%.2f", (end - start) / 1_000_000.0))
                .put("count", results.length())
                .put("results", results)
                .toString()
        );
    }

    private void handleAutocomplete(HttpExchange exchange) throws IOException {
        addCORS(exchange);
        if (handleOptions(exchange)) return;

        String prefix = getParam(exchange.getRequestURI(), "prefix");
        if (prefix == null) prefix = "";

        long start = System.nanoTime();
        List<String> suggestions = engine.autocomplete(prefix);
        long end = System.nanoTime();

        sendJson(exchange, 200,
            new JSONObject()
                .put("prefix", prefix)
                .put("latencyMs",
                        String.format("%.2f", (end - start) / 1_000_000.0))
                .put("suggestions", new JSONArray(suggestions))
                .toString()
        );
    }

    private void handleDoc(HttpExchange exchange) throws IOException {
        addCORS(exchange);
        if (handleOptions(exchange)) return;

        String idStr = getParam(exchange.getRequestURI(), "id");

        if (idStr == null) {
            sendError(exchange, 400, "Missing id");
            return;
        }

        int docId;
        try {
            docId = Integer.parseInt(idStr);
        } catch (NumberFormatException e) {
            sendError(exchange, 400, "Invalid id");
            return;
        }

        Path file = docMap.get(docId);
        if (file == null) {
            sendError(exchange, 404, "Document not found");
            return;
        }

        String text = Files.readString(file);
        if (text.length() > 2000) {
            text = text.substring(0, 2000) + "...";
        }

        sendJson(exchange, 200,
            new JSONObject()
                .put("docId", docId)
                .put("text", text)
                .toString()
        );
    }

    // ================= HELPERS =================

    private static boolean handleOptions(HttpExchange ex) throws IOException {
        if ("OPTIONS".equalsIgnoreCase(ex.getRequestMethod())) {
            ex.sendResponseHeaders(204, -1);
            return true;
        }
        return false;
    }

    private static void addCORS(HttpExchange ex) {
        ex.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        ex.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, OPTIONS");
        ex.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");
    }

    private static void sendJson(HttpExchange ex, int statusCode, String body)
            throws IOException {

        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set(
                "Content-Type", "application/json; charset=utf-8");
        ex.sendResponseHeaders(statusCode, bytes.length);

        try (OutputStream os = ex.getResponseBody()) {
            os.write(bytes);
        }
    }

    private static void sendError(HttpExchange ex, int code, String message)
            throws IOException {

        sendJson(ex, code,
            new JSONObject()
                .put("error", message)
                .put("code", code)
                .toString()
        );
    }

    private static String getParam(URI uri, String key) {
        String q = uri.getQuery();
        if (q == null) return null;

        for (String pair : q.split("&")) {
            String[] kv = pair.split("=", 2);
            if (kv.length == 2 && kv[0].equals(key)) {
                return URLDecoder.decode(kv[1], StandardCharsets.UTF_8);
            }
        }
        return null;
    }

    private String extractTitle(Path file) throws IOException {
        try (BufferedReader r = Files.newBufferedReader(file)) {
            String line = r.readLine();
            if (line == null || line.isBlank()) return "(empty document)";
            return truncateWords(line, TITLE_WORD_LIMIT);
        }
    }

    private static String truncateWords(String text, int max) {
        String[] words = text.trim().split("\\s+");
        if (words.length <= max) return text.trim();
        return String.join(" ",
                Arrays.copyOfRange(words, 0, max)) + "...";
    }

    private String extractPreview(Path file) throws IOException {
        StringBuilder sb = new StringBuilder();

        try (BufferedReader r = Files.newBufferedReader(file)) {
            String line;
            int lines = 0;

            while ((line = r.readLine()) != null && lines < PREVIEW_LINE_LIMIT) {
                line = line.trim();
                if (line.isEmpty()) continue;
                if (sb.length() > 0) sb.append(" ");
                sb.append(line);
                lines++;
            }
        }

        String preview = sb.toString().replaceAll("\\s+", " ");
        if (preview.length() > PREVIEW_CHAR_LIMIT) {
            preview = preview.substring(0, PREVIEW_CHAR_LIMIT) + "...";
        }

        return preview.isEmpty() ? "(no preview available)" : preview;
    }

    // ================= MAIN =================

    public static void main(String[] args) throws Exception {
        new DemoServer().start();
    }
}
