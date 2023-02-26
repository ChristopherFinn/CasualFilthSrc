package io.xeros.model.entity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;
import io.xeros.content.combat.Damage;
import io.xeros.content.combat.Hitmark;
import io.xeros.content.instances.InstancedArea;
import io.xeros.content.minigames.raids.Raids;
import io.xeros.content.minigames.xeric.Xeric;
import io.xeros.content.tournaments.TourneyManager;
import io.xeros.model.*;
import io.xeros.model.collisionmap.RegionProvider;
import io.xeros.model.collisionmap.doors.Location;
import io.xeros.model.entity.npc.NPC;
import io.xeros.model.entity.npc.NPCClipping;
import io.xeros.model.entity.npc.stats.NpcBonus;
import io.xeros.model.entity.npc.stats.NpcCombatDefinition;
import io.xeros.model.entity.player.Boundary;
import io.xeros.model.entity.player.Player;
import io.xeros.model.entity.player.Position;
import io.xeros.model.timers.TickTimer;
import io.xeros.util.Misc;
import io.xeros.util.Stream;
import static io.xeros.model.entity.npc.NPCClipping.DIR;

/**
 * Represents a game-world presence that exists among others of the same nature.
 * The objective is to allow multiple entities to share common similarities and
 * allow simple but effective reference in doing so.
 *
 * @author Jason MacKeigan
 * @date Mar 27, 2015, 2015, 8:00:45 PM
 */
public abstract class Entity {
    /**
     * The index in the list that the player resides
     */
    protected int index;
    /**
     * A mapping of all damage that has been taken by other entities in the game
     */
    protected Map<Entity, List<Damage>> damageTaken = new HashMap<>();
    /**
     * The {@link Entity} that has been determined the killer of this {@link Entity}
     */
    protected Entity killer;
    /**
     * The health of the entity
     */
    protected Health health;
    private InstancedArea instance;
    private Raids raidsInstance;
    private Xeric xeric;
    protected Hitmark hitmark1;
    protected Hitmark hitmark2;
    public EntityReference frozenBy;
    public int freezeTimer;
    public boolean isDead;
    public int attackTimer;
    public int hitDiff2;
    public int hitDiff;
    public boolean hitUpdateRequired2;
    public boolean hitUpdateRequired;
    private boolean animationUpdateRequired;
    private boolean updateRequired = true;
    private Animation animation;
    private boolean gfxUpdateRequired;
    private Graphic graphic;
    private boolean invisible;

    /**
     * The timer associated with the animation of the Entity
     */
    private final TickTimer animationTimer = new TickTimer();

    /**
     * Attribute class for easy caching of variables.
     */
    private final Attributes attributes = new Attributes();

    /**
     * Sends some information to the Stream regarding a possible new hit on the
     * entity.
     *
     * @param str
     *            the stream for the entity
     */
    protected abstract void appendHitUpdate(Stream str);

    /**
     * Sends some information to the Stream regarding a possible new hit on the
     * entity.
     *
     * @param str
     *            the stream for the entity
     */
    protected abstract void appendHitUpdate2(Stream str);

    /**
     * Used to append some amount of damage to the entity and inflict on their total
     * amount of health. The method will also display a new hitmark on that entity.
     *
     * @param entity
     * @param damage
     *            the damage dealt
     * @param hitmark
     */
    public abstract void appendDamage(Entity entity, int damage, Hitmark hitmark);

    public abstract boolean isFreezable();

    /**
     * Determines if the entity is susceptible to a status based on their nature.
     * For example some players when wearing certain equipment are exempt from venom
     * or poison status. In other situations, NPC's are susceptible to venom.
     *
     * @param status
     *            the status the entity may not be susceptible to
     * @return {code true} if the entity is not susceptible to a particular status
     */
    public abstract boolean susceptibleTo(HealthStatus status);

    /**
     * Remove from current instance.
     */
    public abstract void removeFromInstance();

