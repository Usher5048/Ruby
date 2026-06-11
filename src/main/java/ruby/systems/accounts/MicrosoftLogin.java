package ruby.systems.accounts;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import net.minecraft.util.Util;
import ruby.RubyClient;
import ruby.helpers.Http;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public final class MicrosoftLogin {
    private static final String CLIENT_ID = "4673b348-3efa-4f6a-bbb6-34e141cdc638";
    private static final int PORT = 9675;

    private static volatile HttpServer server;
    private static volatile Consumer<String> callback;

    private MicrosoftLogin() {}

    public static class LoginData {
        public String mcToken;
        public String newRefreshToken;
        public String uuid;
        public String username;

        public boolean isGood() {
            return this.mcToken != null;
        }
    }

    public static void requestRefreshToken(Consumer<String> onResult) {
        MicrosoftLogin.callback = onResult;
        MicrosoftLogin.startServer();
        String url = "https://login.live.com/oauth20_authorize.srf?client_id=" + CLIENT_ID
                + "&response_type=code&redirect_uri=http://127.0.0.1:" + PORT
                + "&scope=XboxLive.signin%20offline_access&prompt=select_account";
        Util.getOperatingSystem().open(url);
    }

    public static LoginData login(String refreshToken) {
        AuthTokenResponse token = Http.post("https://login.live.com/oauth20_token.srf")
                .bodyForm("client_id=" + CLIENT_ID
                        + "&refresh_token=" + refreshToken
                        + "&grant_type=refresh_token"
                        + "&redirect_uri=http://127.0.0.1:" + PORT)
                .sendJson(AuthTokenResponse.class);

        if (token == null) return new LoginData();

        String accessToken = token.access_token;
        refreshToken = token.refresh_token;

        XblXstsResponse xbl = Http.post("https://user.auth.xboxlive.com/user/authenticate")
                .bodyJson("{\"Properties\":{\"AuthMethod\":\"RPS\",\"SiteName\":\"user.auth.xboxlive.com\",\"RpsTicket\":\"d="
                        + accessToken + "\"},\"RelyingParty\":\"http://auth.xboxlive.com\",\"TokenType\":\"JWT\"}")
                .sendJson(XblXstsResponse.class);
        if (xbl == null) return new LoginData();

        XblXstsResponse xsts = Http.post("https://xsts.auth.xboxlive.com/xsts/authorize")
                .bodyJson("{\"Properties\":{\"SandboxId\":\"RETAIL\",\"UserTokens\":[\"" + xbl.Token
                        + "\"]},\"RelyingParty\":\"rp://api.minecraftservices.com/\",\"TokenType\":\"JWT\"}")
                .sendJson(XblXstsResponse.class);
        if (xsts == null) return new LoginData();

        McResponse mc = Http.post("https://api.minecraftservices.com/authentication/login_with_xbox")
                .bodyJson("{\"identityToken\":\"XBL3.0 x=" + xbl.DisplayClaims.xui[0].uhs + ";" + xsts.Token + "\"}")
                .sendJson(McResponse.class);
        if (mc == null) return new LoginData();

        GameOwnershipResponse ownership = Http.get("https://api.minecraftservices.com/entitlements/mcstore")
                .bearer(mc.access_token)
                .sendJson(GameOwnershipResponse.class);
        if (ownership == null || !ownership.hasGameOwnership()) return new LoginData();

        ProfileResponse profile = Http.get("https://api.minecraftservices.com/minecraft/profile")
                .bearer(mc.access_token)
                .sendJson(ProfileResponse.class);
        if (profile == null) return new LoginData();

        LoginData data = new LoginData();
        data.mcToken = mc.access_token;
        data.newRefreshToken = refreshToken;
        data.uuid = profile.id;
        data.username = profile.name;
        return data;
    }

    public static void stopServer() {
        if (MicrosoftLogin.server == null) return;
        MicrosoftLogin.server.stop(0);
        MicrosoftLogin.server = null;
        MicrosoftLogin.callback = null;
    }

    private static void startServer() {
        if (MicrosoftLogin.server != null) return;

        try {
            MicrosoftLogin.server = HttpServer.create(new InetSocketAddress("127.0.0.1", PORT), 0);
            MicrosoftLogin.server.createContext("/", MicrosoftLogin::handleRequest);
            MicrosoftLogin.server.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
            MicrosoftLogin.server.start();
        } catch (IOException e) {
            RubyClient.LOGGER.error("Failed to start Microsoft login server", e);
            MicrosoftLogin.stopServer();
        }
    }

    private static void handleRequest(HttpExchange exchange) throws IOException {
        if ("GET".equals(exchange.getRequestMethod())) {
            boolean ok = false;
            for (String[] pair : MicrosoftLogin.parseQuery(exchange.getRequestURI().getRawQuery())) {
                if ("code".equals(pair[0])) {
                    MicrosoftLogin.handleCode(pair[1]);
                    ok = true;
                    break;
                }
            }

            MicrosoftLogin.writeText(exchange, ok
                    ? "You may now close this page."
                    : "Cannot authenticate.");
            if (!ok && MicrosoftLogin.callback != null) {
                MicrosoftLogin.callback.accept(null);
            }
        }

        MicrosoftLogin.stopServer();
    }

    private static void handleCode(String code) {
        AuthTokenResponse token = Http.post("https://login.live.com/oauth20_token.srf")
                .bodyForm("client_id=" + CLIENT_ID
                        + "&code=" + code
                        + "&grant_type=authorization_code"
                        + "&redirect_uri=http://127.0.0.1:" + PORT)
                .sendJson(AuthTokenResponse.class);

        if (MicrosoftLogin.callback == null) return;
        MicrosoftLogin.callback.accept(token == null ? null : token.refresh_token);
    }

    private static void writeText(HttpExchange exchange, String text) throws IOException {
        byte[] body = text.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(200, body.length);
        try (var out = exchange.getResponseBody()) {
            out.write(body);
        }
    }

    private static List<String[]> parseQuery(String query) {
        List<String[]> pairs = new ArrayList<>();
        if (query == null) return pairs;

        for (String part : query.split("&")) {
            int eq = part.indexOf('=');
            if (eq < 0) pairs.add(new String[] { part, "" });
            else pairs.add(new String[] { part.substring(0, eq), part.substring(eq + 1) });
        }
        return pairs;
    }

    private static class AuthTokenResponse {
        String access_token;
        String refresh_token;
    }

    private static class XblXstsResponse {
        String Token;
        DisplayClaims DisplayClaims;

        static class DisplayClaims {
            Claim[] xui;

            static class Claim {
                String uhs;
            }
        }
    }

    private static class McResponse {
        String access_token;
    }

    private static class GameOwnershipResponse {
        Item[] items;

        boolean hasGameOwnership() {
            boolean product = false;
            boolean game = false;
            if (this.items == null) return false;
            for (Item item : this.items) {
                if ("product_minecraft".equals(item.name)) product = true;
                else if ("game_minecraft".equals(item.name)) game = true;
            }
            return product && game;
        }

        static class Item {
            String name;
        }
    }

    private static class ProfileResponse {
        String id;
        String name;
    }
}
