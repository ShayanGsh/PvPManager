package me.NoChance.PvPManager.Dependencies.API;

import org.bukkit.plugin.java.JavaPlugin;

import me.NoChance.PvPManager.Dependencies.Hook;

public interface Dependency {

	public String getName();

	public Hook getHook();

	public JavaPlugin getPlugin();

	default String onEnableMessage() {
		return getName() + " Found! Hooked successfully";
	}

}