    /**
     * Get the entity size (x by x)
     */
    public abstract int getEntitySize();

    /**
     * The x-position on the map where the entity exists. This is on the x-axis
     *
     * @return the x
     */
    public abstract int getX();

    /**
     * Modifies the x-position of the entity.
     *
     * @param x
     *            the new position
     */
    public abstract void setX(int x);

    /**
     * The y-position on the map where the entity exists. This is on the y-axis.
     *
     * @return the y-position
     */
    public abstract int getY();

    /**
     * Modifies the y-position of the entity.
     *
     * @param y
     *            the new position
     */
    public abstract void setY(int y);

    /**
     * The height level of the entity.
     *
     * @return the height
     */
    public abstract int getHeight();

    /**
     * Modifies the height of the entity.
     *
     * @param height
     *            the new height
     */
    public abstract void setHeight(int height);

    public abstract void resetWalkingQueue();

    public abstract int getDefenceLevel();

    public abstract int getDefenceBonus(CombatType combatType, Entity attacker);

    public abstract boolean hasBlockAnimation();

    public abstract Animation getBlockAnimation();

    public abstract boolean isAutoRetaliate();

    public abstract void attackEntity(Entity entity);

    /**
     * Creates a new {@link Entity} object with a specified index value representing
     * where in their respective array they reside. An {@link Entity} is an object
     * that exists within the game-world. A {@link Player} or {@link NPC} are all
     * examples of entities.
     *
     * @param index
     *            the index in the list where this {@link Entity} resides.
     */
    public Entity(int index) {
        this.index = index;
    }

    /**
     * Creates a new {@link Entity}.
     */
    public Entity() { }

    public boolean isRegistered() {
        return getIndex() > 0;
    }

    public abstract void appendHeal(int amount, Hitmark mark);

    public void appendDamage(int damage, Hitmark hitmark) {
        appendDamage(null, damage, hitmark);
    }

    public int lastDamageTaken;

    public void startAnimation(Animation animation) {
        if (this.animation == null || animation.getAnimationPriority().compareTo(this.animation.getAnimationPriority()) > 0) {
            this.animation = animation;
            setUpdateRequired(true);
            animationUpdateRequired = true;
        }
    }

    public void startGraphic(Graphic graphic) {
        if (this.graphic == null || graphic.getAnimationPriority().compareTo(this.graphic.getAnimationPriority()) >= 0) {
            this.graphic = graphic;
            setUpdateRequired(true);
            gfxUpdateRequired = true;
        }
    }

    public void resetAfterUpdate() {
        animation = null;
        animationUpdateRequired = false;
        graphic = null;
        gfxUpdateRequired = false;
        setUpdateRequired(false);
    }

    public double getDistance(Position source, int x, int y) {
        double low = 9999;
        if (insideOf(x, y)) return 0;
        for (Position p : getBorder(source)) {
            double dist = Misc.distance(x, y, p.getX(), p.getY());
            if (dist < low) {
                low = dist;
            }
        }
        return low;
    }

    /**
     * Get the distance between the provided and coordinates and the
     * closest tile within this entity's {@link Entity#getBorder()}.
     */
    public double getDistance(int x, int y) {
        double low = 9999;
        if (insideOf(x, y)) return 0;
        for (Position p : getBorder()) {
            double dist = Misc.distance(x, y, p.getX(), p.getY());
            if (dist < low) {
                low = dist;
            }
        }
        return low;
    }

    public double distance(Position position) {
        double low = 9999;
        if (insideOf(position)) return 0;
        for (Position p : getBorder()) {
            double dist = Misc.distance(position.getX(), position.getY(), p.getX(), p.getY());
            if (dist < low) {
                low = dist;
            }
        }
        return low;
    }

    public boolean insideOf(int x, int y) {
        return insideOf(new Position(x, y));
    }

    public boolean insideOf(Position position) {
        return Arrays.stream(getTiles()).anyMatch(p -> p.getX() == position.getX() && p.getY() == position.getY());
    }

