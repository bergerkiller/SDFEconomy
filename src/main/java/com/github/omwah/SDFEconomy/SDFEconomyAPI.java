/*
 */
package com.github.omwah.SDFEconomy;

import java.text.DecimalFormat;
import java.util.List;
import net.milkbowl.vault.economy.EconomyResponse;
import net.milkbowl.vault.economy.EconomyResponse.ResponseType;
import org.bukkit.Location;
import org.bukkit.configuration.Configuration;

/**
 * Provides the interface necessary to implement a Vault Economy.
 * Implements most of Vault.Economy interface but does not declare
 * itself as implementing this interface because there is no easy
 * way in Vault to use this class directly without a proxy class.
 */
public class SDFEconomyAPI {
    private EconomyStorage storage;
    private Configuration config;
    private LocationTranslator locTrans;
    
    public SDFEconomyAPI(Configuration config, EconomyStorage storage, LocationTranslator locationTrans) {
        this.config = config;
        this.storage = storage;
        this.locTrans = locationTrans;
        
        this.config.addDefault("api.bank.enabled", true);
        this.config.addDefault("api.bank.initial_balance", 0.00);
        this.config.addDefault("api.player.initial_balance", 10.00);
        this.config.addDefault("api.currency.numerical_format", "#,##0.00");
        this.config.addDefault("api.currency.name.plural", "simoleons");
        this.config.addDefault("api.currency.name.singular", "simoleon");
    }
    
    /*
     * Whether bank support is enabled
     */

    public boolean hasBankSupport() {
        return this.config.getBoolean("api.bank.enabled");
    }

    /*
     * Returns -1 since no rounding occurs.
     */

    public int fractionalDigits() {
        return -1;
    }

    public String format(double amount) {
        String pattern = this.config.getString("api.currency.numerical_format");
        DecimalFormat formatter = new DecimalFormat(pattern);
        String formatted = formatter.format(amount);
        if(amount == 1.0) {
            formatted += " " + currencyNameSingular();
        } else {
            formatted += " " + currencyNamePlural();
        }
        return formatted;
    }

    public String currencyNamePlural() {
        return this.config.getString("api.currency.name.plural");
    }

    public String currencyNameSingular() {
         return this.config.getString("api.currency.name.singular");
    }
    
    public String getPlayerLocationName(String playerName) {
        if (playerName == null) {
            return null;
        } else {
            return locTrans.getLocationName(playerName);
        }
    }
    
    public String getLocationTranslated(Location location) {
        if(location == null) {
            return null;
        } else {
            return locTrans.getLocationName(location);
        }
    }
    
    public List<String> getPlayers(String locationName) {
        return storage.getPlayerNames(locationName);
    }
    
    public boolean createPlayerAccount(String playerName) {
        return createPlayerAccount(playerName, getPlayerLocationName(playerName));
    }

    public boolean createPlayerAccount(String playerName, String locationName) {        
        // Make sure an account can not be created without a location
        if(locationName != null && !hasAccount(playerName, locationName)) {
            double initialBalance = config.getDouble("api.player.initial_balance");
            PlayerAccount account = storage.createPlayerAccount(playerName, locationName, initialBalance);
            return true;
        } else {
            return false;
        }
    }
        
    public boolean hasAccount(String playerName) {
        return hasAccount(playerName, getPlayerLocationName(playerName));
    }
    
    public boolean hasAccount(String playerName, String locationName) {
        return playerName != null && locationName != null && storage.hasPlayerAccount(playerName, locationName);
    }

    public double getBalance(String playerName) {
        return getBalance(playerName, getPlayerLocationName(playerName));
    }
    
    public double getBalance(String playerName, String locationName) {
        double balance = 0.0;
        if (locationName != null && hasAccount(playerName, locationName)) { 
            PlayerAccount account = storage.getPlayerAccount(playerName, locationName);
            balance = account.getBalance();
        }
        return balance;
    }    
    
