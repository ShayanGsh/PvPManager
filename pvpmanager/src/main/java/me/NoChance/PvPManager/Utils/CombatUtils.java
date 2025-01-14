package me.NoChance.PvPManager.Utils;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.command.CommandException;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.AreaEffectCloud;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.entity.EntityCombustByEntityEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.projectiles.ProjectileSource;

import me.NoChance.PvPManager.Settings.Messages;
import me.NoChance.PvPManager.Settings.Settings;
import me.chancesd.pvpmanager.utils.ScheduleUtils;

public final class CombatUtils {

	private static final List<String> harmfulPotions = new ArrayList<>();
	private static final DecimalFormat decimalFormat = new DecimalFormat();

	private CombatUtils() {
	}

	static {
		harmfulPotions.add("SLOW");
		harmfulPotions.add("SLOW_DIGGING");
		harmfulPotions.add("CONFUSION");
		harmfulPotions.add("BLINDNESS");
		harmfulPotions.add("HUNGER");
		harmfulPotions.add("WEAKNESS");
		harmfulPotions.add("POISON");
		harmfulPotions.add("WITHER");
		harmfulPotions.add("GLOWING");
		harmfulPotions.add("LEVITATION");
		harmfulPotions.add("UNLUCK");
		harmfulPotions.add("BAD_OMEN");
		decimalFormat.setMaximumFractionDigits(2);
	}

	public static String formatTo2Digits(final double value) {
		return decimalFormat.format(value);
	}

	public static boolean hasTimePassed(final long toggleTime, final int cooldown) {
		return hasTimePassedMs(toggleTime, cooldown * 1000L);
	}

	public static boolean hasTimePassedMs(final long toggleTime, final long cooldown) {
		return System.currentTimeMillis() - toggleTime >= cooldown;
	}

	public static int getTimeLeft(final long startTime, final int time) {
		return (int) (getTimeLeftMs(startTime, time * 1000L) / 1000);
	}

	public static long getTimeLeftMs(final long startTime, final long time) {
		return startTime + time - System.currentTimeMillis();
	}

	public static void executeCommands(final List<String> commands, final Player player, final String playerName, final String victim) {
		for (final String command : commands) {
			try {
				final String preparedCommand = command.replace("<player>", playerName).replace("<victim>", victim).replace("%p", playerName);
				if (preparedCommand.toLowerCase().startsWith("!console")) {
					ScheduleUtils.executeConsoleCommand(preparedCommand.substring(9));
				} else if (preparedCommand.toLowerCase().startsWith("!player")) {
					player.performCommand(preparedCommand.substring(8));
				} else {
					ScheduleUtils.executeConsoleCommand(preparedCommand);
				}
			} catch (final CommandException e) {
				Log.warning("Error executing command: \"" + command + "\" for player: " + playerName);
				Log.warning("This error comes from the command and it's respective plugin below:");
				Log.warning(e.getMessage(), e);
			}
		}
	}

	public static void executeCommands(final List<String> commands, final Player player, final String playerName) {
		executeCommands(commands, player, playerName, "");
	}

	public static final boolean isPvP(final EntityDamageByEntityEvent event) {
		final Entity attacker = event.getDamager();
		final Entity defender = event.getEntity();

		if (defender instanceof Player && !isNPC(defender)) {
			if (attacker instanceof Player && !isNPC(attacker))
				return true;
			if (attacker instanceof Projectile || CombatUtils.isVersionAtLeast(Settings.getMinecraftVersion(), "1.9") && attacker instanceof AreaEffectCloud) {
				final ProjectileSource projSource = getSource(attacker);
				if (projSource instanceof Player) {
					final Entity shooter = (Entity) projSource;
					if (Settings.isSelfTag() || !shooter.equals(defender) && !isNPC(shooter))
						return !Settings.isIgnoreNoDamageHits() || event.getDamage() != 0;
				}
			}
			if (attacker instanceof TNTPrimed) {
				final TNTPrimed tnt = (TNTPrimed) attacker;
				final Entity tntAttacker = tnt.getSource();
				if (tntAttacker instanceof Player && (Settings.isSelfTag() || !tntAttacker.equals(defender))) {
					return true;
				}
			}
		}

		return false;
	}

