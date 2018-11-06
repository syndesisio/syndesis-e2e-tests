package io.syndesis.qe.utils;

import io.syndesis.qe.accounts.Account;
import io.syndesis.qe.accounts.AccountsDirectory;

import java.util.Optional;

/**
 * Utility class to get a test account from AccountsDirectory easily.
 */
public class AccountUtils {
    /**
     * Get a testing account with given name.
     * @param accountName
     * @return Account instance matching the name
     * @throws IllegalStateException when account with such name doesn't exist.
     */
    public static Account get(String accountName) {
        Optional<Account> opt = AccountsDirectory.getInstance().getAccount(accountName);
        if (!opt.isPresent()) {
            throw new IllegalStateException("Following account not found in credentials definitions: " + accountName);
        }
        return opt.get();
    }

    /**
     * Check if testing account with given name exists.
     * @param accountName
     * @return true if exists, false otherwise
     */
    public static boolean exists(String accountName) {
        Optional<Account> opt = AccountsDirectory.getInstance().getAccount(accountName);
        return opt.isPresent();
    }
}
