package ruby.systems.accounts;

import net.minecraft.util.Util;
import ruby.RubyClient;

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
            return;
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
        return RubyClient.client.getSession().getUsername().equalsIgnoreCase(account.getUsername());
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
        Util.getIoWorkerExecutor().execute(() -> {
            if(account.fetchInfo() && account.login())
                RubyClient.LOGGER.info("Auto-logged in as {}", account.getUsername());
            else
                RubyClient.LOGGER.warn("Failed to auto-login as {}", account.getName());
        });
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
