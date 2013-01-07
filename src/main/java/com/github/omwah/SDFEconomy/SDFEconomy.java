package com.github.omwah.SDFEconomy;

import java.io.File;
import java.util.Iterator;
import java.util.Observer;

import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.command.PluginCommand;

import net.milkbowl.vault.permission.Permission;

import com.github.omwah.SDFEconomy.commands.SDFEconomyCommandExecutor;

/*
 * Bukkit Plugin class for SDFEconomy
 */
public class SDFEconomy extends JavaPlugin {
    private SDFEconomyAPI api;
    private EconomyStorage storage;
    private Permission permission;

    /*
     * Called when the plugin is enabled
     */
    @Override
    public void onEnable() {
        this.getConfig().addDefault("storage.yaml.filename", "accounts.yaml");
        this.getConfig().addDefault("storage.yaml.save_on_update", true);

        File storageFile = new File(this.getDataFolder(), this.getConfig().getString("storage.yaml.filename"));

        EconomyYamlStorage yaml_storage = new EconomyYamlStorage(storageFile);
        this.storage = yaml_storage;

        boolean save_on_update = this.getConfig().getBoolean("storage.yaml.save_on_update");
        if(save_on_update) {
            yaml_storage.addObserver((Observer) new StorageCommitEveryUpdate());
        }
                                                 
        MultiverseInvLocationTranslator locationTrans = new MultiverseInvLocationTranslator(this);
        this.api = new SDFEconomyAPI(this.getConfig(), this.storage, locationTrans);

        // save the configuration file
        saveDefaultConfig();
        
        // Set up permissions from Vault
        setupPermissions();
        
        // Create the Listener to register players into economy on joining
        // Or teleporting to a world for the first time
        new SDFEconomyListener(this);
        
        // Load up the list of commands in the plugin.yml and register each of these
        // This makes is simpler to update the command names that this Plugin responds
        // to just by editing plugin.yml
        for(Iterator cmd_iter = this.getDescription().getCommands().keySet().iterator(); cmd_iter.hasNext();) {
            // set the command executor for the Command
            PluginCommand curr_cmd = this.getCommand((String) cmd_iter.next());
            curr_cmd.setExecutor(new SDFEconomyCommandExecutor(curr_cmd, this.permission, this.api, this.getConfig(), this.getServer()));
        }
    }
    
    /*
     * Called when the plug-in shuts down
     */
    @Override
    public void onDisable() {
        storage.commit();
    }

    /*
     * Returns the Vault interface class for the Economy
     */
    public SDFEconomyAPI getAPI() {
        return this.api;
    }
    
    private boolean setupPermissions() {
        RegisteredServiceProvider<Permission> permissionProvider = getServer().getServicesManager().getRegistration(net.milkbowl.vault.permission.Permission.class);
        if (permissionProvider != null) {
            permission = permissionProvider.getProvider();
        }
        return (permission != null);
    }
}