    /* 
     * Normally only accessed for adminstrative purposes
     * @return true if the balance was changed, false otherwise
     */
    public boolean setBalance(String playerName, double amount) {
        return setBalance(playerName, getPlayerLocationName(playerName), amount);
    }

    /* Normally only accessed for adminstrative purposes
     * @return true if the balance was changed, false otherwise
     */
    public boolean setBalance(String playerName, String locationName, double amount) {
        if (locationName != null && hasAccount(playerName, locationName)) { 
            PlayerAccount account = storage.getPlayerAccount(playerName, locationName);
            account.setBalance(amount);
            return true;
        }
        return false;
    }

    public boolean has(String playerName, double amount) {
        return has(playerName, getPlayerLocationName(playerName), amount);
    }

    public boolean has(String playerName, String locationName, double amount) {
        return amount >= 0.0 && amount <= getBalance(playerName, locationName);
    }

    public EconomyResponse withdrawPlayer(String playerName, double amount) {
        String locationName = getPlayerLocationName(playerName);

        EconomyResponse response;
        if (locationName != null && hasAccount(playerName, locationName) && has(playerName, locationName, amount)) {
            PlayerAccount account = storage.getPlayerAccount(playerName, locationName);
            account.setBalance(account.getBalance() - amount);
            response = new EconomyResponse(amount, account.getBalance(), ResponseType.SUCCESS, "");
        } else {
            response = new EconomyResponse(0.0, 0.0, ResponseType.FAILURE, "Can not withdraw from player");
        }
        return response;
    }

    public EconomyResponse depositPlayer(String playerName, double amount) {
        String locationName = getPlayerLocationName(playerName);

        EconomyResponse response;
        if (locationName != null && hasAccount(playerName, locationName)) {
            PlayerAccount account = storage.getPlayerAccount(playerName, locationName);
            account.setBalance(account.getBalance() + amount);
            response = new EconomyResponse(amount, account.getBalance(), ResponseType.SUCCESS, "");
        } else {
            response = new EconomyResponse(0.0, 0.0, ResponseType.FAILURE, "Can not deposit to player");
        }
        return response;
    }
    
    public List<String> getBanks() {
        return storage.getBankNames();
    }

    public EconomyResponse createBank(String name, String playerName) {
        return createBank(name, playerName, getPlayerLocationName(playerName));
    }

    public EconomyResponse createBank(String name, String playerName, String locationName) {
        
        // Make sure a bank can not be created without a location
        EconomyResponse response;
        if(storage.hasBankAccount(name)) {
            response = new EconomyResponse(0.0, 0.0, ResponseType.FAILURE, "Bank account already exists");
        } else if(locationName != null) {
            double initialBalance = config.getDouble("api.bank.initial_balance");
            BankAccount account = storage.createBankAccount(name, playerName, locationName, initialBalance);
            response = new EconomyResponse(initialBalance, account.getBalance(), ResponseType.SUCCESS, "");
        } else {
            response = new EconomyResponse(0.0, 0.0, ResponseType.FAILURE, "Can not create a bank with an unknown location");
        }
        return response;
    }

    public EconomyResponse deleteBank(String name) {
        EconomyResponse response;
        if (storage.hasBankAccount(name)) {
            double balance = storage.getBankAccount(name).getBalance();
            storage.deleteBankAccount(name);
            if(!storage.hasBankAccount(name)) {
                response = new EconomyResponse(balance, 0, ResponseType.SUCCESS, "");
            } else {
                response = new EconomyResponse(0, 0, ResponseType.FAILURE, "Could not delete bank account: " + name);
            }
        } else {
            response = new EconomyResponse(0, 0, ResponseType.FAILURE, "Could find bank account to delete: " + name);
        }
        return response;
    }

