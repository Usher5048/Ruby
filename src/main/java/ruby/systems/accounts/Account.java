package ruby.systems.accounts;

import com.mojang.authlib.minecraft.UserApiService;
import com.mojang.authlib.yggdrasil.ServicesKeyType;
import com.mojang.authlib.yggdrasil.YggdrasilAuthenticationService;
import net.minecraft.client.network.SocialInteractionsManager;
import net.minecraft.client.session.ProfileKeys;
import net.minecraft.client.session.Session;
import net.minecraft.client.session.report.AbuseReportContext;
import net.minecraft.client.session.report.ReporterEnvironment;
import net.minecraft.client.texture.PlayerSkinProvider;
import net.minecraft.client.texture.PlayerSkinTextureDownloader;
import net.minecraft.util.ApiServices;
import net.minecraft.network.encryption.SignatureVerifier;
import net.minecraft.util.Util;
import ruby.RubyClient;
import ruby.mixin.MinecraftClientSessionAccessor;
import ruby.systems.accounts.types.CrackedAccount;
import ruby.systems.accounts.types.MicrosoftAccount;
import ruby.systems.accounts.types.SessionAccount;

import java.nio.file.Path;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public abstract class Account {
    protected final AccountType type;
    protected String name;
    protected final AccountCache cache = new AccountCache();

    protected Account(AccountType type, String name) {
        this.type = type;
        this.name = name;
    }

    public abstract boolean fetchInfo();

    /** Prepares credentials for auto-login; may skip redundant network calls when cache is valid. */
    public boolean prepareAutoLogin() {
        return this.fetchInfo();
    }

    public boolean hasCachedProfile() {
        return !this.cache.username.isEmpty() && !this.cache.uuid.isEmpty();
    }

    public boolean login() {
        YggdrasilAuthenticationService authService =
                new YggdrasilAuthenticationService(RubyClient.client.getNetworkProxy());
        Account.applyLoginEnvironment(authService);
        return true;
    }

    public String getUsername() {
        return this.cache.username.isEmpty() ? this.name : this.cache.username;
    }

    public AccountType getType() {
        return this.type;
    }

    public AccountCache getCache() {
        return this.cache;
    }

    public String getName() {
        return this.name;
    }

    public static void setSession(Session session) {
        MinecraftClientSessionAccessor mc = (MinecraftClientSessionAccessor) RubyClient.client;
        mc.ruby$setSession(session);

        YggdrasilAuthenticationService authService =
                new YggdrasilAuthenticationService(RubyClient.client.getNetworkProxy());
        UserApiService apiService = authService.createUserApiService(session.getAccessToken());

        mc.ruby$setUserApiService(apiService);
        mc.ruby$setSocialInteractionsManager(new SocialInteractionsManager(RubyClient.client, apiService));
        mc.ruby$setProfileKeys(ProfileKeys.create(apiService, session, RubyClient.client.runDirectory.toPath()));
        mc.ruby$setAbuseReportContext(AbuseReportContext.create(ReporterEnvironment.ofIntegratedServer(), apiService));

        UUID uuid = session.getUuidOrNull();
        mc.ruby$setGameProfileFuture(CompletableFuture.supplyAsync(
                () -> RubyClient.client.getApiServices().sessionService().fetchProfile(uuid, true),
                Util.getIoWorkerExecutor()
        ));
    }

    public static void applyLoginEnvironment(YggdrasilAuthenticationService authService) {
        MinecraftClientSessionAccessor mc = (MinecraftClientSessionAccessor) RubyClient.client;
        SignatureVerifier.create(authService.getServicesKeySet(), ServicesKeyType.PROFILE_KEY);

        Path skinCacheDirectory = RubyClient.client.runDirectory.toPath();

        mc.ruby$setApiServices(ApiServices.create(authService, RubyClient.client.runDirectory));
        mc.ruby$setSkinProvider(new PlayerSkinProvider(
                skinCacheDirectory,
                RubyClient.client.getApiServices(),
                new PlayerSkinTextureDownloader(
                        RubyClient.client.getNetworkProxy(),
                        RubyClient.client.getTextureManager(),
                        RubyClient.client
                ),
                Util.getMainWorkerExecutor()
        ));
    }

    public AccountStorage toStorage() {
        AccountStorage storage = new AccountStorage();
        storage.type = this.type.name();
        storage.name = this.name;
        storage.username = this.cache.username;
        storage.uuid = this.cache.uuid;
        return storage;
    }

    public void applyStorage(AccountStorage storage) {
        this.name = storage.name;
        this.cache.username = storage.username == null ? "" : storage.username;
        this.cache.uuid = storage.uuid == null ? "" : storage.uuid;
    }

    public static Account fromStorage(AccountStorage storage) {
        Account account = switch (AccountType.valueOf(storage.type)) {
            case Cracked -> new CrackedAccount(storage.name);
            case Microsoft -> new MicrosoftAccount(storage.name);
            case Session -> new SessionAccount(storage.name, storage.token);
        };
        account.applyStorage(storage);
        return account;
    }
}
