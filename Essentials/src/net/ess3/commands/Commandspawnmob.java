package net.ess3.commands;

import java.util.HashSet;
import java.util.Locale;
import java.util.Random;
import java.util.Set;
import static net.ess3.I18n._;
import net.ess3.api.ISettings;
import net.ess3.api.IUser;
import net.ess3.bukkit.LivingEntities;
import net.ess3.bukkit.LivingEntities.MobException;
import net.ess3.permissions.Permissions;
import net.ess3.utils.LocationUtil;
import net.ess3.utils.Util;
import org.bukkit.DyeColor;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.*;
import org.bukkit.entity.Villager.Profession;
import org.bukkit.material.Colorable;


public class Commandspawnmob extends EssentialsCommand
{
	@Override
	public void run(final IUser user, final String commandLabel, final String[] args) throws Exception
	{
		if (args.length < 1)
		{
			final Set<String> mobList = LivingEntities.getLivingEntityList();
			final Set<String> availableList = new HashSet<String>();
			for (String mob : mobList)
			{
				if (Permissions.SPAWNMOB.isAuthorized(user, mob))
				{
					availableList.add(mob);
				}
			}
			if (availableList.isEmpty())
			{
				availableList.add(_("none"));
			}
			throw new NotEnoughArgumentsException(_("mobsAvailable", Util.joinList(availableList)));
		}


		final String[] mountparts = args[0].split(",");
		String[] parts = mountparts[0].split(":");
		String mobType = parts[0];
		String mobData = null;
		if (parts.length == 2)
		{
			mobData = parts[1];
		}
		String mountType = null;
		String mountData = null;
		if (mountparts.length > 1)
		{
			parts = mountparts[1].split(":");
			mountType = parts[0];
			if (parts.length == 2)
			{
				mountData = parts[1];
			}
		}


		Entity spawnedMob = null;
		EntityType mob = null;
		Entity spawnedMount = null;
		EntityType mobMount = null;

		mob = LivingEntities.fromName(mobType);
		if (mob == null)
		{
			throw new Exception(_("invalidMob"));
		}

		if (!Permissions.SPAWNMOB.isAuthorized(user, mob.getName()))
		{
			throw new Exception(_("noPermToSpawnMob"));
		}

		final Block block = LocationUtil.getTarget(user.getPlayer()).getBlock();
		if (block == null)
		{
			throw new Exception(_("unableToSpawnMob"));
		}
		IUser otherUser = null;
		if (args.length >= 3)
		{
			otherUser = ess.getUserMap().getUser(args[2]);
		}
		final Location loc = (otherUser == null) ? block.getLocation() : otherUser.getPlayer().getLocation();
		final Location sloc = LocationUtil.getSafeDestination(loc);
		try
		{
			spawnedMob = user.getPlayer().getWorld().spawn(sloc, (Class<? extends LivingEntity>)mob.getEntityClass());
		}
		catch (Exception e)
		{
			throw new Exception(_("unableToSpawnMob"), e);
		}

		if (mountType != null)
		{
			mobMount = LivingEntities.fromName(mountType);
			if (mobMount == null)
			{
				user.sendMessage(_("invalidMob"));
				return;
			}

			if (!Permissions.SPAWNMOB.isAuthorized(user, mobMount.getName()))
			{
				throw new Exception(_("noPermToSpawnMob"));
			}
			try
			{
				spawnedMount = user.getPlayer().getWorld().spawn(loc, (Class<? extends LivingEntity>)mobMount.getEntityClass());
			}
			catch (Exception e)
			{
				throw new Exception(_("unableToSpawnMob"), e);
			}
			spawnedMob.setPassenger(spawnedMount);
		}
		if (mobData != null)
		{
			changeMobData(mob, spawnedMob, mobData, user);
		}
		if (spawnedMount != null && mountData != null)
		{
			changeMobData(mobMount, spawnedMount, mountData, user);
		}
		if (args.length >= 2)
		{
			int mobCount = Integer.parseInt(args[1]);

			ISettings settings = ess.getSettings();

			int serverLimit = settings.getData().getCommands().getSpawnmob().getLimit();

			if (mobCount > serverLimit)
			{
				mobCount = serverLimit;
				user.sendMessage(_("mobSpawnLimit"));
			}

			try
			{
				for (int i = 1; i < mobCount; i++)
				{
					spawnedMob = user.getPlayer().getWorld().spawn(sloc, (Class<? extends LivingEntity>)mob.getEntityClass());
					if (mobMount != null)
					{
						try
						{
							spawnedMount = user.getPlayer().getWorld().spawn(loc, (Class<? extends LivingEntity>)mobMount.getEntityClass());
						}
						catch (Exception e)
						{
							throw new Exception(_("unableToSpawnMob"), e);
						}
						spawnedMob.setPassenger(spawnedMount);
					}
					if (mobData != null)
					{
						changeMobData(mob, spawnedMob, mobData, user);
					}
					if (spawnedMount != null && mountData != null)
					{
						changeMobData(mobMount, spawnedMount, mountData, user);
					}
				}
				user.sendMessage(mobCount + " " + mob.getName().toLowerCase(Locale.ENGLISH) + " " + _("spawned"));
			}
			catch (MobException e1)
			{
				throw new Exception(_("unableToSpawnMob"), e1);
			}
			catch (NumberFormatException e2)
			{
				throw new Exception(_("numberRequired"), e2);
			}
			catch (NullPointerException np)
			{
				throw new Exception(_("soloMob"), np);
			}
		}
		else
		{
			user.sendMessage(mob.getName() + " " + _("spawned"));
		}
	}

