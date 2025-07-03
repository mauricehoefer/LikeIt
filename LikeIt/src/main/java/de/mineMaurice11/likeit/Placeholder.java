package de.mineMaurice11.likeit;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;

import java.io.File;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

public class Placeholder extends PlaceholderExpansion {

    private final JavaPlugin plugin;
    private final LikeItPlugin likeitplugin;

    public Placeholder(JavaPlugin plugin, LikeItPlugin likeitplugin) {
        this.plugin = plugin;
        this.likeitplugin = likeitplugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "likeit";
    }

    @Override
    public @NotNull String getAuthor() {
        return "mineMaurice11";
    }

    @Override
    public @NotNull String getVersion() {
        return "1.0";
    }

    @Override
    public String onPlaceholderRequest(Player p, String identifier) {
        if (p == null) return "";

        // singsLeft
    	if (identifier.equals("signsleft")) {
        	int signsLeft = likeitplugin.getAmountSignsLeft(p);
        	return Integer.toString(signsLeft);
        }
    	
    	// givenLikes
    	if (identifier.equals("givenlikes")) {
        	int givenLikes = likeitplugin.getGivenLikes(p);
        	return Integer.toString(givenLikes);
        }
    	
        return null;
    }
}
