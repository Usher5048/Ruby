package ruby.systems.accounts.types;

import ruby.systems.accounts.Account;
import ruby.systems.accounts.AccountType;

import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.client.User;

public class CrackedAccount extends Account {
    public CrackedAccount(String name) {
        super(AccountType.Cracked, name);
    }

    @Override
    public boolean fetchInfo() {
        this.cache.username = this.name;
        return true;
    }

    @Override
    public boolean prepareAutoLogin() {
        this.cache.username = this.name;
        return true;
    }

    @Override
    public boolean login() {
        if (!super.login()) return false;

        UUID uuid = UUID.nameUUIDFromBytes(
                ("OfflinePlayer:" + this.name).getBytes(StandardCharsets.UTF_8));
        Account.setSession(new User(this.name, uuid, "", Optional.empty(), Optional.empty()));
        return true;
    }
}