	private void changeMobData(final EntityType type, final Entity spawned, String data, final IUser user) throws Exception
	{
		data = data.toLowerCase(Locale.ENGLISH);

		if (spawned instanceof Slime)
		{
			try
			{
				((Slime)spawned).setSize(Integer.parseInt(data));
			}
			catch (Exception e)
			{
				throw new Exception(_("slimeMalformedSize"), e);
			}
		}
		if (spawned instanceof Ageable && data.contains("baby"))
		{
			((Ageable)spawned).setBaby();
			return;
		}
		if (spawned instanceof Colorable)
		{
			final String color = data.toUpperCase(Locale.ENGLISH).replace("BABY", "");
			try
			{
				if (color.equals("RANDOM"))
				{
					final Random rand = new Random();
					((Colorable)spawned).setColor(DyeColor.values()[rand.nextInt(DyeColor.values().length)]);
				}
				else
				{
					((Colorable)spawned).setColor(DyeColor.valueOf(color));
				}
			}
			catch (Exception e)
			{
				throw new Exception(_("sheepMalformedColor"), e);
			}
		}
		if (spawned instanceof Tameable && data.contains("tamed"))
		{
			final Tameable tameable = ((Tameable)spawned);
			tameable.setTamed(true);
			tameable.setOwner(user.getPlayer());
		}
		if (type == EntityType.WOLF
			&& data.contains("angry"))
		{
			((Wolf)spawned).setAngry(true);
		}
		if (type == EntityType.CREEPER && data.contains("powered"))
		{
			((Creeper)spawned).setPowered(true);
		}
		if (type == EntityType.OCELOT)
		{
			if (data.contains("siamese"))
			{
				((Ocelot)spawned).setCatType(Ocelot.Type.SIAMESE_CAT);
			}
			else if (data.contains("red"))
			{
				((Ocelot)spawned).setCatType(Ocelot.Type.RED_CAT);
			}
			else if (data.contains("black"))
			{
				((Ocelot)spawned).setCatType(Ocelot.Type.BLACK_CAT);
			}
		}
		if (type == EntityType.VILLAGER)
		{
			for (Profession prof : Villager.Profession.values())
			{
				if (data.contains(prof.toString().toLowerCase(Locale.ENGLISH)))
				{
					((Villager)spawned).setProfession(prof);
				}
			}
		}
	}
}
