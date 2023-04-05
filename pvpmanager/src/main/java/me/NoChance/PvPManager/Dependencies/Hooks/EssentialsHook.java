package me.NoChance.PvPManager.Dependencies.Hooks;

import org.bukkit.entity.Player;

import com.earth2me.essentials.Essentials;

import me.NoChance.PvPManager.Dependencies.Hook;
import me.NoChance.PvPManager.Dependencies.API.BaseDependency;
import me.NoChance.PvPManager.Dependencies.API.GodDependency;

public class EssentialsHook extends BaseDependency implements GodDependency {

	private final Essentials ess;

	public EssentialsHook(final Hook hook) {
		super(hook);
		this.ess = (Essentials) hook.getPlugin();
	}

	@Override
	public boolean hasGodMode(final Player player) {
		return ess.getUser(player).isGodModeEnabled();
	}

	@Override
	public void enableGodMode(final Player player) {
		ess.getUser(player).setGodModeEnabled(true);
	}

	@Override
	public void disableGodMode(final Player player) {
		ess.getUser(player).setGodModeEnabled(false);
	}

}
