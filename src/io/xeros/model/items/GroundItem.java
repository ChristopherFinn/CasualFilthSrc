package io.xeros.model.items;

import com.google.common.base.Preconditions;
import io.xeros.content.instances.InstancedArea;
import io.xeros.model.entity.player.Player;
import io.xeros.model.entity.player.Position;
import io.xeros.model.entity.player.mode.group.GroupIronmanGroup;
import io.xeros.model.entity.player.mode.group.GroupIronmanRepository;

/**
 * Represents an item on the ground at a specific location on the map.
 * 
 * @author Jason MacKeigan
 * @date Feb 10, 2015, 4:08:14 PM
 */
public class GroundItem {

	/**
	 * The identification value of the item that makes it unique from the rest
	 */
	private final int id;

	/**
	 * The location of the item on the x-axis
	 */
	private final int x;

	/**
	 * The location of the item on the y-axis
	 */
	private final int y;

	/**
	 * The height level of the item on the ground
	 */
	private final int height;

	/**
	 * The amount of the item.
	 */
	private final int amount;

	public int globalisationTicks;

	public int removeTicks;

	public String ownerName;
	
	private InstancedArea instance;

	private boolean globalise = true;

	/**
	 * Creates a new {@link GroundItem} object on the x, y, and z-axis.
	 * 
	 * @param id the identification value of the item
	 * @param x the x location
	 * @param y the y location
	 * @param height the height on the map
	 * @param amount the amount of the item
	 * @param globalisationTicks the amount of ticks until hidden
	 * @param name the name of the owner
	 */
	public GroundItem(int id, int x, int y, int height, int amount, int globalisationTicks, String name) {
		Preconditions.checkState(name.equals(name.toLowerCase()), "name must be lowercase.");
		this.id = id;
		this.x = x;
		this.y = y;
		this.height = height;
		this.amount = amount;
		this.globalisationTicks = globalisationTicks;
		this.ownerName = name;
	}

	public boolean isViewable(Player player) {
		boolean ironman = !player.getMode().isItemScavengingPermitted();
		boolean isGroupIronman = player.isApartOfGroupIronmanGroup();
		return isOwner(player) || isGroupIronman && checkForGroupMember(player, getOwnerName()) || !ironman && isGlobal();
	}

	public boolean checkForGroupMember(Player player, String dropOwner) {
		if (dropOwner == null)
			return false;
		GroupIronmanGroup group = GroupIronmanRepository.getGroupForOnline(player).orElse(null);
		if (group == null)
			return false;
		return group.isGroupMember(dropOwner);
	}

	public boolean inDistance(Player player) {
		return player.distanceToPoint(getX(), getY()) <= 60;
	}

	public boolean isVisible(Player player) {
		if (!isViewable(player))
			return false;
		return getInstance() == player.getInstance()
				&& player.getHeight() == getHeight()
				&& inDistance(player);
	}

	public boolean isOwner(Player player) {
		return player.getLoginNameLower().equals(getOwnerName());
	}

	public boolean canGlobalise() {
		return isGlobalise() && GameItem.isTradeable(getId());
	}

	public boolean isGlobal() {
		return globalisationTicks == 0 && canGlobalise();
	}

	@Override
	public String toString() {
		return "GroundItem{" +
				"id=" + id +
				", position=" + new Position(x, y, height).toString() +
				", ownerName='" + ownerName + '\'' +
				", instance=" + instance +
				'}';
	}

	public Position getPosition() {
		return new Position(x, y, height);
	}

	/**
	 * The item identification value
	 * 
	 * @return the item id
	 */
	public int getId() {
		return this.id;
	}

	/**
	 * Retrieves the absolute x position of the item on the map
	 * 
	 * @return the x position
	 */
	public int getX() {
		return x;
	}

	/**
	 * Retrieves the absolute y position of the item on the map
	 * 
	 * @return the y position
	 */
	public int getY() {
		return y;
	}

	/**
	 * The amount of the item that exists at this position
	 * 
	 * @return the amount of the item
	 */
	public int getAmount() {
		return amount;
	}

	/**
	 * Item name.
	 * 
	 * @return
	 */
	public String getOwnerName() {
		return this.ownerName;
	}

	/**
	 * The ground item must be displayed on a height level. The ground item can only appear on the height level its created on.
	 * 
	 * @return the height level of the ground item
	 */
	public int getHeight() {
		return height;
	}

	public InstancedArea getInstance() {
		return instance;
	}

	public GroundItem setInstance(InstancedArea instance) {
		this.instance = instance;
		return this;
	}

	public boolean isGlobalise() {
		return globalise;
	}

	public void setGlobalise(boolean globalise) {
		this.globalise = globalise;
	}
}