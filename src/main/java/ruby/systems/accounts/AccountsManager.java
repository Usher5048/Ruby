package ruby.systems.accounts;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
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

    private AccountsManager() {}

    public static List<Account> getAccounts() {
        return AccountsManager.accounts;
    }

    public static void add(Account account) {
        AccountsManager.accounts.add(account);
        AccountsManager.save();
    }

    public static void remove(Account account) {
        if (AccountsManager.accounts.remove(account)) {
            AccountsManager.save();
        }
    }

    public static boolean isActive(Account account) {
        return RubyClient.client.getSession().getUsername().equalsIgnoreCase(account.getUsername());
    }

    public static void load() {
        AccountsManager.accounts.clear();
        File file = AccountsManager.accountsFile();
        if (!file.isFile()) return;

        try (FileReader reader = new FileReader(file)) {
            List<AccountStorage> stored = AccountsManager.GSON.fromJson(reader, AccountsManager.LIST_TYPE);
            if (stored == null) return;

            for (AccountStorage storage : stored) {
                if (storage == null || storage.type == null) continue;
                try {
                    AccountsManager.accounts.add(Account.fromStorage(storage));
                } catch (Exception e) {
                    RubyClient.LOGGER.warn("Skipped invalid account entry", e);
                }
            }
        } catch (Exception e) {
            RubyClient.LOGGER.error("Failed to load accounts", e);
        }
    }

    public static void save() {
        File file = AccountsManager.accountsFile();
        file.getParentFile().mkdirs();

        List<AccountStorage> stored = new ArrayList<>();
        for (Account account : AccountsManager.accounts) {
            stored.add(account.toStorage());
        }

        try (FileWriter writer = new FileWriter(file)) {
            AccountsManager.GSON.toJson(stored, writer);
        } catch (Exception e) {
            RubyClient.LOGGER.error("Failed to save accounts", e);
        }
    }

    private static File accountsFile() {
        return new File(RubyClient.client.runDirectory, "config/ruby/accounts.json");
    }

    @Override
    public Iterator<Account> iterator() {
        return AccountsManager.accounts.iterator();
    }
}
