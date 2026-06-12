package ruby.systems.accounts;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.minecraft.util.Util;
import ruby.RubyClient;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public final class AccountsManager implements Iterable<Account> {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Type LIST_TYPE = new TypeToken<List<AccountStorage>>() {}.getType();

    private static final List<Account> accounts = new ArrayList<>();
    private static String lastAccountName = "";

    private AccountsManager() {}

    public static List<Account> getAccounts() {
        return AccountsManager.accounts;
    }

    public static void add(Account account) {
        AccountsManager.accounts.add(account);
        AccountsManager.save();
    }

    public static void remove(Account account) {
        if(AccountsManager.accounts.remove(account)) {
            if(account.getName().equals(AccountsManager.lastAccountName))
                AccountsManager.lastAccountName = "";
            AccountsManager.save();
            AccountsManager.saveLastUsed();
        }
    }

    public static boolean isActive(Account account) {
        return RubyClient.client.getSession().getUsername().equalsIgnoreCase(account.getUsername());
    }

    public static void markLastUsed(Account account) {
        if(account == null) return;
        AccountsManager.lastAccountName = account.getName();
        AccountsManager.saveLastUsed();
    }

    public static void loginLast() {
        if(AccountsManager.lastAccountName == null || AccountsManager.lastAccountName.isEmpty()) return;

        Account target = null;
        for(Account account : AccountsManager.accounts) {
            if(account.getName().equals(AccountsManager.lastAccountName)) {
                target = account;
                break;
            }
        }
        if(target == null || AccountsManager.isActive(target)) return;

        final Account account = target;
        Util.getIoWorkerExecutor().execute(() -> {
            if(account.fetchInfo() && account.login())
                RubyClient.LOGGER.info("Auto-logged in as {}", account.getUsername());
            else
                RubyClient.LOGGER.warn("Failed to auto-login as {}", account.getName());
        });
    }

    public static void load() {
        AccountsManager.accounts.clear();
        AccountsManager.loadLastUsed();

        File file = AccountsManager.accountsFile();
        if(!file.isFile()) return;

        try(FileReader reader = new FileReader(file)) {
            List<AccountStorage> stored = AccountsManager.GSON.fromJson(reader, AccountsManager.LIST_TYPE);
            if(stored == null) return;

            for(AccountStorage storage : stored) {
                if(storage == null || storage.type == null) continue;
                try {
                    AccountsManager.accounts.add(Account.fromStorage(storage));
                } catch(Exception e) {
                    RubyClient.LOGGER.warn("Skipped invalid account entry", e);
                }
            }
        } catch(Exception e) {
            RubyClient.LOGGER.error("Failed to load accounts", e);
        }
    }

    public static void save() {
        File file = AccountsManager.accountsFile();
        file.getParentFile().mkdirs();

        List<AccountStorage> stored = new ArrayList<>();
        for(Account account : AccountsManager.accounts)
            stored.add(account.toStorage());

        try(FileWriter writer = new FileWriter(file)) {
            AccountsManager.GSON.toJson(stored, writer);
        } catch(Exception e) {
            RubyClient.LOGGER.error("Failed to save accounts", e);
        }
    }

    private static void loadLastUsed() {
        AccountsManager.lastAccountName = "";
        File file = AccountsManager.lastAccountFile();
        if(!file.isFile()) return;

        try(FileReader reader = new FileReader(file)) {
            LastAccountStorage storage = AccountsManager.GSON.fromJson(reader, LastAccountStorage.class);
            if(storage != null && storage.name != null)
                AccountsManager.lastAccountName = storage.name;
        } catch(Exception e) {
            RubyClient.LOGGER.warn("Failed to load last account", e);
        }
    }

    private static void saveLastUsed() {
        File file = AccountsManager.lastAccountFile();
        file.getParentFile().mkdirs();

        LastAccountStorage storage = new LastAccountStorage();
        storage.name = AccountsManager.lastAccountName;

        try(FileWriter writer = new FileWriter(file)) {
            AccountsManager.GSON.toJson(storage, writer);
        } catch(Exception e) {
            RubyClient.LOGGER.error("Failed to save last account", e);
        }
    }

    private static File accountsFile() {
        return new File(RubyClient.client.runDirectory, "config/ruby/accounts.json");
    }

    private static File lastAccountFile() {
        return new File(RubyClient.client.runDirectory, "config/ruby/last_account.json");
    }

    private static class LastAccountStorage {
        String name;
    }

    @Override
    public Iterator<Account> iterator() {
        return AccountsManager.accounts.iterator();
    }
}
