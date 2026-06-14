package ruby.systems.accounts.types;

import com.mojang.util.UndashedUuid;
import net.minecraft.client.session.Session;
import ruby.systems.accounts.Account;
import ruby.systems.accounts.AccountType;
import ruby.systems.accounts.MicrosoftLogin;

import java.util.Optional;

public class MicrosoftAccount extends Account {
    private String accessToken;

    public MicrosoftAccount(String refreshToken) {
        super(AccountType.Microsoft, refreshToken);
    }

    @Override
    public boolean fetchInfo() {
        this.accessToken = this.authenticate(false);
        return this.accessToken != null;
    }

    @Override
    public boolean prepareAutoLogin() {
        this.accessToken = this.authenticate(this.hasCachedProfile());
        return this.accessToken != null;
    }

    @Override
    public boolean login() {
        if (this.accessToken == null) return false;
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

    private String authenticate(boolean fast) {
        MicrosoftLogin.LoginData data = MicrosoftLogin.login(this.name, fast);
        if (!data.isGood()) return null;

        this.name = data.newRefreshToken;
        if (!fast || !this.hasCachedProfile()) {
            this.cache.username = data.username;
            this.cache.uuid = data.uuid;
        }
        return data.mcToken;
    }

    @Override
    public void applyStorage(ruby.systems.accounts.AccountStorage storage) {
        super.applyStorage(storage);
        if (storage.name != null) this.name = storage.name;
    }
}