    public Position[] getTiles() {
        return getTiles(getPosition(), getEntitySize());
    }

    public Position[] getTiles(Position position, int size) {
        Position[] tiles = new Position[getEntitySize() == 1 ? 1 : (int) Math.pow(size, 2)];
        int index = 0;
        for (int i = 1; i < size + 1; i++) {
            for (int k = 0; k < NPCClipping.SIZES[i].length; k++) {
                int x3 = position.getX() + NPCClipping.SIZES[i][k][0];
                int y3 = position.getY() + NPCClipping.SIZES[i][k][1];
                tiles[index] = new Position(x3, y3, getHeight());
                index++;
            }
        }
        return tiles;
    }

    public Position[] getBorder() {
        return getBorder(getPosition());
    }

    /**
     * The border is the outer most ring of an npc. For a size one npc it's one tile,
     * for a size two npcs it's every tile, for a size 3 npc it includes every tile on
     * the outside edge.
     * @param source The position of the entity.
     */
    public Position[] getBorder(Position source) {
        int x = source.getX();
        int y = source.getY();
        int size = getEntitySize();
        if (size <= 1) {
            return new Position[] {new Position(source.getX(), source.getY())};
        }
        Position[] border = new Position[(size) + (size - 1) + (size - 1) + (size - 2)];
        int j = 0;
        border[0] = new Position(x, y);
        for (int i = 0; i < 4; i++) {
            for (int k = 0; k < (i < 3 ? (i == 0 || i == 2 ? size : size) - 1 : (i == 0 || i == 2 ? size : size) - 2); k++) {
                if (i == 0) x++;
                 else if (i == 1) y++;
                 else if (i == 2) x--;
                 else if (i == 3) {
                    y--;
                }
                border[(++j)] = new Position(x, y);
            }
        }
        return border;
    }

    public Position getAdjacentPosition() {
        for (int index = 0; index < DIR.length; index++) {
            if (getRegionProvider().canMove(getX(), getY(), getHeight(), index, this.isNPC())) {
                return new Position(getX() + DIR[index][0], getY() + DIR[index][1], getHeight());
            }
        }
        return null;
    }

    public Position getAdjacentPosition(Position... except) {
        for (int index = 0; index < DIR.length; index++) {
            Position position = new Position(getX() + DIR[index][0], getY() + DIR[index][1], getHeight());
            if (getRegionProvider().canMove(getX(), getY(), getHeight(), index, this.isNPC()) && Arrays.stream(except).noneMatch(position::equals)) {
                return position;
            }
        }
        return null;
    }

    /**
     * Get a position along an entity's border that is closest to the provided position.
     */
    public Position getAdjacentBorderPosition(Position position) {
        Position[] tiles = getBorder();
        double lowDist = 999;
        Position lowTile = null;
        for (Position tile : tiles) {
            double dist = tile.getAbsDistance(position);
            if (lowTile == null || dist < lowDist) {
                lowDist = dist;
                lowTile = tile;
            }
        }

        return lowTile;
    }

    public Position getCenterPosition() {
        Position position = getPosition();
        if (getEntitySize() > 2) {
            int deltax = (int) Math.ceil((double) getEntitySize() / 3.0);
            int deltay = (int) Math.ceil((double) getEntitySize() / 3.0);
            position = new Position(getPosition().getX() + deltax, getPosition().getY() + deltay, getPosition().getHeight());
        }
        return position;
    }

    public int distanceToPoint(int pointX, int pointY) {
        return (int) Math.sqrt(Math.pow(getX() - pointX, 2) + Math.pow(getY() - pointY, 2));
    }

    public boolean sameInstance(Entity other) {
        if (this.instance == null) {
            return other.instance == null;
        } else if (other.instance != null) {
            return other.instance == this.instance;
        }
        return false;
    }