	public static final boolean isPvP(final EntityCombustByEntityEvent event) {
		final Entity attacker = event.getCombuster();
		final Entity defender = event.getEntity();

		if (defender instanceof Player && !isNPC(defender)) {
			if (attacker instanceof Player && !isNPC(attacker))
				return true;
			if (attacker instanceof Projectile) {
				final ProjectileSource projSource = ((Projectile) attacker).getShooter();
				if (projSource instanceof Player) {
					final Entity shooter = (Entity) projSource;
					return !shooter.equals(defender) && !isNPC(shooter);
				}
			}
		}

		return false;
	}

	public static final boolean isNPC(final Entity entity) {
		return entity.hasMetadata("NPC");
	}

	public static boolean canFly(final Player p) {
		return p.isFlying() || p.getAllowFlight();
	}

	public static void checkGlide(final Player p) {
		if (p.isGliding()) {
			p.setGliding(false);
			p.teleport(p.getLocation());
			if (!Settings.isBorderPushbackTakeElytra())
				return;
			final ItemStack chestplate = p.getInventory().getChestplate();
			if (chestplate == null || chestplate.getType() != Material.ELYTRA)
				return;
			p.getInventory().setChestplate(null);
			final Map<Integer, ItemStack> item = p.getInventory().addItem(chestplate);
			if (!item.isEmpty())
				p.getWorld().dropItemNaturally(p.getLocation(), item.values().stream().findFirst().orElse(chestplate));
		}
	}

	public static void fakeItemStackDrop(final Player player, final ItemStack[] inventory) {
		final Location playerLocation = player.getLocation();
		final World playerWorld = player.getWorld();
		for (final ItemStack itemstack : inventory) {
			if (itemstack != null && !itemstack.getType().equals(Material.AIR)) {
				playerWorld.dropItemNaturally(playerLocation, itemstack);
			}
		}
	}

	public static boolean isOnline(final String name) {
		return Bukkit.getPlayer(name) != null;
	}

	public static boolean isOnlineWithFeedback(final CommandSender sender, final String name) {
		if (!isOnline(name)) {
			sender.sendMessage(Messages.getErrorPlayerNotFound().replace("%p", name));
			return false;
		}
		return true;
	}

	public static boolean isOnline(final UUID id) {
		return Bukkit.getPlayer(id) != null;
	}

	public static boolean isReal(final UUID id) {
		return Bukkit.getPlayer(id) != null;
	}

	public static boolean isWorldExcluded(final String worldName) {
		return Settings.getWorldsExcluded().contains(worldName);
	}

	public static boolean isHarmfulPotion(final PotionEffectType type) {
		return harmfulPotions.contains(type.getName());
	}

	public static boolean recursiveContainsCommand(final String[] givenCommand, final List<String> list) {
		boolean contains = false;
		for (int i = 0; i < givenCommand.length; i++) {
			String args = givenCommand[0];
			for (int j = 1; j <= i; j++) {
				args += " " + givenCommand[j];
			}
			if (list.contains(args.toLowerCase())) {
				contains = true;
				break;
			}
		}
		return contains;
	}

	public static final boolean isVersionAtLeast(final String v1, final String v2) {
		if (v1.equals(v2))
			return true;

		final String[] v1Array = v1.split("\\.");
		final String[] v2Array = v2.split("\\.");
		final int length = Math.max(v2Array.length, v1Array.length);
		try {
			for (int i = 0; i < length; i++) {
				final int x = i < v2Array.length ? Integer.parseInt(v2Array[i]) : 0;
				final int y = i < v1Array.length ? Integer.parseInt(v1Array[i]) : 0;
				if (y > x)
					return true;
				if (y < x)
					return false;
			}
		} catch (final NumberFormatException ex) {
			Log.severe("Error reading version number! Comparing " + v1 + " to " + v2);
		}
		return true;
	}

	public static String stripTags(final String version) {
		return version.replaceAll("[-;+].+", "");
	}

	private static ProjectileSource getSource(final Entity entity) {
		if (entity instanceof Projectile)
			return ((Projectile) entity).getShooter();
		else
			return ((AreaEffectCloud) entity).getSource();
	}

}
