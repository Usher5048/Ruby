package ruby.systems.accounts;

import com.sun.net.httpserver.HttpServer;
import net.minecraft.util.Util;
import ruby.RubyClient;
import ruby.helpers.Http;
import ruby.systems.gui.Toasts;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

public final class MicrosoftLogin {
    /** Official Minecraft title id. Meteor's old app id is blocked, so Continue never returned a token. */
    private static final String CLIENT_ID = "00000000402b5328";
    private static final String SCOPE = "service::user.auth.xboxlive.com::MBI_SSL";
    private static final String TOKEN_URL = "https://login.live.com/oauth20_token.srf";

    private static volatile HttpServer server;
    private static volatile int attempt;

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
        int id = ++MicrosoftLogin.attempt;
        MicrosoftLogin.stopServer();
        Util.ioPool().execute(() -> MicrosoftLogin.runDeviceLogin(id, onResult));
    }

    private static void runDeviceLogin(int id, Consumer<String> onResult) {
        Http.Result started = Http.post("https://login.live.com/oauth20_connect.srf")
                .bodyForm("client_id=" + CLIENT_ID
                        + "&scope=" + MicrosoftLogin.form(SCOPE)
                        + "&response_type=device_code")
                .send();
        if (id != MicrosoftLogin.attempt) return;
        if (started == null || !started.ok()) {
            MicrosoftLogin.logHttp("device code", started);
            MicrosoftLogin.finish(id, onResult, null);
            return;
        }

        DeviceCodeResponse code = Http.json(started.body, DeviceCodeResponse.class);
        if (code == null || code.device_code == null || code.user_code == null || code.verification_uri == null) {
            MicrosoftLogin.finish(id, onResult, null);
            return;
        }

        String page = code.verification_uri + "?otc=" + MicrosoftLogin.form(code.user_code);
        boolean opened = MicrosoftLogin.openBrowser(page);
        RubyClient.client.execute(() -> {
            if (id != MicrosoftLogin.attempt) return;
            Toasts.toast(opened ? "Finish sign-in in your browser" : "Could not open the browser")
                    .icon(opened ? "users" : "x")
                    .detail(code.user_code)
                    .seconds(12f)
                    .show();
        });
        if (!opened) RubyClient.LOGGER.info("Microsoft sign-in page: {}", page);

        long deadline = System.nanoTime() + Math.max(30, code.expires_in) * 1_000_000_000L;
        int waitMs = Math.max(1, code.interval) * 1000;
        while (id == MicrosoftLogin.attempt && System.nanoTime() < deadline) {
            Http.Result polled = Http.post(TOKEN_URL)
                    .bodyForm("client_id=" + CLIENT_ID
                            + "&grant_type=device_code"
                            + "&device_code=" + MicrosoftLogin.form(code.device_code))
                    .send();
            if (id != MicrosoftLogin.attempt) return;
            if (polled != null && polled.ok()) {
                AuthTokenResponse token = Http.json(polled.body, AuthTokenResponse.class);
                MicrosoftLogin.finish(id, onResult, token == null ? null : token.refresh_token);
                return;
            }

            String error = MicrosoftLogin.errorCode(polled);
            if ("authorization_pending".equals(error)) {
                if (!MicrosoftLogin.pause(id, waitMs)) return;
                continue;
            }
            if ("slow_down".equals(error)) {
                waitMs += 5000;
                if (!MicrosoftLogin.pause(id, waitMs)) return;
                continue;
            }
            MicrosoftLogin.logHttp("device token", polled);
            MicrosoftLogin.finish(id, onResult, null);
            return;
        }
        if (id == MicrosoftLogin.attempt) MicrosoftLogin.finish(id, onResult, null);
    }

    private static void finish(int id, Consumer<String> onResult, String refreshToken) {
        if (id != MicrosoftLogin.attempt) return;
        onResult.accept(refreshToken);
    }

    private static boolean pause(int id, int waitMs) {
        try {
            Thread.sleep(waitMs);
            return id == MicrosoftLogin.attempt;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    private static String errorCode(Http.Result result) {
        if (result == null || result.body.isBlank()) return null;
        ErrorBody error = Http.json(result.body, ErrorBody.class);
        return error == null ? null : error.error;
    }

    private static void logHttp(String step, Http.Result result) {
        if (result == null) {
            RubyClient.LOGGER.warn("Microsoft login failed during {}", step);
            return;
        }
        String body = result.body.replace('\n', ' ');
        if (body.length() > 240) body = body.substring(0, 240);
        RubyClient.LOGGER.warn("Microsoft login failed during {} (HTTP {}): {}", step, result.status, body);
    }

    private static boolean openBrowser(String url) {
        if (MicrosoftLogin.spawn("xdg-open", url) || MicrosoftLogin.spawn("gio", "open", url)) return true;
        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(URI.create(url));
                return true;
            }
        } catch (Exception e) {
            RubyClient.LOGGER.warn("Failed to open the Microsoft login page", e);
        }
        return false;
    }

    private static boolean spawn(String... command) {
        try {
            new ProcessBuilder(command)
                    .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                    .redirectError(ProcessBuilder.Redirect.DISCARD)
                    .start();
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    public static LoginData login(String refreshToken) {
        return MicrosoftLogin.login(refreshToken, false);
    }

    public static LoginData login(String refreshToken, boolean fast) {
        AuthTokenResponse token = Http.post(TOKEN_URL)
                .bodyForm("client_id=" + CLIENT_ID
                        + "&scope=" + MicrosoftLogin.form(SCOPE)
                        + "&grant_type=refresh_token"
                        + "&refresh_token=" + MicrosoftLogin.form(refreshToken))
                .sendJson(AuthTokenResponse.class);

        if (token == null || token.access_token == null) return MicrosoftLogin.fail("refresh");

        String accessToken = token.access_token;
        refreshToken = token.refresh_token;

        XblXstsResponse xbl = Http.post("https://user.auth.xboxlive.com/user/authenticate")
                .bodyJson("{\"Properties\":{\"AuthMethod\":\"RPS\",\"SiteName\":\"user.auth.xboxlive.com\",\"RpsTicket\":"
                        + Http.quote(accessToken) + "},\"RelyingParty\":\"http://auth.xboxlive.com\",\"TokenType\":\"JWT\"}")
                .sendJson(XblXstsResponse.class);
        if (xbl == null) return MicrosoftLogin.fail("xbox user");

        XblXstsResponse xsts = Http.post("https://xsts.auth.xboxlive.com/xsts/authorize")
                .bodyJson("{\"Properties\":{\"SandboxId\":\"RETAIL\",\"UserTokens\":[" + Http.quote(xbl.Token)
                        + "]},\"RelyingParty\":\"rp://api.minecraftservices.com/\",\"TokenType\":\"JWT\"}")
                .sendJson(XblXstsResponse.class);
        if (xsts == null) return MicrosoftLogin.fail("xbox xsts");

        String uhs = MicrosoftLogin.userHash(xsts);
        if (uhs == null) uhs = MicrosoftLogin.userHash(xbl);
        if (uhs == null || xsts.Token == null) return MicrosoftLogin.fail("xbox user hash");

        McResponse mc = Http.post("https://api.minecraftservices.com/authentication/login_with_xbox")
                .bodyJson("{\"identityToken\":" + Http.quote("XBL3.0 x=" + uhs + ";" + xsts.Token) + "}")
                .sendJson(McResponse.class);
        if (mc == null || mc.access_token == null) return MicrosoftLogin.fail("minecraft token");

        if (fast) {
            LoginData data = new LoginData();
            data.mcToken = mc.access_token;
            data.newRefreshToken = refreshToken;
            return data;
        }

        ProfileResponse profile = Http.get("https://api.minecraftservices.com/minecraft/profile")
                .bearer(mc.access_token)
                .sendJson(ProfileResponse.class);
        if (profile == null || profile.name == null || profile.name.isEmpty() || profile.id == null) {
            return MicrosoftLogin.fail("minecraft profile");
        }

        LoginData data = new LoginData();
        data.mcToken = mc.access_token;
        data.newRefreshToken = refreshToken;
        data.uuid = profile.id;
        data.username = profile.name;
        return data;
    }

    private static LoginData fail(String step) {
        RubyClient.LOGGER.warn("Microsoft login stopped at {}", step);
        return new LoginData();
    }

    public static void stopServer() {
        if (MicrosoftLogin.server == null) return;
        MicrosoftLogin.server.stop(0);
        MicrosoftLogin.server = null;
    }

    private static String form(String value) {
        return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
    }

    private static String userHash(XblXstsResponse response) {
        if (response == null || response.DisplayClaims == null || response.DisplayClaims.xui == null) return null;
        if (response.DisplayClaims.xui.length == 0) return null;
        String hash = response.DisplayClaims.xui[0].uhs;
        return hash == null || hash.isEmpty() ? null : hash;
    }

    private static class DeviceCodeResponse {
        String device_code;
        String user_code;
        String verification_uri;
        int expires_in;
        int interval;
    }

    private static class ErrorBody {
        String error;
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

    private static class ProfileResponse {
        String id;
        String name;
    }
}
