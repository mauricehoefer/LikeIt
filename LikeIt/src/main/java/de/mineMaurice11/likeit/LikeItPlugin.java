package de.mineMaurice11.likeit;

import net.milkbowl.vault.economy.Economy;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.WallSign;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
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
    
    //Data
    private int likeAmount;
    private boolean allowSelfLike;
    private int maxSigns;
    private boolean allowFastPlacement;
    private int placementCooldown;
    
    //YAML
    private File UPlayerDataFile;
    
    //Messages / Log
    private String msg_prefix = "§8[§7LikeIt§8] ";
    private String log_prefix = "[LikeIt] ";
    

    
    @Override
    public void onEnable() {
    	instance = this;
    	
    	//load Config
    	loadConfig();
    	
    	//Economy Check
        if (!setupEconomy()) {
            getLogger().severe("Vault/Economy nicht gefunden – Plugin deaktiviert!");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        //Placeholder Check
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new Placeholder(this, this).register();
        } else {
            getLogger().warning("PlaceholderAPI nicht gefunden!");
        }

        Bukkit.getPluginManager().registerEvents(this, this);
        
        logToConsole("§2    (  \\");
        logToConsole("§2     \\  \\  __");
        logToConsole("§2  (___)   |");
        logToConsole("§2 (___)|   |  LikeIt-Plugin");
        logToConsole("§2  (___) __|    aktiviert.");
        logToConsole("§2   (__)__ |__");
    }

	private static LikeItPlugin getInstance() {
	    return instance;
	}

	private void loadConfig() {
		//copies config.yml from.jar to plugin folder
		saveDefaultConfig();
		
		//Data       
        likeAmount = getConfig().getInt("likeAmount", 1);
        allowSelfLike = getConfig().getBoolean("allow-self-like", false);
        maxSigns = getConfig().getInt("maxSigns", 3);	
        allowFastPlacement = getConfig().getBoolean("allow-fast-placement", false);
        placementCooldown = getConfig().getInt("placement-cooldown", 5);
	}

	//Methode zum Prüfen, ob Economy-Plugin existiert
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

    private String locationToString(Location loc) {
        return loc.getWorld().getName() + "," + loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ();
    }
    
    private void logToConsole(String string) {
		//TODO log enabled?
		
		//Send to console
		ConsoleCommandSender console = Bukkit.getServer().getConsoleSender();
		console.sendMessage(log_prefix + string);
	}

	private void createPlayerFile(Player p) {
		UUID playerUUID = p.getUniqueId();
		
		//folder and file
		File dataFolder = new File(getDataFolder(), "playerData");
		if (!dataFolder.exists()) dataFolder.mkdirs();
		UPlayerDataFile = new File(dataFolder, playerUUID + ".yml");
		
		//does player file exist?
		if(!UPlayerDataFile.exists()) {
			try {
				UPlayerDataFile.createNewFile();
				
				YamlConfiguration UPlayerData = YamlConfiguration.loadConfiguration(UPlayerDataFile);
				
				//add real player name
				UPlayerData.set("name", p.getName());
				
				//save
				UPlayerData.save(UPlayerDataFile);
				
				logToConsole("Playerfile for " + p.getName() + " created.");				
			}catch(IOException e){
				logToConsole("Failed creating playerfile for " + p.getName());
			}
		}	
	}

	private void playLikeSound(Player player, Player owner) {
		//Player who is liking
	    player.playSound(player.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_TWINKLE, 1.0f, 1.0f);
	    
	    //Player who gets liked
	    owner.playSound(owner.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
	}

	private void updateLikesOnSign(Sign sign, int currentLikes, Player p) {
    	
    	sign.setLine(2, "§2" + currentLikes);
        if(currentLikes == 1) {
        	sign.setLine(3, "§2Like");
        }else {
        	sign.setLine(3, "§2Likes");           
        }
        sign.update();
    }


    private void deleteSignData(Sign sign) {
		//get owner
		UUID ownerUUID = getSignOwnerUUID(sign);
		
	    //get Location sign
	    Location signLoc = sign.getLocation();
	    String locStr = locationToString(signLoc);
	    
	    //YAML
		File file = new File(new File(getDataFolder(), "playerData"), ownerUUID.toString() + ".yml");
	    YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
	    
	    //set null / delete      
	    config.set(locStr, null);
	    
	    try {
	        config.save(file);
	    } catch (IOException e) {
	        e.printStackTrace();
	    }
	}

	private void setSignData(Sign sign) {
		//write signData only to owner
		//get ownerUUID
		UUID ownerUUID = getSignOwnerUUID(sign);
	
	    //get sign location 
	    Location signLoc = sign.getLocation();
	    String locStr = locationToString(signLoc);
	    
	    //YAML
		File file = new File(new File(getDataFolder(), "playerData"), ownerUUID.toString() + ".yml");
	    YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
	    
	    //set      
	    config.set(locStr + ".clickedBy", new ArrayList<String>());
	    config.set(locStr + ".likes", 0);
	    
	    try {
	        config.save(file);
	    } catch (IOException e) {
	        e.printStackTrace();
	    }
	}

	private void setFlags(Player p, Sign sign) {
		//get DataContainer 
		PersistentDataContainer container = sign.getPersistentDataContainer();
		
		//BYTE "likeSign"
		NamespacedKey key = new NamespacedKey(getInstance(), "likeSign");
		container.set(key, PersistentDataType.BYTE, (byte) 1);
		
		//STRING "owner"
		NamespacedKey ownerKey = new NamespacedKey(getInstance(), "owner");
		container.set(ownerKey, PersistentDataType.STRING, p.getUniqueId().toString());
		
		//update sign
	    sign.update();
	}

	private void setSignClickData(Player owner ,Player clicker, String locStr) {
		File file = new File(new File(getDataFolder(), "playerData"), owner.getUniqueId().toString() + ".yml");
	    YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
	    
	    //count likes
	    int currentLikes = config.getInt(locStr + ".likes",0);        
	    currentLikes += likeAmount;
	    config.set(locStr + ".likes", currentLikes);
	    
	    //clickedBy-List     
	    List<String> clickedBy = config.getStringList(locStr + ".clickedBy");
	    if (clickedBy == null) clickedBy = new ArrayList<>();
	    
	    String clickerUUID = clicker.getUniqueId().toString();
	    if (!clickedBy.contains(clickerUUID)) {
	        clickedBy.add(clickerUUID);
	    }

	    config.set(locStr + ".clickedBy", clickedBy);
	    
	    try {
	        config.save(file);
	    } catch (IOException e) {
	        e.printStackTrace();
	    }	
	}

	private void setGivenLikes(Player p, int amount) {
		File file = new File(new File(getDataFolder(), "playerData"), p.getUniqueId() + ".yml");
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        
        //get given Likes. Wenn keine vorhanden, dann 0 :)
        int givenLikesNEW = config.getInt("givenLikes",0);
        
        if(amount == -1) {
			//Player Join Event
			//Init given Likes zu Wert welcher gespeichert ist, oder 0
			
			config.set("givenLikes", givenLikesNEW);
			
		}else {
			givenLikesNEW += amount;
			config.set("givenLikes", givenLikesNEW);    		
		}
        try {
            config.save(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
		
	}

	private void setUsedSigns(Player p, int amount) {    	
    	File file = new File(new File(getDataFolder(), "playerData"), p.getUniqueId() + ".yml");
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        //get
        int usedSigns = config.getInt("usedSigns",0);
        
        //calc
        usedSigns += amount;
        
        //ausnahme, bei config Änderung, kann auch negativ werden -> mehr verfügbare signs(signsleft = max - used)
        if(usedSigns < 0) {
        	usedSigns = 0;
        }
        
        //set       
        config.set("usedSigns",usedSigns);
        try {
            config.save(file);
        } catch (IOException e) {
            e.printStackTrace();
        }		
	}

	private void setMaxSignUse(Player p) {
    	File file = new File(new File(getDataFolder(), "playerData"), p.getUniqueId() + ".yml");
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        
        //set       
        config.set("maxSigns",maxSigns); //maxSings aus Config geladen ;)
        try {
            config.save(file);
        } catch (IOException e) {
            e.printStackTrace();
        }	
    }
    
    private void setLikeEconomy(Player clicker, Player owner, int likeAmount) {
		
		//Economy give Like
		econ.depositPlayer(owner, likeAmount);
	    clicker.sendMessage(msg_prefix + "§aDu hast " + likeAmount + " Like an " + owner.getName() + " gegeben. Danke!");
	    
	    //owner recieves Like
	    if (owner != null && owner.isOnline()) {
	        owner.sendMessage(msg_prefix + "§aDu hast " + likeAmount + " Like von " + clicker.getName() + " bekommen!");
	    } 
	    
	    //log
	    logToConsole(clicker.getName() + " gave " + owner.getName() + " " + likeAmount + " Like.");
	}

	private void setLastSignTime(Player p) {
		File file = new File(new File(getDataFolder(), "playerData"), p.getUniqueId() + ".yml");
	    YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
	    
	    //get current time
	    long lastSignTime = System.currentTimeMillis(); 
	    
	    //set       
	    config.set("lastSignTime", lastSignTime);
	    try {
	        config.save(file);
	        logToConsole("setLastSignTime()");
	    } catch (IOException e) {
	        e.printStackTrace();
	    }			
	}

	protected int getGivenLikes(Player p) {
		File file = new File(new File(getDataFolder(), "playerData"), p.getUniqueId().toString() + ".yml");
	    YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
	    
	    //get
	    int givenLikes = config.getInt("givenLikes",0);        
	   	return givenLikes;
	}

	protected int getAmountSignsLeft(Player p) {
		File file = new File(new File(getDataFolder(), "playerData"), p.getUniqueId().toString() + ".yml");
	    YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
	    //get
	    int usedSigns = config.getInt("usedSigns",0);
	    int maxSigns = config.getInt("maxSigns",0);
	    
	    int signsLeft = maxSigns - usedSigns;
	   	return signsLeft;
    }
    
    private List<String> getClickedByList(UUID ownerUUID, String locStr) {
		File file = new File(new File(getDataFolder(), "playerData"), ownerUUID.toString() + ".yml");
	    YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
	    
	    List<String> clickedBy = config.getStringList(locStr + ".clickedBy");
	    if (clickedBy == null) {
	        return new ArrayList<>(); // Niemals null zurückgeben!
	    }
	    return clickedBy; 
	}

	private UUID getSignOwnerUUID (Sign sign) {
		//get owner
		PersistentDataContainer container = sign.getPersistentDataContainer();
		NamespacedKey ownerKey = new NamespacedKey(getInstance(), "owner");
		if(!container.has(ownerKey, PersistentDataType.STRING)) {
			//no owner
			logToConsole("Cant get Owner from Sign! (getSignOwnerUUID)");
			return null;
		}
		//get UUID owner
		String ownerUUID = container.get(ownerKey, PersistentDataType.STRING);			
		return UUID.fromString(ownerUUID);
	}

	private int getLikesData(Player p, String locStr) {
		File file = new File(new File(getDataFolder(), "playerData"), p.getUniqueId().toString() + ".yml");
	    YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
	    //get
	    int likes = config.getInt(locStr + ".likes");        
	   	return likes;		
	}

	private long getLastSignTime(Player p) {
		File file = new File(new File(getDataFolder(), "playerData"), p.getUniqueId().toString() + ".yml");
	    YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
	    //get
	    long lastSignTime = config.getLong("lastSignTime");
	   	return lastSignTime;
	}

	private boolean hasFlag(Sign sign, String flag) {
    	PersistentDataContainer container = sign.getPersistentDataContainer();
        NamespacedKey key = new NamespacedKey(getInstance(), flag);
        
        switch(flag) {
        	case "likeSign":
        		if(container.has(key, PersistentDataType.BYTE)) {
        			return true;
        		}
        		break;
        	case "owner":
        		break;	
        	default:
        		return false;
        }
        return false;
    }
    

    private boolean allowFastPlacement(Player p) {
		if(!allowFastPlacement) {
			long lastSignTime = getLastSignTime(p);
			long currentTime = System.currentTimeMillis();
			
			//calculate difference
			long timeDifference = currentTime - lastSignTime;
			long timeDifferenceSeconds = timeDifference/1000;
			long timeDifferenceMinutes = timeDifferenceSeconds/60;
			
			logToConsole("Secondes: " + String.valueOf(timeDifferenceSeconds));
			logToConsole("Minutes: " + String.valueOf(timeDifferenceMinutes));
			
			if(timeDifferenceMinutes >= placementCooldown) {
				return true;
			}else {
				
				long timeLeft = placementCooldown - timeDifferenceMinutes;
				
				if(timeLeft > 1) {
					p.sendMessage(msg_prefix + "§eBitte warte noch " + timeLeft + " Minuten, um ein neues Like-Schild zu platzieren.");
				}else {
					timeLeft = (placementCooldown*60)-timeDifferenceSeconds;
					p.sendMessage(msg_prefix + "§eBitte warte noch " + timeLeft + " Sekunden, um ein neues Like-Schild zu platzieren.");
				}
				
				return false;
			}
		}
		return true;
	}

	@EventHandler
	public void onPlayerJoin(PlayerJoinEvent event) {
		Player p = event.getPlayer();
		//existiert eine Player datei?
		createPlayerFile(p);
		
		
		//update beim joinen
		//maxSigns
		setMaxSignUse(p);
		setUsedSigns(p, 0); //init
		
		// update givenLikes
		setGivenLikes(p, -1);
	}

	@EventHandler
	public void onSignChange(SignChangeEvent event) {
		Player p = event.getPlayer();
		
		Sign sign = (Sign) event.getBlock().getState();
	    PersistentDataContainer container = sign.getPersistentDataContainer();
	    NamespacedKey key = new NamespacedKey(getInstance(), "likeSign");
		
	    // Keine Bearbeitung wenn schon LikeSign
	    if (container.has(key, PersistentDataType.BYTE)) return;
	    
	    //Limitierung mit singsLeft
	    int signsLeft = getAmountSignsLeft(p);
	    if(signsLeft <= 0) {
	    	p.sendMessage(msg_prefix + "§eDu hast alle verfügbaren Like-Schilder verwendet. Du kannst bestehende Schilder umplatzieren.");
	    	return;
	    }
	    
	    // Falls richtig [Like], speichern und Flag setzen
	    if (event.getLine(0).equalsIgnoreCase("[Like]")) {
	    	
	    	//Limitierung fastPlacement
		    if(!allowFastPlacement(p)) return;
	    	
	    	//Set FLAGS
	        setFlags(p, sign);
	        
	        //loc
	    	Location signLoc = sign.getLocation();
	        String locStr = locationToString(signLoc);
	        int currentLikes = getLikesData(p, locStr);
	        
	        //Schild Formatierung
	        event.setLine(0, "§2[Like]");
	        event.setLine(1, "§7"+ p.getName());
	        event.setLine(2, "§2" + currentLikes);
	        event.setLine(3, "§2Likes");
	        
	        //UPDATES
	        setLastSignTime(p);
	        setSignData(sign);        	
	        setMaxSignUse(p);            	
	        setUsedSigns(p,1);            
	        getAmountSignsLeft(p);
	        
	        p.sendMessage(msg_prefix + "§aLike-Schild erstellt! Spieler können dir jetzt Likes geben.");
	        logToConsole(p.getName() + " created a Like-sign.");
	    }
	}

	/*	Like-Schild Interaction (kein Platzieren)
	 * 	
	 * 
	 */
	@EventHandler
	public void onPlayerSignInteract(PlayerInteractEvent event) {
	    if (event.getClickedBlock() == null) return;
	    
	    // nur Rechtsklick erlauben
	    if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return; 
	    
	    
	    /* Schild (-material) check
	     * 12 Materials mit je 4 Ausführungen
	     */
	    Material mat = event.getClickedBlock().getType();
	    if (mat != Material.OAK_SIGN && mat != Material.OAK_WALL_SIGN && mat != Material.OAK_HANGING_SIGN && mat != Material.OAK_WALL_HANGING_SIGN && 
	        mat != Material.SPRUCE_SIGN && mat != Material.SPRUCE_WALL_SIGN && mat != Material.SPRUCE_HANGING_SIGN && mat != Material.SPRUCE_WALL_HANGING_SIGN &&
	        mat != Material.BIRCH_SIGN && mat != Material.BIRCH_WALL_SIGN && mat != Material.BIRCH_HANGING_SIGN && mat != Material.BIRCH_WALL_HANGING_SIGN &&
	        mat != Material.JUNGLE_SIGN && mat != Material.JUNGLE_WALL_SIGN && mat != Material.JUNGLE_HANGING_SIGN && mat != Material.JUNGLE_WALL_HANGING_SIGN &&
	        mat != Material.ACACIA_SIGN && mat != Material.ACACIA_WALL_SIGN && mat != Material.ACACIA_HANGING_SIGN && mat != Material.ACACIA_WALL_HANGING_SIGN &&
	        mat != Material.DARK_OAK_SIGN && mat != Material.DARK_OAK_WALL_SIGN && mat != Material.DARK_OAK_HANGING_SIGN && mat != Material.DARK_OAK_WALL_HANGING_SIGN &&
	        mat != Material.MANGROVE_SIGN && mat != Material.MANGROVE_WALL_SIGN && mat != Material.MANGROVE_HANGING_SIGN && mat != Material.MANGROVE_WALL_HANGING_SIGN &&
	        mat != Material.CHERRY_SIGN && mat != Material.CHERRY_WALL_SIGN && mat != Material.CHERRY_HANGING_SIGN && mat != Material.CHERRY_WALL_HANGING_SIGN &&
			mat != Material.PALE_OAK_SIGN && mat != Material.PALE_OAK_WALL_SIGN && mat != Material.PALE_OAK_HANGING_SIGN && mat != Material.PALE_OAK_WALL_HANGING_SIGN &&
			mat != Material.BAMBOO_SIGN && mat != Material.BAMBOO_WALL_SIGN && mat != Material.BAMBOO_HANGING_SIGN && mat != Material.BAMBOO_WALL_HANGING_SIGN &&
			mat != Material.CRIMSON_SIGN && mat != Material.CRIMSON_WALL_SIGN && mat != Material.CRIMSON_HANGING_SIGN && mat != Material.CRIMSON_WALL_HANGING_SIGN &&
	        mat != Material.WARPED_SIGN &&  mat != Material.WARPED_WALL_SIGN && mat != Material.WARPED_HANGING_SIGN &&  mat != Material.WARPED_WALL_HANGING_SIGN) return;
	    
	    BlockState clickedBlock = event.getClickedBlock().getState();
	    if (!(clickedBlock instanceof Sign)) return; 					//Wall- und Hanging-Signs müssen nicht extra behandelt werden, sind auch typ sign
	    Sign sign = (Sign) event.getClickedBlock().getState();
	    
	    //flag correct?
	    if(!hasFlag(sign, "likeSign")) return;
	
	    event.setCancelled(true); // ????
	    Player clicker = event.getPlayer();
	
	    Location signLoc = sign.getLocation();
	    String locStr = locationToString(signLoc);
	    UUID ownerUUID = getSignOwnerUUID(sign);
	    
	    UUID clickerUUID = clicker.getUniqueId();
	
		// Eigenes Schild klicken Treatment
	    if (ownerUUID.equals(clickerUUID) && !allowSelfLike ) { //check if selflike allowed
	        clicker.sendMessage(msg_prefix + "§cDu kannst dein eigenes Schild nicht liken.");
	        return;
	    }
	    
	    // Zweifach Liken verhindern
	    List<String> clickedBy = getClickedByList(ownerUUID, locStr);
	    if (clickedBy.contains(clickerUUID.toString())) {
	        clicker.sendMessage(msg_prefix + "§cDu hast dieses Schild bereits geliked.");
	        return;
	    }
	
	    //Player Messages after LIKE
	    Player ownerPlayer = Bukkit.getPlayer(ownerUUID);
	        
	    //do Economy and message stuff
	    setLikeEconomy(clicker, ownerPlayer, likeAmount);
	    
	    //update givenLikes
	    setGivenLikes(clicker, likeAmount);
	    
	    setSignClickData(ownerPlayer, clicker, locStr);
	    
	    //Update Text on signs
	    updateLikesOnSign(sign, getLikesData(ownerPlayer, locStr), clicker);
	    
	    //Play Sound when LIKE
	    playLikeSound(clicker, ownerPlayer);
	}

	@EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
    	BlockState state = event.getBlock().getState();
        if (!(state instanceof Sign sign)) return;
        
        //flag correct?
        if(!hasFlag(sign, "likeSign")) return;
        
        // Standort als String 
        String locStr = locationToString(sign.getLocation());
        
        //get owner
	    UUID ownerUUID = getSignOwnerUUID(sign);
	    Player owner = Bukkit.getPlayer(ownerUUID);
	    
	    //Updates
	    setUsedSigns(owner, -1);
	    deleteSignData(sign);
	    getAmountSignsLeft(owner);
        logToConsole("[DEL] at " + locStr + " ("+Bukkit.getPlayer(ownerUUID).getName()+") " + "from player " + event.getPlayer().getName() + ", data deleted.");
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
            	
                /*Unterschiedung nach Blocktype
                 * WallSign
                 * HangingSign
                 * WallHangingSign
                 * stehendes Schild (kein eigener Typ)
                 */
                String signType = switch (data){                
	                case WallSign ignored -> "WALL_SIGN";
	                case org.bukkit.block.data.type.HangingSign ignored -> "HANGING_SIGN";
	                case org.bukkit.block.data.type.WallHangingSign ignored -> "WALL_HANGING_SIGN";
	                default -> "STANDING_SIGN";                            	
                };
                
                switch(signType) {
	                case "WALL_SIGN" -> {
	                	BlockFace attachedFace = ((WallSign) data).getFacing(); // BlockFace des Schilds
	            	    if (attachedFace.equals(face)) {
	            	        // Der zerstörte Block ist die Rückwand dieses Schilds
	            	        cancelBlockBreakNearSign(event);
	            	        return;
	            	    }
	                }	                
	                case "HANGING_SIGN" -> {
	                	if (face == BlockFace.DOWN) {
	                		cancelBlockBreakNearSign(event);
	                        return;
	                    }
	                }
	                case "WALL_HANGING_SIGN" -> {
	                	// do nothing, block does not drop :)
	                    return;
	                }
	                case "STANDING_SIGN" -> {
	                	if (face == BlockFace.UP) {
	                		cancelBlockBreakNearSign(event);
	                        return;
	                    }
	                }
                }                
            }
        }

        // kein Schild in der Nähe: Abbau erlaubt
    }

	private void cancelBlockBreakNearSign(BlockBreakEvent event) {
		event.setCancelled(true);
	    event.getPlayer().sendMessage(msg_prefix + "§cDu kannst dieses Like-Schild nicht indirekt zerstören. Baue es direkt ab.");
	}
}
