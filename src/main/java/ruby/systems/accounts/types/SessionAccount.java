package ruby.systems.accounts.types;

import com.mojang.util.UndashedUuid;
import net.minecraft.client.session.Session;
import ruby.helpers.Http;
import ruby.systems.accounts.Account;
import ruby.systems.accounts.AccountStorage;
import ruby.systems.accounts.AccountType;

import java.util.Optional;

public class SessionAccount extends Account {
    private String accessToken;

    public SessionAccount(String label, String accessToken) {
        super(AccountType.Session, label);
        this.accessToken = accessToken;
    }

    @Override
    public boolean fetchInfo() {
        if (this.accessToken == null || this.accessToken.isBlank()) return false;

        ProfileResponse profile = Http.get("https://api.minecraftservices.com/minecraft/profile")
                .bearer(this.accessToken)
                .sendJson(ProfileResponse.class);

        if (profile == null || profile.id == null || profile.name == null) return false;

        this.cache.username = profile.name;
        this.cache.uuid = profile.id;
        return true;
    }

    @Override
    public boolean prepareAutoLogin() {
        if (this.accessToken == null || this.accessToken.isBlank()) return false;
        if (this.hasCachedProfile()) return true;
        return this.fetchInfo();
    }

    @Override
    public boolean login() {
        if (this.accessToken == null || this.accessToken.isBlank()) return false;
        if (!super.login()) return false;

        Account.setSession(new Session(
                this.cache.username,
                UndashedUuid.fromStringLenient(this.cache.uuid),
                this.accessToken,
                Optional.empty(),
                Optional.empty()
        ));
        return true;
    }

    @Override
    public AccountStorage toStorage() {
        AccountStorage storage = super.toStorage();
        storage.token = this.accessToken;
        return storage;
    }

    @Override
    public void applyStorage(AccountStorage storage) {
        super.applyStorage(storage);
        if (storage.token != null) this.accessToken = storage.token;
    }

    private static class ProfileResponse {
        String id;
        String name;
    }
}
