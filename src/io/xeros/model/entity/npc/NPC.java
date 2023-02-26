package io.xeros.model.entity.npc;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.ser.std.StdArraySerializers;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import io.xeros.Configuration;
import io.xeros.Server;
import io.xeros.content.bosses.Skotizo;
import io.xeros.content.bosses.wildypursuit.FragmentOfSeren;
import io.xeros.content.combat.CombatConfigs;
import io.xeros.content.combat.CombatHit;
import io.xeros.content.combat.Hitmark;
import io.xeros.content.combat.npc.NPCAutoAttack;
import io.xeros.content.combat.npc.NPCCombatAttack;
import io.xeros.content.combat.npc.NPCCombatAttackHit;
import io.xeros.content.combat.weapon.CombatStyle;
import io.xeros.content.commands.owner.Npc;
import io.xeros.content.commands.owner.Pos;
import io.xeros.content.minigames.pest_control.PestControl;
import io.xeros.content.minigames.raids.Raids;
import io.xeros.model.*;
import io.xeros.model.collisionmap.PathChecker;
import io.xeros.model.cycleevent.CycleEvent;
import io.xeros.model.cycleevent.CycleEventContainer;
import io.xeros.model.cycleevent.CycleEventHandler;
import io.xeros.model.definitions.NpcDef;
import io.xeros.model.definitions.NpcStats;
import io.xeros.model.entity.Entity;
import io.xeros.model.entity.HealthStatus;
import io.xeros.model.entity.npc.actions.NPCHitPlayer;
import io.xeros.model.entity.npc.data.BlockAnimation;
import io.xeros.model.entity.npc.data.DeathAnimation;
import io.xeros.model.entity.npc.stats.NpcBonus;
import io.xeros.model.entity.npc.stats.NpcCombatDefinition;
import io.xeros.model.entity.npc.stats.NpcCombatSkill;
import io.xeros.model.entity.player.Boundary;
import io.xeros.model.entity.player.Player;
import io.xeros.model.entity.player.Position;
import io.xeros.model.items.EquipmentSet;
import io.xeros.util.Location3D;
import io.xeros.util.Misc;
import io.xeros.util.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NPC extends Entity {

	private static final Logger logger = LoggerFactory.getLogger(NPC.class);
    public int parentIndex;

    private int npcId;
	public int summonedBy;
	public int absX, absY;
	public int heightLevel;
	private boolean unregister;

	public boolean isPet;

	public int makeX;
	public int makeY;
	public int maxHit;
	public Direction walkDirection = Direction.NONE;
	public Direction runDirection = Direction.NONE;
	public int walkingType;

	public int lastX, lastY;
	public boolean summoner;
	public boolean teleporting;

	public int parentNpc = -1;
	public List<Integer> children;

	public long lastRandomlySelectedPlayer = 0;

	private boolean transformUpdateRequired;
	public int transformId;
	public Location3D targetedLocation;

	/**
	 * attackType: 0 = melee, 1 = range, 2 = mage
	 */
	public long lastSpecialAttack;

	public boolean spawnedMinions;

	private CombatType attackType;

	public int projectileId, endGfx, spawnedBy, hitDelayTimer, hitDiff, actionTimer;
	public boolean applyDead;
	public boolean needRespawn;
	public boolean walkingHome, underAttack;
	private int playerAttackingIndex;
	public int killedBy;
	public int oldIndex;
	public int underAttackBy;
	public long lastDamageTaken;
	public boolean randomWalk;
	public boolean faceEntityUpdateRequired;
	public boolean hitUpdateRequired;
	public boolean forcedChatRequired;
	private boolean forceMovementUpdateRequired;
	private ForceMovement forceMovement;
	public String forcedText;
	public int FocusPointX = -1, FocusPointY = -1;
	public int face;
	public int totalAttacks;
	private boolean facePlayer = true;
	private int projectileDelay;

	private NPCBehaviour behaviour = new NPCBehaviour();
	private final NpcDef definition;

	private long lastRandomWalk;
	private long lastRandomWalkHome;

	private long randomWalkDelay;
	private long randomStopDelay;

	private NpcStats defaultNpcStats;
	private NpcStats npcStats;

	private List<NPCAutoAttack> npcAutoAttacks = Lists.newArrayList();
	private NPCAutoAttack currentAttack;

	private NpcCombatDefinition npcCombatDefinition;

	public NPC(int npcId, Position position) {
		super();
		setNpcId(npcId);
		this.definition = NpcDef.forId(npcId);
		Preconditions.checkState(definition != null, "NPCDefinition cannot be null!");
		absX = makeX = position.getX();
		absY = makeY = position.getY();
		heightLevel = position.getHeight();
		setup();
		register();
		clearUpdateFlags();
		fetchDefaultNpcStats();
		setNpcCombatDefinition();
	}

	public NPC(int index, int npcId, NpcDef definition, NpcStats defaultNpcStats) {
		super(index);
		this.definition = definition;
		setNpcId(npcId);
		walkDirection = Direction.NONE;
		runDirection = Direction.NONE;
		setup();
		setDefaultNpcStats(defaultNpcStats);
		setNpcCombatDefinition();
	}

	public NPC(int index, int npcId, NpcDef definition) {
		super(index);
		this.definition = definition;
		setNpcId(npcId);
		walkDirection = Direction.NONE;
		runDirection = Direction.NONE;
		setup();
		fetchDefaultNpcStats();
		setNpcCombatDefinition();
	}

	private void setup() {
		setDead(false);
		applyDead = false;
		actionTimer = 0;
		randomWalk = true;
		if (definition.isRunnable()) {
			getBehaviour().setRunnable(true);
		}
	}

	public void setNpcCombatDefinition() {
		NpcCombatDefinition definition = NpcCombatDefinition.definitions.get(this.npcId);
		if (definition == null) {
			this.npcCombatDefinition = new NpcCombatDefinition(this);
		} else {
			this.npcCombatDefinition = new NpcCombatDefinition(definition);
		}
	}

	public NPC provideRespawnInstance() {
		return null;
	}

	private void fetchDefaultNpcStats() {
		if (definition.getCombatLevel() > 0 || NpcStats.forId(npcId).getHitpoints() > 0) {
			setDefaultNpcStats(NpcStats.forId(npcId));
		} else {
			setDefaultNpcStats(NpcStats.builder().setAttackSpeed(4).createNpcStats());
		}
	}

	public void setDefaultNpcStats(NpcStats defaultNpcStats) {
		this.npcStats = defaultNpcStats;
		this.defaultNpcStats = defaultNpcStats;
		//this.defence = defaultNpcStats.getDefenceLevel();
		getHealth().setMaximumHealth(getNpcStats().getHitpoints());
		getHealth().reset();
	}

	public void setNpcStats(NpcStats npcStats) {
		this.npcStats = npcStats;
	}

	public boolean canBeAttacked(Entity entity) {
		return true;
	}

	public boolean canBeDamaged(Entity entity) {
		return true;
	}

	@Override
	public boolean isAutoRetaliate() {
		return true;
	}

	public void onDeath() {}

	public void afterDeath() {}

	public int getDeathAnimation() {
		return DeathAnimation.handleEmote(getNpcId());
	}

	public int modifyDamage(Player player, int damage) {
		return damage;
	}

	public int getAttackDistanceModifier(Player player, CombatType combatType) {
		return 0;
	}

	@Override
	public boolean hasBlockAnimation() {
		return Arrays.stream(PestControl.PORTAL_DATA).noneMatch(data -> data[0] == getNpcId()) && getNpcId() != 2042 && getNpcId() != 2043 & getNpcId() != 2044
				&& getNpcId() != 3127 && getNpcId() != 319;
	}

	@Override
	public Animation getBlockAnimation() {
		return new Animation(BlockAnimation.getAnimation(getNpcId()));
	}

	@Override
	public void attackEntity(Entity entity) {
		if (entity.isPlayer()) {
			setPlayerAttackingIndex(entity.getIndex());
		}
	}

	@Override
	public boolean isNPC() {
		return true;
	}

	@Override
	public String toString() {
		return "NPC{" +
				"npcId=" + npcId +
				", absX=" + absX +
				", absY=" + absY +
				", spawnX=" + makeX +
				", spawnY=" + makeY +
				", name='" + getName() + '\'' +
				", index=" + getIndex() +
				", instance=" + getInstance() +
				'}';
	}

	@Override
	public int getX() {
		return absX;
	}
	@Override
	public int getY() {
		return absY;
	}
	@Override
	public void setX(int x) {
		this.absX = x;
	}

	@Override
	public void setY(int y) {
		this.absY = y;
	}

	@Override
	public int getHeight() {
		return heightLevel;
	}

	@Override
	public void setHeight(int height) {
		this.heightLevel = height;
	}

	@Override
	public int getDefenceLevel() {
		return this.npcCombatDefinition.getLevel(NpcCombatSkill.DEFENCE);
	}

	@Override
	public int getDefenceBonus(CombatType type, Entity attacker) {
		// WARNING: the returned value can't be negative for magic combat formula
		// and other combat formulas if they ever get corrected to osrs
		if (type.equals(CombatType.MELEE)) {
			switch (getNpcId()) {
				case 965://kalphite queen
					return attacker.isPlayer() ? (EquipmentSet.VERAC.isWearing(attacker.asPlayer()) ? +500 : 5000) : 5000;
				case 9021://hunllef melee
				case 9022://hunllef range
				case 9023://hunllef mage
					return 100;
			}
		} else if (type.equals(CombatType.MAGE)) {
			switch (getNpcId()) {
				//case 2042://green zulrah
				//	return -150;
				case 319://corp
					return +80;
				//case 2044://blue zulrah
				//	return 1550;
				case 963://kalhpite queen
					return +7000;
				case 965://kalhpite queen part 2
				case 5890://abyssal sire
					return 100;
			}
		} else if (type.equals(CombatType.RANGE)) {
			switch (getNpcId()) {
				case 2042://green zulrah
				case 2043://red zulrah
				case 5890://abyssale sire
					return 0;
				case 2044://blue zulrah
					return -150;
				case 963://kalphite queen
					return +7000;
				case 965://kalphite queen part 2
					return 300;

			}
		}

		switch (type) {
			case MELEE:
				if (attacker.isPlayer()) {
					switch (attacker.asPlayer().getCombatConfigs().getWeaponMode().getCombatStyle()) {
						case STAB:
							return getNpcStats().getStabDef();
						case SLASH:
							return getNpcStats().getSlashDef();
						case CRUSH:
							return getNpcStats().getCrushDef();
					}
				}
				return getNpcStats().getSlashDef();
			case RANGE:
				return getNpcStats().getRangeDef();
			case MAGE:
				return getNpcStats().getMagicDef();
		}

		return 0;
	}

	@Override
	public void removeFromInstance() {
		if (getInstance() != null) {
			getInstance().remove(this);
		}
	}

	@Override
	public int getEntitySize() {
		return getSize();
	}

	public void register() {
		Preconditions.checkState(getIndex() == 0, "Already registered!");
		int index = Server.npcHandler.register(this);
		Preconditions.checkState(index != -1, "Cannot register npc!");
		setIndex(index);
		this.getRegionProvider().addNpcClipping(this);
	}

	/**
	 * You should avoid using this unless you need instant deregistration.
	 * If deregistration deffered to the next npc process with due, you should simply
	 * call {@link NPC#unregister}. Calling this will cause issues, especially
	 * if you're calling from inside the npc process, as that will still complete
	 * after the npc has been unregistered.
	 */
	public void unregisterInstant() {
		unregister();
		processDeregistration();
	}

	/**
	 * Set the NPC to unregister on the next process.
	 * If you need instant deregistration call this and {@link NPC#processDeregistration()}.
	 */
	public void unregister() {
		setUnregister(true);
		this.getRegionProvider().removeNpcClipping(this);
	}

	/**
	 * Called by the internal npc processing to finish deregistration of an NPC.
	 * This is to prevent npcs from being unregistered while they are still processing.
	 * You shouldn't call this method to remove an npc, use {@link NPC#unregister()}.
	 */
	public boolean processDeregistration() {
		if (isUnregister() && getIndex() > 0) {
			CycleEventHandler.getSingleton().stopEvents(this);
			NPCHandler.npcs[getIndex()] = null;
			setIndex(0);
			if (getInstance() != null) {
				getInstance().remove(this);
			}
			logger.debug("Unregistered {}", this);
			return true;
		}

		return false;
	}

	public void resetAttack() {
		setPlayerAttackingIndex(0);
		facePlayer(0);
		underAttack = false;
		randomWalk = true;
	}

	public void addChild(NPC npc) {
		Preconditions.checkState(npc.parentNpc == -1);
		npc.parentNpc = getIndex();
		if (children == null) {
			children = Lists.newArrayList(npc.getIndex());
		} else {
			children.add(npc.getIndex());
		}
	}

	public NPC getParent() {
		if (parentNpc != -1) {
			return NPCHandler.npcs[parentNpc];
		} else {
			return null;
		}
	}

	public List<NPC> getChildren() {
		List<NPC> childrenNpcs = Lists.newArrayList();
		if (children != null) {
			children.forEach(child -> {
				NPC npc = NPCHandler.npcs[child];
				Objects.requireNonNull(npc);
				childrenNpcs.add(npc);
			});
		}
		return childrenNpcs;
	}

	public void process() {
		Server.npcHandler.getNpcProcess().process(getIndex());
	}

	public void selectAutoAttack(Entity entity) {
		Preconditions.checkState(!npcAutoAttacks.isEmpty(), "No auto attacks present!");
		List<NPCAutoAttack> viable = npcAutoAttacks.stream().filter(autoAttack -> autoAttack.getSelectAutoAttack() == null
				|| autoAttack.getSelectAutoAttack().apply(new NPCCombatAttack(this, entity))).collect(Collectors.toList());
		Preconditions.checkState(!viable.isEmpty(), "No viable attacks can be found, npc: " + toString());
		currentAttack = viable.get(Misc.trueRand(viable.size()));
	}

	public void attack(Player c, NPCAutoAttack npcAutoAttack) {
		if (lastX != getX() || lastY != getY()) {
			return;
		} else if (c.isInvisible()  || isDead() || c.respawnTimer > 0) {
			return;
		} else if (c.getBankPin().requiresUnlock()) {
			c.getBankPin().open(2);
			return;
		} else if (!getPosition().inMulti() && underAttackBy > 0 && underAttackBy != c.getIndex()) {
			resetAttack();
			return;
		} else if (!getPosition().inMulti() && ((c.underAttackByPlayer > 0 && c.underAttackByNpc != getIndex())
				|| (c.underAttackByNpc > 0 && c.underAttackByNpc != getIndex()))) {
			resetAttack();
			return;
		} else if (heightLevel != c.heightLevel) {
			resetAttack();
			return;
		}

		facePlayer(c.getIndex());

		if (getDistance(c.getX(), c.getY()) > ((double) npcAutoAttack.getDistanceRequiredForAttack()) + (getSize() > 1 ? 0.5 : 0.0)) {
			return;
		}

		/*if (getAttackType() == CombatType.MELEE) { // This fixes attacking throough walls
			if (!PathChecker.raycast(this, c, false)) {
				return;
			}
		}*/
		if (NPCHandler.projectileClipping && !npcAutoAttack.isIgnoreProjectileClipping()) {
			if (getAttackType() == null || getAttackType() == CombatType.MAGE || getAttackType() == CombatType.RANGE) {
				int x1 = absX;
				int y1 = absY;
				int z = heightLevel;
				/*if (!PathChecker.isProjectilePathClear(this, c, x1, y1, z, c.absX, c.absY)
						&& !PathChecker.isProjectilePathClear(this, c, c.absX, c.absY, z, x1, y1)) {
					return;
				}*/
				if (!PathChecker.raycast(this, c, true)
					&& !PathChecker.raycast(c, this, true))
					return;
			}
		}

		attackTimer = npcAutoAttack.getAttackDelay();
		setAttackType(npcAutoAttack.getCombatType());
		oldIndex = c.getIndex();

		if (npcAutoAttack.getAnimation() != null)
			startAnimation(npcAutoAttack.getAnimation());
		if (npcAutoAttack.getStartGraphic() != null)
			startGraphic(npcAutoAttack.getStartGraphic());

		if (npcAutoAttack.isMultiAttack()) {
			Preconditions.checkState(npcAutoAttack.getSelectPlayersForMultiAttack() != null, "You must define a NPCAutoAttack#selectPlayersForMultiAttack");
			for (Player player : npcAutoAttack.getSelectPlayersForMultiAttack().apply(new NPCCombatAttack(this, c))) {
				finishAutoAttack(player, npcAutoAttack);
			}
		} else {
			finishAutoAttack(c, npcAutoAttack);
		}
	}

	private void finishAutoAttack(Player c, NPCAutoAttack npcAutoAttack) {
		c.underAttackByNpc = getIndex();
		c.singleCombatDelay2 = System.currentTimeMillis();
		c.getPA().removeAllWindows();

		NPCCombatAttack npcCombatAttack = new NPCCombatAttack(this, c);

		if (npcAutoAttack.getOnAttack() != null)
			npcAutoAttack.getOnAttack().accept(npcCombatAttack);
		if (npcAutoAttack.getProjectile() != null)
			npcAutoAttack.getProjectile().createTargetedProjectile(this, c).send(getInstance());

		if (getAttackType() == CombatType.DRAGON_FIRE) {
			hitDelayTimer += 2;
			c.getCombatItems().absorbDragonfireDamage();
		}

		Preconditions.checkState(npcAutoAttack.getHitDelay() > 0, "Hit delay is zero!");
		final NPC npc = this;
		CycleEventHandler.getSingleton().addEvent(this, new CycleEvent() {
			@Override
			public void execute(CycleEventContainer container) {
				CombatHit hit = NPCHitPlayer.applyAutoAttackDamage(npc, c, npcAutoAttack);
				if (npcAutoAttack.getOnHit() != null)
					npcAutoAttack.getOnHit().accept(NPCCombatAttackHit.of(npcCombatAttack, hit));
				container.stop();
			}
		}, npcAutoAttack.getHitDelay());
	}

	public Position getFollowPosition() {
		switch (getNpcId()) {
			case Skotizo.AWAKENED_ALTAR_EAST:
				return new Position(1713, 9888);
			case Skotizo.AWAKENED_ALTAR_NORTH:
				return new Position(1694, 9903);
			case Skotizo.AWAKENED_ALTAR_SOUTH:
				return new Position(1696, 9872);
			case Skotizo.AWAKENED_ALTAR_WEST:
				return new Position(1679, 9888);
		}
		return null;
	}

	public Position getPosition() {
		return new Position(absX, absY, heightLevel);
	}

	public Position getCenterPosition() {
		Position position = getPosition();
		Position position2 = getPosition().translate(getSize() - 1, getSize() - 1);
		return new Position((position.getX() + position2.getX()) / 2, (position.getY() + position2.getY()) / 2, position.getHeight());
	}

	public void startAnimation(int animationId) {
		startAnimation(new Animation(animationId));
	}

	public void startAnimation(int animationId, AnimationPriority animationPriority) {
		startAnimation(new Animation(animationId, 0, animationPriority));
	}

	public void sendForceMovement(ForceMovement forceMovement) {
		this.forceMovement = forceMovement;
		forceMovementUpdateRequired = true;
		setUpdateRequired(true);
	}

	/**
	 * Teleport
	 * 
	 * @param x
	 * @param y
	 * @param z
	 */
	public void teleport(int x, int y, int z) {
		teleport(new Position(x, y, z));
	}

	public void teleport(Position position) {
		teleporting = true;
		setUpdateRequired(true);

		if (!this.isPet) {
			this.getRegionProvider().removeNpcClipping(this);
		}

		absX = position.getX();
		absY = position.getY();
		heightLevel = position.getHeight();

		if (!this.isPet) {
			this.getRegionProvider().addNpcClipping(this);
		}
	}

	/**
	 * Makes the npcs either able or unable to face other players
	 * 
	 * @param facePlayer
	 *            {@code true} if the npc can face players
	 */
	public void setFacePlayer(boolean facePlayer) {
		this.facePlayer = facePlayer;
	}

	/**
	 * Sends the request to a client that the npc should be transformed into
	 * another.
	 */
	public void requestTransform(int id) {
		if (npcId != id) {
			transformId = id;
			setNpcId(id);
			transformUpdateRequired = true;
			setUpdateRequired(true);
			this.setNpcCombatDefinition();
		}
	}

	public void appendTransformUpdate(Stream str) {
		str.writeWordBigEndianA(transformId);
	}

	public void updateNPCMovement(Stream str) {
		if (walkDirection == Direction.NONE) {
			if (isUpdateRequired()) {
				str.writeBits(1, 1);
				str.writeBits(2, 0);
			} else {
				str.writeBits(1, 0);
			}
		} else {
			str.writeBits(1, 1);

			if (runDirection == Direction.NONE) {
				str.writeBits(2, 1);
				str.writeBits(3, walkDirection.toInteger());
			} else {
				str.writeBits(2, 2);
				str.writeBits(3, walkDirection.toInteger());
				str.writeBits(3, runDirection.toInteger());
			}

			if (isUpdateRequired()) {
				str.writeBits(1, 1);
			} else {
				str.writeBits(1, 0);
			}
		}
	}
	
	/**
	 * Text update
	 **/

	public void forceChat(String text) {
		forcedText = text;
		forcedChatRequired = true;
		setUpdateRequired(true);
	}

	public void appendMask80Update(Stream str) {
		str.writeUnsignedWord(getGraphic().getId());
		str.writeDWord(getGraphic().getHeight() + (getGraphic().getDelay() & 0xffff));
	}

	public void gfx100(int gfx) {
		startGraphic(new Graphic(gfx, 0, 6553600));
	}
	public void gfx100(int gfx, int height) {
		startGraphic(new Graphic(gfx, 0, 65536 * height));
	}
	public void gfx0(int gfx) {
		startGraphic(new Graphic(gfx, 0, 0));
	}

	@Override
	public boolean isFreezable() {
		switch (getNpcId()) {
			case 2042:
			case 2043:
			case 2044:
			case 7544:
			case 5129:
			case FragmentOfSeren.NPC_ID:
			case 2205:
			case 3129:
			case 2215:
			case 3162:
				return false;
		}
		return true;
	}

	public void appendAnimUpdate(Stream str) {
		str.writeWordBigEndian(getAnimation().getId());
		str.writeByte(getAnimation().getDelay());
	}

	private void appendSetFocusDestination(Stream str) {
		str.writeWordBigEndian(FocusPointX);
		str.writeWordBigEndian(FocusPointY);
	}

	public void facePosition(Position position) {
		facePosition(position.getX(), position.getY());
	}

	public void facePosition(int x, int y) {
		FocusPointX = 2 * x + 1;
		FocusPointY = 2 * y + 1;
		setUpdateRequired(true);
	}

	public void appendFaceEntity(Stream str) {
		str.writeUnsignedWord(face);
	}

	public void facePlayer(int player) {
		if (getNpcId() == Npcs.MAX_DUMMY) {
			return;
		}

		if (!facePlayer) {
			if (face == -1) {
				return;
			}
			face = -1;
		} else {
			face = player + 32768;
		}
		faceEntityUpdateRequired = true;
		setUpdateRequired(true);
	}

	public void faceNPC(int index) {
		face = index;
		faceEntityUpdateRequired = true;
		setUpdateRequired(true);
	}

	public void appendForcedMovementUpdate(Player player, Stream str) {
		str.writeByte(forceMovement.getStart(player).getX());
		str.writeByte(forceMovement.getStart(player).getY());
		str.writeByte(forceMovement.getEnd(player).getX());
		str.writeByte(forceMovement.getEnd(player).getY());
		str.writeUnsignedWord(forceMovement.getMoveCycleStart());
		str.writeUnsignedWord(forceMovement.getMoveCycleEnd());
		str.writeByte(forceMovement.getMoveDirection());
	}

	public void appendNPCUpdateBlock(Player player, Stream str) {
		if (!isUpdateRequired())
			return;
		int updateMask = 0;
		if (forceMovementUpdateRequired)
			updateMask |= 0x100;
		if (isAnimationUpdateRequired())
			updateMask |= 0x10;
		if (hitUpdateRequired2)
			updateMask |= 8;
		if (isGfxUpdateRequired())
			updateMask |= 0x80;
		if (faceEntityUpdateRequired)
			updateMask |= 0x20;
		if (forcedChatRequired)
			updateMask |= 1;
		if (hitUpdateRequired)
			updateMask |= 0x40;
		if (transformUpdateRequired)
			updateMask |= 2;
		if (FocusPointX != -1)
			updateMask |= 4;

		str.writeUnsignedWord(updateMask);

		if (forceMovementUpdateRequired)
			appendForcedMovementUpdate(player, str);
		if (isAnimationUpdateRequired())
			appendAnimUpdate(str);
		if (hitUpdateRequired2)
			appendHitUpdate2(str);
		if (isGfxUpdateRequired())
			appendMask80Update(str);
		if (faceEntityUpdateRequired)
			appendFaceEntity(str);
		if (forcedChatRequired) {
			str.writeString(forcedText);
		}
		if (hitUpdateRequired)
			appendHitUpdate(str);
		if (transformUpdateRequired)
			appendTransformUpdate(str);
		if (FocusPointX != -1)
			appendSetFocusDestination(str);
	}

	public void clearUpdateFlags() {
		setUpdateRequired(false);
		forcedChatRequired = false;
		hitUpdateRequired = false;
		hitUpdateRequired2 = false;
		faceEntityUpdateRequired = false;
		transformUpdateRequired = false;
		forcedText = null;
		walkDirection = Direction.NONE;
		runDirection = Direction.NONE;
		FocusPointX = -1;
		FocusPointY = -1;
		teleporting = false;
		forceMovementUpdateRequired = false;
		resetAfterUpdate();
	}

	@Override
	public void resetWalkingQueue() {
		setMovement(Direction.NONE);
	}

	public void setMovement(Direction walkDirection) {
		setMovement(walkDirection, Direction.NONE);
	}

	public void setMovement(Direction walkDirection, Direction runDirection) {
		setWalkDirection(walkDirection);
		setRunDirection(runDirection);
	}

	public void moveTowards(int x, int y) {
		moveTowards(x, y, false, false);
	}

	public void moveTowards(int x, int y, boolean checkClipping) {
		moveTowards(x, y, false, checkClipping);
	}

	public void moveTowards(int x, int y, boolean run, boolean checkClipping) {
		resetWalkingQueue();
		Position moveTowards = new Position(x, y, heightLevel);
		Direction dir1 = Direction.fromDeltas(getPosition(), moveTowards);
		if (!checkClipping || NPCDumbPathFinder.canMoveTo(this, getPosition(), dir1)) {
			setWalkDirection(dir1);
		} else {
			resetWalkingQueue();
			return;
		}

		Position walked = new Position(absX + walkDirection.x(), absY + walkDirection.y());

		if (walked.equals(moveTowards)) {
			return;
		}

		if ((behaviour.isRunnable() || run) && (!checkClipping || NPCDumbPathFinder.canMoveTo(this, walked, runDirection))) {
			setRunDirection(Direction.fromDeltas(walked, moveTowards));
		}
	}

	public boolean revokeWalkingPrivilege;

	public void processMovement() {
		if (revokeWalkingPrivilege)
			return;
		if (walkDirection != Direction.NONE) {
			if (freezeTimer != 0 || (forceMovement != null && !forceMovement.isActive()) || teleporting) {
				resetWalkingQueue();
			} else {
				if (!this.isPet)
					this.getRegionProvider().removeNpcClipping(this);

				absX += walkDirection.x() + runDirection.x();
				absY += walkDirection.y() + runDirection.y();

				if (!this.isPet)
					this.getRegionProvider().addNpcClipping(this);

				setUpdateRequired(true);
			}
		} else {
			Preconditions.checkState(runDirection == Direction.NONE, "How can you walk before you run?");
		}
		//dir >>= 1; // TODO is this needed, commented out when adding npc running?
	}

	@Override
	public void appendHitUpdate(Stream str) {
		if (getHealth().getCurrentHealth() <= 0) {
			setDead(true);
		}
		if (hitmark1 != null && !hitmark1.isMiss() && hitDiff == 0) {
			hitDiff = 0;
			hitmark1 = Hitmark.MISS;
		}
		str.writeByteC(hitDiff);
		if (hitmark1 != null) {
			str.writeByteS(hitmark1.getId());
		} else {
			str.writeByteS(0);
		}
		str.writeUnsignedWord(health.getCurrentHealth());
		str.writeUnsignedWord(health.getMaximumHealth());
	}

	public int hitDiff2;
	public boolean hitUpdateRequired2;


	@Override
	public void appendHitUpdate2(Stream str) {
		if (getHealth().getCurrentHealth() <= 0) {
			setDead(true);
		}
		if (hitmark2 != null && !hitmark2.isMiss() && hitDiff2 == 0) {
			hitDiff2 = 0;
			hitmark2 = Hitmark.MISS;
		}
		str.writeByteA(hitDiff2);
		if (hitmark2 != null) {
			str.writeByteC(hitmark2.getId());
		} else {
			str.writeByteC(0);
		}
		str.writeUnsignedWord(getHealth().getCurrentHealth());
		str.writeUnsignedWord(getHealth().getMaximumHealth());
	}

	public void lowerDefence(double percent) {
		Preconditions.checkArgument(percent > 0 && percent <= 1, "Percent out of bounds.");
		int defence = getDefence();
		int loweredDefence = (int) (defence * (1.0 - percent));
		if (loweredDefence < 0) {
			loweredDefence = 0;
		}
		this.npcCombatDefinition.setLevel(NpcCombatSkill.DEFENCE, loweredDefence);
	}

	public void increaseDefence(int newDefence) {
		this.npcCombatDefinition.setLevel(NpcCombatSkill.DEFENCE, newDefence);
	}

	public int getDefence() {
		return this.npcCombatDefinition.getLevel(NpcCombatSkill.DEFENCE);
	}

	public boolean inRaids() {
		return (getX() > 3210 && getX() < 3368 && getY() > 5137 && getY() < 5759);
	}
	public boolean inXeric() {
		return (getX() > 2685 && getX() < 2743 && getY() > 5440 && getY() < 5495);
	}

	public int getSize() {
		if (definition == null)
			return 1;
		return definition.getSize();
	}

	/**
	 * An object containing specific information about the NPC such as the combat
	 * level, default maximum health, the name, etcetera.
	 * 
	 * @return the {@link NpcDef} object associated with this NPC
	 */
	public NpcDef getDefinition() {
		return definition;
	}

	public String getName() {
		return getDefinition().getName();
	}

	public int getProjectileDelay() {
		return projectileDelay;
	}

	public void setProjectileDelay(int delay) {
		projectileDelay = delay;
	}

	@Override
	public void appendHeal(int amount, Hitmark hitmark) {
		getHealth().increase(amount);
		if (!hitUpdateRequired) {
			hitUpdateRequired = true;
			hitDiff = amount;
			hitmark1 = hitmark;
		} else if (!hitUpdateRequired2) {
			hitUpdateRequired2 = true;
			hitDiff2 = amount;
			hitmark2 = hitmark;
		}
		setUpdateRequired(true);
	}

	@Override
	public void appendDamage(Entity entity, int damage, Hitmark hitmark) {
		if (entity != null && entity.isPlayer()) {
			Player player = (Player) entity;
			damage = modifyDamage(player, damage);
			if (!canBeDamaged(player)) {
				damage = 0;
			}

			if (damage > 0 && hitmark == Hitmark.MISS) {
				hitmark = Hitmark.HIT;
			}

			addDamageTaken(player, damage);

			if (player.getRaidsInstance() != null && Boundary.isIn(player, Boundary.FULL_RAIDS)) {
				Raids.damage(player, damage);
			}
		}

		if (damage < 0) {
			damage = 0;
			hitmark = Hitmark.MISS;
		}
		if (getHealth().getCurrentHealth() - damage < 0) {
			damage = getHealth().getCurrentHealth();
		}
		if (getNpcId() != Npcs.MAX_DUMMY) {
			getHealth().reduce(damage);
			if (getHealth().getCurrentHealth() <= 0) {
				setDead(true);
			}
		}
		if (!hitUpdateRequired) {
			hitUpdateRequired = true;
			hitDiff = damage;
			hitmark1 = hitmark;
		} else if (!hitUpdateRequired2) {
			hitUpdateRequired2 = true;
			hitDiff2 = damage;
			hitmark2 = hitmark;
		}
		setUpdateRequired(true);
	}

	@Override
	public boolean susceptibleTo(HealthStatus status) {
		switch (getNpcId()) {
		case 2042:
		case 2043:
		case 2044:
		case 6720:
		case 7413:
		case 7544:
		case 5129:
		case FragmentOfSeren.NPC_ID:
		case 7604:
		case 7605:
		case 7606:
			return false;
		}

		if (status == HealthStatus.POISON) {
			return !defaultNpcStats.isPoisonImmune();
		} else if (status == HealthStatus.VENOM) {
			return !defaultNpcStats.isVenomImmune();
		}

		return true;
	}

	public long getLastRandomWalk() {
		return lastRandomWalk;
	}

	public void setLastRandomWalk(long lastRandomWalk) {
		this.lastRandomWalk = lastRandomWalk;
	}

	public long getRandomWalkDelay() {
		return randomWalkDelay;
	}

	public void setRandomWalkDelay(long randomWalkDelay) {
		this.randomWalkDelay = randomWalkDelay;
	}

	public int getNpcId() {
		return npcId;
	}

	public void setNpcId(int npcId) {
		this.npcId = npcId;
	}

	public NPCAutoAttack getCurrentAttack() {
		return currentAttack;
	}

	public List<NPCAutoAttack> getNpcAutoAttacks() {
		return Collections.unmodifiableList(npcAutoAttacks);
	}

	public void setNpcAutoAttacks(List<NPCAutoAttack> npcAutoAttacks) {
		this.npcAutoAttacks = npcAutoAttacks;
	}

	public boolean isDeadOrDying() {
		return isDead() || needRespawn || getHealth().getCurrentHealth() == 0;
	}

	public boolean isDead() {
		return isDead;
	}

	public void setDead(boolean dead) {
		isDead = dead;
	}

	public NPCBehaviour getBehaviour() {
		return behaviour;
	}

	public NpcStats getDefaultNpcStats() {
		return defaultNpcStats;
	}

	public NpcStats getNpcStats() {
		return npcStats;
	}

	public CombatType getAttackType() {
		return attackType;
	}

	public void setAttackType(CombatType attackType) {
		this.attackType = attackType;
	}

	public int getPlayerAttackingIndex() {
		return playerAttackingIndex;
	}

	public boolean isAggro() {
		return playerAttackingIndex > 1;
	}

	public void setPlayerAttackingIndex(int playerAttackingIndex) {
		if (this.playerAttackingIndex != playerAttackingIndex) {
			this.playerAttackingIndex = playerAttackingIndex;
		}
	}

	public Direction getWalkDirection() {
		return walkDirection;
	}

	public void setWalkDirection(Direction walkDirection) {
		this.walkDirection = walkDirection;
	}

	public Direction getRunDirection() {
		return runDirection;
	}

	public void setRunDirection(Direction runDirection) {
		this.runDirection = runDirection;
	}

	public boolean isUnregister() {
		return unregister;
	}

	public void setUnregister(boolean unregister) {
		this.unregister = unregister;
	}

	public NpcCombatDefinition getCombatDefinition() {
		return this.npcCombatDefinition;
	}

	public boolean isDemon() {
		return Misc.linearSearch(Configuration.DEMON_IDS, this.npcId) != -1;
	}

	public boolean isUndead() {
		return Misc.linearSearch(Configuration.UNDEAD_NPCS, this.npcId) != -1;
	}

	public boolean isDragon() {
		return Misc.linearSearch(Configuration.DRAG_IDS, this.npcId) != -1;
	}

	public boolean isLeafy() {
		return Misc.linearSearch(Configuration.LEAF_IDS, this.npcId) != -1;
	}

	@Override
	public int getBonus(Bonus bonus) {
		NpcCombatDefinition definition = this.getCombatDefinition();
		if (definition == null) {
			return 0;
		}

		switch (bonus) {
			case ATTACK_STAB:
			case ATTACK_SLASH:
			case ATTACK_CRUSH:
				return definition.getAttackBonus(NpcBonus.ATTACK_BONUS);
			case ATTACK_MAGIC:
				return definition.getAttackBonus(NpcBonus.MAGIC_BONUS);
			case ATTACK_RANGED:
				return definition.getAttackBonus(NpcBonus.RANGE_BONUS);

			case DEFENCE_STAB:
				return definition.getDefenceBonus(NpcBonus.STAB_BONUS);
			case DEFENCE_SLASH:
				return definition.getDefenceBonus(NpcBonus.SLASH_BONUS);
			case DEFENCE_CRUSH:
				return definition.getDefenceBonus(NpcBonus.CRUSH_BONUS);
			case DEFENCE_MAGIC:
				return definition.getDefenceBonus(NpcBonus.MAGIC_BONUS);
			case DEFENCE_RANGED:
				return definition.getDefenceBonus(NpcBonus.RANGE_BONUS);
			case MAGIC_DMG:
				return definition.getAttackBonus(NpcBonus.MAGIC_STRENGTH_BONUS);
		}

		return 0;
	}

}
