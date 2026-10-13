package ruby.systems.accounts;

import net.minecraft.util.Util;
import ruby.RubyClient;
import ruby.systems.config.ConfigManager;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public final class AccountsManager implements Iterable<Account> {

    private static final List<Account> accounts = new ArrayList<>();
    private static String lastAccountName = "";

    private AccountsManager() {}

    public static List<Account> getAccounts() {
        return AccountsManager.accounts;
    }

    public static void add(Account account) {
        for(Account other : AccountsManager.accounts) {
            if(account.getType() != other.getType()) continue;
            if(!account.getUsername().equals(other.getUsername())) continue;

            AccountsManager.accounts.remove(other);
            break;
        }

        AccountsManager.accounts.add(account);
    }

    public static void remove(Account account) {
        if(AccountsManager.accounts.remove(account)) {
            if(account.getUsername().equals(AccountsManager.lastAccountName))
                AccountsManager.lastAccountName = "";
        }
    }

    public static boolean isActive(Account account) {
        return RubyClient.client.getUser().getName().equalsIgnoreCase(account.getUsername());
    }

    public static void markLastUsed(Account account) {
        if(account == null) return;
        AccountsManager.lastAccountName = account.getUsername();
    }

    public static void loginLast() {
        if(AccountsManager.lastAccountName == null || AccountsManager.lastAccountName.isEmpty()) return;

        Account target = null;
        for(Account account : AccountsManager.accounts) {
            if(account.getUsername().equals(AccountsManager.lastAccountName)) {
                target = account;
                break;
            }
        }
        if(target == null || AccountsManager.isActive(target)) return;

        final Account account = target;
        Runnable apply = () -> {
            if(account.login()) {
                AccountsManager.save();
                RubyClient.LOGGER.info("Auto-logged in as {}", account.getUsername());
            } else {
                RubyClient.LOGGER.warn("Failed to auto-login as {}", account.getName());
            }
        };

        if(account.getType() == AccountType.Cracked) {
            if(account.prepareAutoLogin()) apply.run();
            else RubyClient.LOGGER.warn("Failed to auto-login as {}", account.getName());
            return;
        }

        if(account.getType() == AccountType.Session && account.hasCachedProfile()) {
            if(account.prepareAutoLogin()) apply.run();
            else RubyClient.LOGGER.warn("Failed to auto-login as {}", account.getName());
            return;
        }

        Util.ioPool().execute(() -> {
            if(!account.prepareAutoLogin()) {
                RubyClient.LOGGER.warn("Failed to auto-login as {}", account.getName());
                return;
            }
            RubyClient.client.execute(apply);
        });
    }

    public static void load() {
        File file = AccountsManager.file();
        if (!file.isFile()) return;

        try {
            ByteArrayInputStream in = new ByteArrayInputStream(Files.readAllBytes(file.toPath()));
            if (in.read() != 1) return;
            AccountsManager.lastAccountName = ConfigManager.readUtf8(in);
            int count = ConfigManager.readShort(in);
            AccountsManager.accounts.clear();
            for (int i = 0; i < count; i++) {
                AccountStorage storage = new AccountStorage();
                storage.type = ConfigManager.readUtf8(in);
                storage.name = ConfigManager.readUtf8(in);
                storage.username = ConfigManager.readUtf8(in);
                storage.uuid = ConfigManager.readUtf8(in);
                storage.token = ConfigManager.readUtf8(in);
                AccountsManager.add(Account.fromStorage(storage));
            }
        } catch (Exception e) {
            RubyClient.LOGGER.warn("Failed to load accounts", e);
        }
    }

    public static void save() {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            out.write(1);
            ConfigManager.writeUtf8(out, AccountsManager.lastAccountName);
            ConfigManager.writeShort(out, AccountsManager.accounts.size());
            for (Account account : AccountsManager.accounts) {
                AccountStorage storage = account.toStorage();
                ConfigManager.writeUtf8(out, storage.type);
                ConfigManager.writeUtf8(out, storage.name);
                ConfigManager.writeUtf8(out, storage.username);
                ConfigManager.writeUtf8(out, storage.uuid);
                ConfigManager.writeUtf8(out, storage.token);
            }
            File file = AccountsManager.file();
            File parent = file.getParentFile();
            if (parent != null) parent.mkdirs();
            Files.write(file.toPath(), out.toByteArray());
        } catch (Exception e) {
            RubyClient.LOGGER.warn("Failed to save accounts", e);
        }
    }

    private static File file() {
        return new File(RubyClient.client.gameDirectory, "config/" + RubyClient.MOD_ID + "-accounts");
    }

    public static void loadStoredAccounts(List<AccountStorage> accounts) {
        AccountsManager.accounts.clear();
        for(AccountStorage storage : accounts)
            AccountsManager.add(Account.fromStorage(storage));
    }

    public static List<AccountStorage> getStoredAccounts() {
        return AccountsManager.accounts.stream()
                .map(Account::toStorage)
                .toList();
    }

    public static void setLastAccount(String name) {
        AccountsManager.lastAccountName = name;
    }
    public static String getLastAccount() {
        return AccountsManager.lastAccountName;
    }

    @Override
    public Iterator<Account> iterator() {
        return AccountsManager.accounts.iterator();
    }
}