    public boolean collides(int x, int y, int size, int targetX, int targetY, int targetSize) {
        int distanceX = x - targetX;
        int distanceY = x - targetY;
        return distanceX < targetSize && distanceX > -size && distanceY < targetSize && distanceY > -size;
    }

    /**
     * When an entity dies it is paramount that we know who dealt the most damage to
     * that entity so that we can determine who will receive the drop.
     *
     * @return the {@link Entity} that dealt the most damage to this {@link Entity}.
     */
    public Entity calculateTourneyKiller() {
        final long VALID_TIMEFRAME = TimeUnit.SECONDS.toMillis(90);
        Entity killer = null;
        int totalDamage = 0;
        for (Entry<Entity, List<Damage>> entry : damageTaken.entrySet()) {
            Entity tempKiller = entry.getKey();
            List<Damage> damageList = entry.getValue();
            int damage = 0;
            if (tempKiller == null) {
                continue;
            }
            if (tempKiller instanceof Player) {
                if (!TourneyManager.getSingleton().isInArena((Player) tempKiller)) {
                    continue;
                }
                for (Damage d : damageList) {
                    if (System.currentTimeMillis() - d.getTimestamp() < VALID_TIMEFRAME) {
                        damage += d.getAmount();
                    }
                }
                if (tempKiller.isPlayer()) {
                    Player p = tempKiller.asPlayer();
                    if (p.isDisconnected() || p.getSession() == null || !p.getSession().isActive()) continue;
                    if (this.getRaidsInstance() != null) {
                        if (p.getRaidsInstance() == null || p.getRaidsInstance() != this.getRaidsInstance()) continue;
                    }
                }
                if (totalDamage == 0 || damage > totalDamage || killer == null) {
                    totalDamage = damage;
                    killer = tempKiller;
                }
            }
        }
        return killer;
    }

    /**
     * Adds some damage value to the entities list of taken damage
     *
     * @param entity
     *            the entity that dealt the damage
     * @param damage
     *            the total damage taken
     */
    public void addDamageTaken(Entity entity, int damage) {
        if (entity == null || damage <= 0) {
            return;
        }
        Damage combatDamage = new Damage(damage);
        if (damageTaken.containsKey(entity)) {
            damageTaken.get(entity).add(new Damage(damage));
        } else {
            damageTaken.put(entity, new ArrayList<>(Arrays.asList(combatDamage)));
        }
    }

    /**
     * When an entity dies it is paramount that we know who dealt the most damage to
     * that entity so that we can determine who will receive the drop.
     *
     * @return the {@link Entity} that dealt the most damage to this {@link Entity}.
     */
    public Entity calculateKiller() {
        final long VALID_TIMEFRAME = this instanceof NPC ? TimeUnit.MINUTES.toMillis(5) : TimeUnit.SECONDS.toMillis(90);
        Entity killer = null;
        int totalDamage = 0;
        for (Entry<Entity, List<Damage>> entry : damageTaken.entrySet()) {
            Entity tempKiller = entry.getKey();
            List<Damage> damageList = entry.getValue();
            int damage = 0;
            if (tempKiller == null) {
                continue;
            }
            for (Damage d : damageList) {
                if (System.currentTimeMillis() - d.getTimestamp() < VALID_TIMEFRAME) {
                    damage += d.getAmount();
                }
            }
            if (totalDamage == 0 || damage > totalDamage || killer == null) {
                totalDamage = damage;
                killer = tempKiller;
            }
            if (killer != null && killer instanceof Player && this instanceof NPC) {
                Player player = (Player) killer;
                NPC npc = (NPC) this;
                if (player.getMode().isIronmanType() && !Boundary.isIn(player, Boundary.GODWARS_BOSSROOMS) && !Boundary.isIn(player, Boundary.CORPOREAL_BEAST_LAIR) && !Boundary.isIn(player, Boundary.DAGANNOTH_KINGS) && !Boundary.isIn(player, Boundary.TEKTON) && !Boundary.isIn(player, Boundary.SKELETAL_MYSTICS) && !Boundary.isIn(player, Boundary.RAID_MAIN)) {
                    double percentile = ((double) totalDamage / (double) npc.getHealth().getMaximumHealth()) * 100.0;
                    if (percentile < 75.0) {
                        killer = null;
                    }
                }
            }
        }
        return killer;
    }

