package de.mineMaurice11.likeit;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.HangingSign;
import org.bukkit.block.Sign;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.WallHangingSign;
import org.bukkit.block.data.type.WallSign;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.Sound;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class LikeItPlugin extends JavaPlugin implements Listener {

	private static LikeItPlugin instance;
	
    private Economy econ;
    
    //signsData
    private int likeAmount;
    private boolean allowSelfLike;
    
    //playerData
    private int maxSigns;

    //YML
    private File signsDataFile;
    private YamlConfiguration signsData;
    
    private File playerDataFile;    
    private YamlConfiguration playerData;
    

    
    @Override
    public void onEnable() {
    	instance = this;
    	
    	//CONFIG
    	saveDefaultConfig();
    	
    	//signsData        
        likeAmount = getConfig().getInt("likeAmount", 1);
        allowSelfLike = getConfig().getBoolean("allow-self-like", false);
        
        //playerData
        maxSigns = getConfig().getInt("maxSigns", 3);
        
        //CONFIG END
        
    	//Economy Check
        if (!setupEconomy()) {
            getLogger().severe("Vault/Economy nicht gefunden – Plugin deaktiviert!");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        loadData();

        Bukkit.getPluginManager().registerEvents(this, this);
        getLogger().info("LikeItPlugin aktiviert.");
    }

	//Methode zum Prüfen, ob Eco-Plugin existiert
    private boolean setupEconomy() {
        RegisteredServiceProvider<Economy> rsp = getServer()
                .getServicesManager()
                .getRegistration(Economy.class);
        if (rsp != null) {
            econ = rsp.getProvider();
            return true;
        }
        return false;
    }
    
    
    private void loadData() {
    	
        signsDataFile = new File(getDataFolder(), "signsData.yml");
        if (!signsDataFile.exists()) {
            signsDataFile.getParentFile().mkdirs();
            saveResource("signsData.yml", false);
        }
        signsData = YamlConfiguration.loadConfiguration(signsDataFile);
       
        playerDataFile = new File(getDataFolder(), "playerData.yml");
        if (!playerDataFile.exists()) {
            playerDataFile.getParentFile().mkdirs();
            saveResource("playerData.yml", false);
        }
        playerData = YamlConfiguration.loadConfiguration(playerDataFile);
    }

    private void saveData() {
        try {
           signsData.save(signsDataFile);
        } catch (IOException e) {
            getLogger().severe("Fehler beim Speichern der signsData.yml");
            e.printStackTrace();
        }
        
        try {
            playerData.save(playerDataFile);
         } catch (IOException e) {
             getLogger().severe("Fehler beim Speichern der playerData.yml");
             e.printStackTrace();
         }
    }

    private String locToString(Location loc) {
        return loc.getWorld().getName() + "," + loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ();
    }
    
    public boolean isAllowSelfLike() {
        return allowSelfLike;
    }
    
    public static LikeItPlugin getInstance() {
        return instance;
    }
    
    public void updateLikes(Sign sign, int currentLikes, Player p) {
    	
    	sign.setLine(2, "§2" + currentLikes);
        if(currentLikes == 1) {
        	sign.setLine(3, "§2Like");
        }else {
        	sign.setLine(3, "§2Likes");           
        }
        sign.update();
    }
    
    public void playLikeSound(Player player) {
        player.playSound(player.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_TWINKLE, 1.0f, 1.0f);
    }
    
    @EventHandler
    public void onSignChange(SignChangeEvent event) {
    	Player p = event.getPlayer();
    	
    	Sign sign = (Sign) event.getBlock().getState();
        PersistentDataContainer container = sign.getPersistentDataContainer();
        NamespacedKey key = new NamespacedKey(getInstance(), "likeSign");
    	
        // Keine Bearbeitung wenn schon LikeSign
        if (container.has(key, PersistentDataType.BYTE)) return;
        
        // Falls richtig [Like], speichern und Flag setzen
        if (event.getLine(0).equalsIgnoreCase("[Like]")) {
        	
        	Location signLoc = sign.getLocation();
            String locStr = locToString(signLoc);
            int currentLikes =signsData.getInt(locStr + ".likes", 0);
            
            
            
            //Schild Formatierung
            event.setLine(0, "§2[Like]");
            event.setLine(1, "§7"+ p.getName());
            event.setLine(2, "§2" + currentLikes);
            event.setLine(3, "§2Likes");
            
            
            // signsData.yml
            signsData.set(locStr + ".owner", p.getUniqueId().toString());
            signsData.set(locStr + ".clickedBy", new ArrayList<String>());
            signsData.set(locStr + ".likes", 0);
            
            signUse(p);
            saveData();
            
            // flag: Like-Schild gesetzt
            container.set(key, PersistentDataType.BYTE, (byte) 1);
            sign.update();

            p.sendMessage("§aLike-Schild erstellt! Spieler können dir jetzt Likes geben.");
            
        }
    }
    
    public void signUse(Player p) {
    	
    	int usedSigns = playerData.getInt(p.getUniqueId().toString() + ".usedSigns", 0);
    	int maxSigns = playerData.getInt(p.getUniqueId().toString() + ".maxSigns", 3);
    	    	
        // playerData.yml
        usedSigns += 1;
        playerData.set(p.getUniqueId().toString() + ".usedSigns", usedSigns);
        playerData.set(p.getUniqueId().toString() + ".maxSigns", 3);
        
        //signs left
        int signsLeft = maxSigns - usedSigns;
        
        p.sendMessage("Schilder übrig: " + signsLeft);
    }
    
    public void signBreak(String locStr) {
    	// uses wiederherstellen
        
        UUID ownerUUID = UUID.fromString(signsData.getString(locStr + ".owner"));
        
        Player p = Bukkit.getPlayer(ownerUUID);
        
        int usedSigns = playerData.getInt(p.getUniqueId().toString() + ".usedSigns", 0);
        usedSigns += -1;
        playerData.set(p.getUniqueId().toString() + ".usedSigns", usedSigns);
        
        
	     // Gespeicherte Like-Daten löschen
        signsData.set(locStr, null);
    }
    

    /*	Like-Schild Interaction (kein Platzieren)
     * 	
     * 
     */
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getClickedBlock() == null) return;
        
        // nur Rechtsklick
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return; 
        
        // Schild (-material) check
        Material mat = event.getClickedBlock().getType();
        if (mat != Material.OAK_SIGN && mat != Material.OAK_WALL_SIGN && mat != Material.OAK_HANGING_SIGN && mat != Material.OAK_WALL_HANGING_SIGN && 
            mat != Material.SPRUCE_SIGN && mat != Material.SPRUCE_WALL_SIGN && mat != Material.SPRUCE_HANGING_SIGN && mat != Material.SPRUCE_WALL_HANGING_SIGN &&
            mat != Material.BIRCH_SIGN && mat != Material.BIRCH_WALL_SIGN && mat != Material.BIRCH_HANGING_SIGN && mat != Material.BIRCH_WALL_HANGING_SIGN &&
            mat != Material.JUNGLE_SIGN && mat != Material.JUNGLE_WALL_SIGN && mat != Material.JUNGLE_HANGING_SIGN && mat != Material.JUNGLE_WALL_HANGING_SIGN &&
            mat != Material.ACACIA_SIGN && mat != Material.ACACIA_WALL_SIGN && mat != Material.ACACIA_HANGING_SIGN && mat != Material.ACACIA_WALL_HANGING_SIGN &&
            mat != Material.DARK_OAK_SIGN && mat != Material.DARK_OAK_WALL_SIGN &&
            mat != Material.MANGROVE_SIGN && mat != Material.MANGROVE_WALL_SIGN &&
            mat != Material.CHERRY_SIGN && mat != Material.CHERRY_WALL_SIGN &&
    		mat != Material.PALE_OAK_SIGN && mat != Material.PALE_OAK_WALL_SIGN &&
			mat != Material.BAMBOO_SIGN && mat != Material.BAMBOO_WALL_SIGN &&
			mat != Material.CRIMSON_SIGN && mat != Material.CRIMSON_WALL_SIGN &&
            mat != Material.WARPED_SIGN &&  mat != Material.WARPED_WALL_SIGN) return;
        
        if (!(event.getClickedBlock().getState() instanceof Sign)) return;
        Sign sign = (Sign) event.getClickedBlock().getState();
        
        // Check ob LIKE richtig geschrieben NOTWENDIG?
        if (!sign.getLine(0).equalsIgnoreCase("§2[Like]")) return;
        
        //Check ob Flag gesetzt
        PersistentDataContainer container = sign.getPersistentDataContainer();
        NamespacedKey key = new NamespacedKey(getInstance(), "likeSign");
        if (!container.has(key, PersistentDataType.BYTE)) {
        	event.getPlayer().sendMessage("Kein Like-Schild. Nice try!");
        	return;
        }

        event.setCancelled(true);
        Player clicker = event.getPlayer();

        Location signLoc = sign.getLocation();
        String locStr = locToString(signLoc);

        // Check if sign is registered NOTWENDIG?
        if (!signsData.contains(locStr + ".owner")) {
            clicker.sendMessage("§cDieses Schild ist kein gültiges Like-Schild.");
            return;
        }
        
        UUID ownerUUID = UUID.fromString(signsData.getString(locStr + ".owner"));
        UUID clickerUUID = clicker.getUniqueId();

    	// Eigenes Schild klicken Treatment
        if (ownerUUID.equals(clickerUUID) && !isAllowSelfLike() ) { //check if selflike allowed
            clicker.sendMessage("§cDu kannst dein eigenes Schild nicht liken.");
            return;
        }
        
        // Zweifach Liken verhindern
        List<String> clickedBy = signsData.getStringList(locStr + ".clickedBy");
        if (clickedBy.contains(clickerUUID.toString())) {
            clicker.sendMessage("§cDu hast dieses Schild bereits geliked.");
            return;
        }

        //Player Messages after LIKE
        Player ownerPlayer = Bukkit.getPlayer(ownerUUID);
        
        econ.depositPlayer(ownerPlayer, likeAmount);
        clicker.sendMessage("§aDu hast " + ownerPlayer.getName() + " ein Like gegeben!");
        
        if (ownerPlayer != null && ownerPlayer.isOnline()) {
            ownerPlayer.sendMessage("§eDu hast 1 Like von " + clicker.getName() + " bekommen!");
        }
        
        //signsData
        clickedBy.add(clickerUUID.toString());
        signsData.set(locStr + ".clickedBy", clickedBy);
        
        int currentLikes =signsData.getInt(locStr + ".likes", 0);
        currentLikes += likeAmount;	//add Like-Value to Player
        signsData.set(locStr + ".likes", currentLikes);
        
        saveData();
        
        //Update Text on signs
        updateLikes(sign, currentLikes, clicker);
        
        //Play Sound when LIKE
        playLikeSound(clicker);
    }
    
    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
    	BlockState state = event.getBlock().getState();
        if (!(state instanceof Sign sign)) return;

        // Prüfen, ob das Schild ein Like-Schild ist
        PersistentDataContainer container = sign.getPersistentDataContainer();
        NamespacedKey key = new NamespacedKey(getInstance(), "likeSign");
        if (!container.has(key, PersistentDataType.BYTE)) return;
        
          
        
        // Standort als String 
        String locStr = locToString(sign.getLocation());
        
        //behandelt uses
        // falls owner null
        String ownerStr = signsData.getString(locStr + ".owner");
        
        if (ownerStr != null && !ownerStr.isEmpty()) {
            UUID ownerUUID = UUID.fromString(ownerStr);
            signBreak(locStr);
            getLogger().info("[DEL] bei " + locStr + " ("+Bukkit.getPlayer(ownerUUID).getName()+") " + "von Spieler " + event.getPlayer() + ", Daten gelöscht.");
        } else {
            // Kein Owner gespeichert → evtl. kein Like-Schild
        	getLogger().info("Fehler UUID Owner in inBlockBreakEvent");
            return;
        }
        
        saveData();       
	}
    
    @EventHandler
    public void onBreakSignCheck(BlockBreakEvent event) {
        Block brokenBlock = event.getBlock();

        // Alle 6 Nachbar-Richtungen prüfen
        BlockFace[] faces = {
            BlockFace.UP,
            BlockFace.DOWN,
            BlockFace.NORTH,
            BlockFace.SOUTH,
            BlockFace.EAST,
            BlockFace.WEST
        };

        for (BlockFace face : faces) {
            Block neighbor = brokenBlock.getRelative(face);

            if (neighbor.getState() instanceof Sign sign) {
            	BlockData data = neighbor.getBlockData();
            	
            	// prüfen, ob es ein Like-Schild ist
                PersistentDataContainer container = sign.getPersistentDataContainer();
                NamespacedKey key = new NamespacedKey(getInstance(), "likeSign");
                if (!container.has(key, PersistentDataType.BYTE)) return;
                
                getLogger().info(face.toString()+" ");
            	
                //Block-Art
            	if(data instanceof WallSign wallSign) {
            		BlockFace attachedFace = wallSign.getFacing(); // BlockFace des Schilds

            	    if (attachedFace.equals(face)) {
            	        // Der zerstörte Block ist die Rückwand dieses Schilds
            	        event.setCancelled(true);
            	        event.getPlayer().sendMessage("Du kannst dieses Schild nicht indirekt zerstören.");
            	        return;
            	    }
            	}else if(data instanceof HangingSign hangingSign){
            		if(face == BlockFace.DOWN){
            			event.setCancelled(true);
            	        event.getPlayer().sendMessage("Du kannst dieses Schild nicht indirekt zerstören.");
            	        return;
            		}
            	}else{            		
            		if(face == BlockFace.UP){
            			event.setCancelled(true);
            	        event.getPlayer().sendMessage("Du kannst dieses Schild nicht indirekt zerstören.");
            	        return;
            		}
            	}

                
            }
        }

        // Wenn kein Schild in der Nähe war → Abbau erlaubt
    }
}
