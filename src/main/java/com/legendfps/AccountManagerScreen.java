package com.legendfps;

import net.minecraft.client.gui.screens.Screen;

public final class AccountManagerScreen extends RedScreen {
    public AccountManagerScreen(Screen parent) { super("ACCOUNT MANAGER", parent); }

    @Override
    protected void init() {
        int panel = Math.min(300, width - 24);
        int x = (width - panel) / 2;
        int y = 58;
        for (Config.SavedAccount account : Config.data.savedAccounts) {
            String label = account.username + (account.username.equalsIgnoreCase(AccountService.username()) ? "  (active)" : "");
            add(label, x, y, panel - 76, b -> {
                status = "Switching account…";
                AccountService.switchAccount(account.username, result -> {
                    status = result.message();
                    if (result.ok()) minecraft.setScreen(new AccountManagerScreen(parent));
                });
            });
            add("Remove", x + panel - 72, y, 72, b -> {
                AccountService.removeAccount(account.username);
                status = "Removed " + account.username;
                rebuildWidgets();
            });
            y += 24;
        }
        if (Config.data.savedAccounts.isEmpty()) {
            status = "No saved accounts on this device";
        }
        add("Add / Login account", x, Math.min(y + 8, height - 82), panel, b -> minecraft.setScreen(new AccountScreen(this)));
        add("Sign out", x, height - 78, panel / 2 - 4, b -> {
            AccountService.signOut();
            status = "Signed out";
            rebuildWidgets();
        });
        add("Back", x + panel / 2 + 4, height - 78, panel / 2 - 4, b -> onClose());
    }
}