    /**
     * The index value where the {@link Entity} resides along with other common
     * counterparts.
     *
     * @return the index of the array where this object resides
     */
    public int getIndex() {
        return index;
    }

    /**
     * Set the index.
     * @param index the index of the array where this object resides
     */
    public void setIndex(int index) {
        this.index = index;
    }

    /**
     * Gets the current {@link Position}
     */
    public Position getPosition() {
        return new Position(getX(), getY(), getHeight());
    }

    /**
     * Clears any and all damage that has been taken by the entity
     */
    public void resetDamageTaken() {
        damageTaken.clear();
    }

    /**
     * The status of the entities health whether it's normal, poisoned, or some
     * other nature.
     *
     * @return the status of the entities health
     */
    public Health getHealth() {
        if (health == null) {
            health = new Health(this);
        }
        return health;
    }

    /**
     * Updates the {@link Entity} that has killed this {@link Entity
     *
     * @param killer
     *            the {@link Entity} killer
     */
    public void setKiller(Entity killer) {
        this.killer = killer;
    }

    /**
     * The {@link Entity} that has dealt the most damage in combat
     *
     * @return the killer
     */
    public Entity getKiller() {
        return killer;
    }

    /**
     * Retrieves the current hitmark
     *
     * @return the hitmark
     */
    public Hitmark getHitmark() {
        return hitmark1;
    }

    /**
     * Retrieves the second hitmark
     *
     * @return the second hitmark
     */
    public Hitmark getSecondHitmark() {
        return hitmark2;
    }

    public boolean isPlayer() {
        return this instanceof Player;
    }

    public boolean isNPC() {
        return this instanceof NPC;
    }

    public Player asPlayer() {
        return (Player) this;
    }

    public NPC asNPC() {
        return (NPC) this;
    }

    public RegionProvider getRegionProvider() {
        if (instance != null) {
            return instance;
        } else {
            return RegionProvider.getGlobal();
        }
    }

    public Attributes getAttributes() {
        return attributes;
    }

    public Location getLocation() {
        return new Location(getX(), getY(), getHeight());
    }

    public Raids getRaidsInstance() {
        return raidsInstance;
    }

    public Entity setRaidsInstance(Raids raids) {
        this.raidsInstance = raids;
        return this;
    }

    public Animation getAnimation() {
        return animation;
    }

    public boolean isUpdateRequired() {
        return updateRequired;
    }

    public void setUpdateRequired(boolean updateRequired) {
        this.updateRequired = updateRequired;
    }

    public boolean isAnimationUpdateRequired() {
        return animationUpdateRequired;
    }

    public InstancedArea getInstance() {
        return instance;
    }

    /**
     * Set an instance. This is only used by an {@link InstancedArea}, do not call this method elsewhere.
     * Instead use {@link InstancedArea#add} and {@link Entity#removeFromInstance()}
     * @param instance the instance
     */
    public void setInstance(InstancedArea instance) {
        this.instance = instance;
    }

    public boolean isGfxUpdateRequired() {
        return gfxUpdateRequired;
    }

    public Graphic getGraphic() {
        return graphic;
    }

    public boolean isInvisible() {
        return invisible;
    }

    public void setInvisible(boolean invisible) {
        this.invisible = invisible;
    }

    public void setXeric(final Xeric xeric) {
        this.xeric = xeric;
    }

    public Xeric getXeric() {
        return this.xeric;
    }

    public TickTimer getAnimationTimer() {
        return this.animationTimer;
    }

    public int getBonus(Bonus bonus) {
        return 0;
    }

}
