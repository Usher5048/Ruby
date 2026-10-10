package ruby.systems.accounts;

import com.mojang.authlib.minecraft.UserApiService;
import com.mojang.authlib.services.FriendsService;
import com.mojang.authlib.services.MinecraftServicesDiscoveryService;
import com.mojang.authlib.services.ServicesKeyType;
import net.minecraft.client.User;
import net.minecraft.client.gui.screens.social.PlayerSocialManager;
import net.minecraft.client.gui.screens.social.RemoteFriendListUpdateHandler;
import net.minecraft.client.multiplayer.ProfileKeyPairManager;
import net.minecraft.client.multiplayer.chat.report.ReportEnvironment;
import net.minecraft.client.multiplayer.chat.report.ReportingContext;
import net.minecraft.client.renderer.texture.SkinTextureDownloader;
import net.minecraft.client.resources.SkinManager;
import net.minecraft.server.Services;
import net.minecraft.util.SignatureValidator;
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
        MinecraftServicesDiscoveryService authService =
                MinecraftServicesDiscoveryService.create(RubyClient.client.getProxy());
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

    public static void setSession(User session) {
        MinecraftClientSessionAccessor mc = (MinecraftClientSessionAccessor) RubyClient.client;
        mc.ruby$setSession(session);

        MinecraftServicesDiscoveryService authService =
                MinecraftServicesDiscoveryService.create(RubyClient.client.getProxy());
        UserApiService apiService = authService.createUserApiService(session.getAccessToken());

        FriendsService fs = authService.createFriendsService(session.getAccessToken());
        RemoteFriendListUpdateHandler rfluh = new RemoteFriendListUpdateHandler(fs, RubyClient.client);

        mc.ruby$setUserApiService(apiService);
        mc.ruby$setSocialInteractionsManager(new PlayerSocialManager(RubyClient.client, apiService, fs, rfluh));
        mc.ruby$setProfileKeys(ProfileKeyPairManager.create(apiService, session, RubyClient.client.gameDirectory.toPath()));
        mc.ruby$setAbuseReportContext(ReportingContext.create(ReportEnvironment.local(), apiService));

        UUID uuid = session.getProfileId();
        mc.ruby$setGameProfileFuture(CompletableFuture.supplyAsync(
                () -> RubyClient.client.services().sessionService().fetchProfile(uuid, true),
                Util.ioPool()
        ));
    }

    public static void applyLoginEnvironment(MinecraftServicesDiscoveryService authService) {
        MinecraftClientSessionAccessor mc = (MinecraftClientSessionAccessor) RubyClient.client;
        SignatureValidator.from(authService.getServicesKeySet(), ServicesKeyType.PROFILE_KEY);

        Path skinCacheDirectory = RubyClient.client.gameDirectory.toPath();

        mc.ruby$setApiServices(Services.create(authService, RubyClient.client.gameDirectory));
        mc.ruby$setSkinProvider(new SkinManager(
                skinCacheDirectory,
                RubyClient.client.services(),
                new SkinTextureDownloader(
                        RubyClient.client.getProxy(),
                        RubyClient.client.getTextureManager(),
                        RubyClient.client
                ),
                Util.backgroundExecutor()
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
