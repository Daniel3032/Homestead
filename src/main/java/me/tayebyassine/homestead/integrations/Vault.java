package me.tayebyassine.homestead.integrations;

import me.tayebyassine.homestead.Homestead;
import me.tayebyassine.homestead.integrations.vault.EconomyProvider;
import me.tayebyassine.homestead.integrations.vault.LegacyVaultProvider;
import me.tayebyassine.homestead.integrations.vault.PermissionsProvider;
import me.tayebyassine.homestead.integrations.vault.VaultUnlockedProvider;
import me.tayebyassine.homestead.logs.Logger;

/**
 * The ancient Vault plugin, doesn't support Folia.
 * <p>
 * When VaultUnlocked is installed, each service is looked up on its modern API first.
 * VaultUnlocked also ships the legacy Vault API, and many plugins (EssentialsX, LuckPerms...)
 * still only register there, so if nothing is found on the modern API, the legacy one is used.
 */
public class Vault {

    private static boolean IS_VAULTUNLOCKED_DETECTED = false;
    private static boolean IS_LEGACY_VAULT_DETECTED = false;

    static {
        try {
            Class.forName("net.milkbowl.vault2.economy.Economy");
            Class.forName("net.milkbowl.vault2.permission.PermissionUnlocked");
            IS_VAULTUNLOCKED_DETECTED = true;
        } catch (ClassNotFoundException e) {
            IS_VAULTUNLOCKED_DETECTED = false;
        }

        try {
            Class.forName("net.milkbowl.vault.economy.Economy");
            Class.forName("net.milkbowl.vault.permission.Permission");
            IS_LEGACY_VAULT_DETECTED = true;
        } catch (ClassNotFoundException e) {
            IS_LEGACY_VAULT_DETECTED = false;
        }
    }

    private VaultUnlockedProvider vaultUnlockedProvider;
    private LegacyVaultProvider legacyVaultProvider;

    private EconomyProvider economy;
    private PermissionsProvider permissions;

    public Vault(Homestead plugin) {
        if (isVaultUnlockedDetected()) {
            this.vaultUnlockedProvider = new VaultUnlockedProvider(plugin);
        }

        if (IS_LEGACY_VAULT_DETECTED) {
            this.legacyVaultProvider = new LegacyVaultProvider(plugin);
        }

        if (vaultUnlockedProvider != null) {
            this.economy = vaultUnlockedProvider;
            this.permissions = vaultUnlockedProvider;
        } else {
            this.economy = legacyVaultProvider;
            this.permissions = legacyVaultProvider;
        }
    }

    public boolean setupEconomy() {
        if (vaultUnlockedProvider != null && vaultUnlockedProvider.setupEconomy()) {
            economy = vaultUnlockedProvider;
            return true;
        }

        if (legacyVaultProvider != null && legacyVaultProvider.setupEconomy()) {
            economy = legacyVaultProvider;

            if (vaultUnlockedProvider != null) {
                Logger.warning("No Economy service provider found on the VaultUnlocked API, using the legacy Vault API instead.");
            }

            return true;
        }

        return false;
    }

    public boolean setupPermissions() {
        if (vaultUnlockedProvider != null && vaultUnlockedProvider.setupPermissions()) {
            permissions = vaultUnlockedProvider;
            return true;
        }

        if (legacyVaultProvider != null && legacyVaultProvider.setupPermissions()) {
            permissions = legacyVaultProvider;

            if (vaultUnlockedProvider != null) {
                Logger.warning("No Permissions service provider found on the VaultUnlocked API, using the legacy Vault API instead.");
            }

            return true;
        }

        return false;
    }

    public EconomyProvider getEconomy() {
        return economy;
    }

    public PermissionsProvider getPermissions() {
        return permissions;
    }

    public boolean isEconomyReady() {
        return getEconomy() != null;
    }

    public boolean isPermissionsReady() {
        return getPermissions() != null;
    }

    public static boolean isVaultUnlockedDetected() {
        return IS_VAULTUNLOCKED_DETECTED;
    }
}