    public EconomyResponse bankBalance(String name) {
        EconomyResponse response;
        if (storage.hasBankAccount(name)) {
            BankAccount account = storage.getBankAccount(name);
            response = new EconomyResponse(0, account.getBalance(), ResponseType.SUCCESS, "");
        } else {
            response = new EconomyResponse(0, 0, ResponseType.FAILURE, "Could not find bank account: " + name);
        }
        return response;
    }

    public EconomyResponse bankHas(String name, double amount) {
        EconomyResponse response;
        if(storage.hasBankAccount(name)) {
            BankAccount account = storage.getBankAccount(name);
            if (account.getBalance() > amount) {
                response = new EconomyResponse(0, account.getBalance(), ResponseType.SUCCESS, "");
            } else {
                response = new EconomyResponse(0, account.getBalance(), ResponseType.FAILURE, "Account does not enough money");
            }
        } else {
            response = new EconomyResponse(0, 0, ResponseType.FAILURE, "Account does not exist");
        }
        return response;
    }

    public EconomyResponse bankWithdraw(String name, double amount) {
        EconomyResponse response = bankHas(name, amount);
        // Only act upon account if it has enough money
        if(response.type == ResponseType.SUCCESS) {
            BankAccount account = storage.getBankAccount(name);
            account.setBalance(account.getBalance() - amount);
            response = new EconomyResponse(amount, account.getBalance(), ResponseType.SUCCESS, "");
        }
        return response;
    }

    public EconomyResponse bankDeposit(String name, double amount) {
        EconomyResponse response;
        if (storage.hasBankAccount(name)) {
            BankAccount account = storage.getBankAccount(name);
            account.setBalance(account.getBalance() + amount);
            response = new EconomyResponse(amount, account.getBalance(), ResponseType.SUCCESS, "");
        } else {
            response = new EconomyResponse(0, 0, ResponseType.FAILURE, "Account does not exist");
        }
        return response;
    }

    public EconomyResponse isBankOwner(String name, String playerName) {
        String location = getPlayerLocationName(playerName);
        return isBankOwner(name, playerName, location);
    }
    
    public EconomyResponse isBankOwner(String name, String playerName, String location) {
        EconomyResponse response;
        if(storage.hasBankAccount(name) && location != null) {
            BankAccount account = storage.getBankAccount(name);
            if(account.getLocation().equalsIgnoreCase(location) && account.isOwner(playerName)) {
                response = new EconomyResponse(0, account.getBalance(), ResponseType.SUCCESS, "");
            } else {
                response = new EconomyResponse(0, account.getBalance(), ResponseType.FAILURE, playerName + " is not an owner of " + name);
            }
        } else {
            response = new EconomyResponse(0, 0, ResponseType.FAILURE, "Bank account: " + name + " does not exist @ " + location);
        }
        return response;    
    }
    
    public EconomyResponse isBankMember(String name, String playerName) {
        String location = getPlayerLocationName(playerName);
        return isBankMember(name, playerName, location);
    }

    public EconomyResponse isBankMember(String name, String playerName, String location) {
        EconomyResponse response;
        if(storage.hasBankAccount(name) && location != null) {
            BankAccount account = storage.getBankAccount(name);
            // An owner should also be a member
            if(account.getLocation().equalsIgnoreCase(location) && (account.isOwner(playerName) || account.isMember(playerName))) {
                response = new EconomyResponse(0, account.getBalance(), ResponseType.SUCCESS, "");
            } else {
                response = new EconomyResponse(0, account.getBalance(), ResponseType.FAILURE, playerName + " is not a member of " + name);
            }
        } else {
            response = new EconomyResponse(0, 0, ResponseType.FAILURE, "Bank account: " + name + " does not exist @ " + location);
        }
        return response;
    }
    
    /*
     * Force a reload of the accounts storage
     */
    public void forceReload() {
        storage.reload();
    }

    /*
     * Force a commit of the accounts storage
     */
     public void forceCommit() {
        storage.commit();
    }
}
