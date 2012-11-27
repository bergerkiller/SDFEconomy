/*
 */
package com.github.omwah.SDFEconomy;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.ArrayList;
import java.util.logging.Logger;
import java.util.Observer;
import java.util.Observable;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.configuration.ConfigurationSection;

/**
 * Implements an Economy storage through a YAML file
 */
public class EconomyYamlStorage implements EconomyStorage, Observer {
    private final String player_prefix = "player";
    private final String bank_prefix = "bank";
    
    private final String filename;
    private final boolean save_on_update;
    YamlConfiguration storage;

    private static final Logger log = Logger.getLogger("Minecraft");

    public EconomyYamlStorage(String filename, boolean save_on_update) {
        this.filename = filename;
        this.save_on_update = save_on_update;
        this.storage = new YamlConfiguration();
    }
    
    private ConfigurationSection getPlayerSection(String playerName, String location) {
        ConfigurationSection section = this.storage.getConfigurationSection(location + "." + this.player_prefix + "." + playerName);
        if (section == null) {
            section = this.storage.createSection(location + "." + this.player_prefix + "." + playerName);
        }
        return section;
    }

    public boolean hasPlayerAccount(String playerName, String location) {
        return getPlayerSection(playerName, location) != null;
    }

    public PlayerAccount getPlayerAccount(String playerName, String location) {
        ConfigurationSection section = getPlayerSection(playerName, location);
        PlayerAccount account = new PlayerAccount(playerName, location);
        account.setBalance(section.getDouble("balance"));
        account.addObserver((Observer) this);
        return account;
    }
    
    public PlayerAccount createPlayerAccount(String playerName, String location, double begBalance) {
        PlayerAccount newAccount = new PlayerAccount(playerName, location);
        newAccount.setBalance(begBalance);
        updateAccount(newAccount);
        newAccount.addObserver((Observer) this);
        return newAccount;
    }
    
    public List<String> getBankNames() {
        Set<String> namesSet = this.storage.getConfigurationSection(this.bank_prefix).getKeys(false);
        List<String> nameList = new ArrayList<String>();
        nameList.addAll(namesSet);
        return nameList;
    }
    
    private ConfigurationSection getBankSection(String accountName) {
        ConfigurationSection section = this.storage.getConfigurationSection(this.bank_prefix + "." + accountName);
        if (section == null) {
            section = this.storage.createSection(this.bank_prefix + "." + accountName);
        }
        return section;
    }

     public boolean hasBankAccount(String accountName, String location) {
        ConfigurationSection section = getBankSection(accountName);
        return section != null && section.getString("location").compareTo(location) == 0;
    }

    public BankAccount getBankAccount(String accountName) {
        ConfigurationSection section = getBankSection(accountName);
        BankAccount account = new BankAccount(accountName, section.getString("owner"), section.getString("location"));
        account.setBalance(section.getDouble("balance"));
        account.setMembers(section.getStringList("members"));
        account.addObserver((Observer) this);
        return account;
     }
    
    public BankAccount createBankAccount(String accountName, String owner, String location, double begBalance) {
        BankAccount account = new BankAccount(accountName, owner, location);
        account.setBalance(begBalance);
        account.addObserver((Observer) this);
        updateAccount(account);
        return account;
    }
    
    public void deleteBankAccount(String accountName) {
        this.storage.set(this.bank_prefix + "." + accountName, null);
    }
    
    public void update(Observable o, Object arg) {
        if (o instanceof Account) {
            updateAccount((Account) o);
        }
    }
            
    @Override
    public void updateAccount(Account account) {
        ConfigurationSection section;
        if(account instanceof PlayerAccount) {
            section = getPlayerSection(account.getName(), account.getLocation());
        } else if (account instanceof BankAccount) {
            section = getBankSection(account.getName());
        } else {
            throw new IllegalArgumentException("Account passed was not an instance of PlayerAccount or BankAccount");
        }
        section.set("balance", account.getBalance());
        section.set("location", account.getLocation());
        if (account instanceof BankAccount) {
            section.set("members", ((BankAccount) account).getMembers());
            section.set("owner", ((BankAccount) account).getOwner());
        }
        if (this.save_on_update) {
            this.commit();
        }
    }
    
    @Override
    public void commit() {
        try {
            this.storage.save(this.filename);
        } catch(IOException e) {
            this.log.severe("Error saving YamlStorage to: " + this.filename + "\n" + e);
        }
    }

}
