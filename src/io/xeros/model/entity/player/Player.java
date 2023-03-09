package io.xeros.model.entity.player;

import java.time.LocalDate;
import java.time.Month;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import com.google.common.collect.Lists;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.xeros.Configuration;
import io.xeros.Server;
import io.xeros.content.*;
import io.xeros.content.achievement.AchievementType;
import io.xeros.content.bosses.*;
import io.xeros.content.bosses.hespori.Hespori;
import io.xeros.content.bosspoints.BossPoints;
import io.xeros.content.combat.effects.damageeffect.impl.amuletofthedamned.impl.ToragsEffect;
import io.xeros.content.combat.melee.MeleeExtras;
import io.xeros.content.combat.weapon.WeaponMode;
import io.xeros.content.commands.all.Reclaim;
import io.xeros.content.compromised.CompromisedAccounts;
import io.xeros.content.dailyrewards.DailyRewards;
import io.xeros.content.dialogue.DialogueBuilder;
import io.xeros.content.dialogue.DialogueOption;
import io.xeros.content.donationrewards.DonationRewards;
import io.xeros.content.dwarfmulticannon.Cannon;
import io.xeros.content.event.eventcalendar.EventChallengeMonthlyReward;
import io.xeros.content.item.lootable.impl.*;
import io.xeros.content.items.PvpWeapons;
import io.xeros.content.itemskeptondeath.perdu.PerduLostPropertyShop;
import io.xeros.content.leaderboards.LeaderboardPeriodicity;
import io.xeros.content.leaderboards.LeaderboardUtils;
import io.xeros.content.minigames.tob.TobConstants;
import io.xeros.content.minigames.tob.TobContainer;
import io.xeros.content.minigames.tob.instance.TobInstance;
import io.xeros.content.party.PlayerParty;
import io.xeros.content.privatemessaging.FriendsList;
import io.xeros.content.minigames.raids.Raids;
import io.xeros.content.miniquests.magearenaii.MageArenaII;
import io.xeros.content.questing.Questing;
import io.xeros.content.teleportation.inter.TeleportInterface;
import io.xeros.content.tournaments.OutlastLeaderboardEntry;
import io.xeros.content.tutorial.ModeSelection;
import io.xeros.content.tutorial.TutorialDialogue;
import io.xeros.content.wogw.WogwContributeInterface;
import io.xeros.model.*;
import io.xeros.content.bosses.nightmare.NightmareConstants;
import io.xeros.content.combat.core.AttackEntity;
import io.xeros.content.combat.EntityDamageQueue;
import io.xeros.content.combat.formula.MeleeMaxHit;
import io.xeros.content.combat.melee.CombatPrayer;
import io.xeros.content.combat.melee.MeleeData;
import io.xeros.content.combat.weapon.CombatStyle;
import io.xeros.content.miniquests.MageArena;
import io.xeros.content.skills.Skill;
import io.xeros.content.vote_panel.VotePanelManager;
import io.xeros.content.vote_panel.VoteUser;
import io.xeros.model.collisionmap.RegionProvider;
import io.xeros.model.collisionmap.doors.Location;
import io.xeros.model.controller.Controller;
import io.xeros.model.controller.ControllerRepository;
import io.xeros.model.entity.player.lock.PlayerLock;
import io.xeros.model.entity.player.lock.Unlocked;
import io.xeros.model.entity.player.migration.PlayerMigrationRepository;
import io.xeros.model.entity.player.mode.ModeRevertType;
import io.xeros.model.entity.player.mode.group.GroupIronmanGroup;
import io.xeros.model.entity.player.mode.group.GroupIronmanRepository;
import io.xeros.model.entity.player.packets.ChangeAppearance;
import io.xeros.model.entity.player.save.PlayerAddresses;
import io.xeros.model.items.*;
import io.xeros.model.StringInput;
import io.xeros.model.multiplayersession.flowerpoker.FlowerPoker;
import io.xeros.model.multiplayersession.flowerpoker.FlowerPokerHand;
import io.xeros.model.tickable.Tickable;
import io.xeros.model.tickable.TickableContainer;
import io.xeros.model.definitions.ItemDef;
import io.xeros.model.entity.EntityReference;
import io.xeros.model.entity.player.save.PlayerSave;
import io.xeros.model.multiplayersession.MultiplayerSessionFinalizeType;
import io.xeros.model.timers.TickTimer;
import io.xeros.net.PacketBuilder;
import io.xeros.model.cycleevent.CycleEventHandler;
import io.xeros.model.cycleevent.Event;
import io.xeros.model.cycleevent.impl.MinigamePlayersEvent;
import io.xeros.model.cycleevent.impl.RunEnergyEvent;
import io.xeros.model.cycleevent.impl.SkillRestorationEvent;
import io.xeros.content.item.lootable.unref.CoinBagBuldging;
import io.xeros.content.achievement.AchievementHandler;
import io.xeros.content.achievement.Achievements;
import io.xeros.content.achievement_diary.AchievementDiaryManager;
import io.xeros.content.achievement_diary.RechargeItems;
import io.xeros.content.bosses.godwars.God;
import io.xeros.content.bosses.godwars.Godwars;
import io.xeros.content.bosses.godwars.GodwarsEquipment;
import io.xeros.content.bosses.zulrah.Zulrah;
import io.xeros.content.cheatprevention.RandomEventInterface;
import io.xeros.content.collection_log.CollectionLog;
import io.xeros.content.combat.CombatItems;
import io.xeros.content.combat.Hitmark;
import io.xeros.content.combat.death.PlayerDeath;
import io.xeros.content.combat.magic.CombatSpellData;
import io.xeros.content.combat.melee.QuickPrayers;
import io.xeros.content.combat.stats.BossTimers;
import io.xeros.content.combat.stats.NPCDeathTracker;
import io.xeros.content.combat.pvp.Killstreak;
import io.xeros.content.event.eventcalendar.EventCalendar;
import io.xeros.content.event.eventcalendar.EventChallenge;
import io.xeros.content.item.lootable.impl.VoteMysteryBox;
import io.xeros.content.item.lootable.impl.NormalMysteryBox;
import io.xeros.content.item.lootable.impl.PvmCasket;
import io.xeros.content.item.lootable.impl.SuperMysteryBox;
import io.xeros.content.item.lootable.impl.UltraMysteryBox;
import io.xeros.content.item.lootable.unref.CoinBagLarge;
import io.xeros.content.item.lootable.unref.CoinBagMedium;
import io.xeros.content.item.lootable.unref.CoinBagSmall;
import io.xeros.content.item.lootable.unref.DailyGearBox;
import io.xeros.content.item.lootable.unref.DailySkillBox;
import io.xeros.content.items.Degrade;
import io.xeros.content.items.pouch.GemBag;
import io.xeros.content.items.pouch.HerbSack;
import io.xeros.content.items.pouch.RunePouch;
import io.xeros.content.lootbag.LootingBag;
import io.xeros.content.minigames.bounty_hunter.BountyHunter;
import io.xeros.content.minigames.fight_cave.FightCave;
import io.xeros.content.minigames.inferno.Inferno;
import io.xeros.content.minigames.pest_control.PestControl;
import io.xeros.content.minigames.pest_control.PestControlRewards;
import io.xeros.content.minigames.pk_arena.Highpkarena;
import io.xeros.content.minigames.pk_arena.Lowpkarena;
import io.xeros.content.minigames.raids.RaidConstants;
import io.xeros.content.minigames.warriors_guild.WarriorsGuild;
import io.xeros.content.minigames.xeric.XericLobby;
import io.xeros.content.polls.PollTab;
import io.xeros.content.preset.Preset;
import io.xeros.content.prestige.PrestigeSkills;
import io.xeros.content.skills.Agility;
import io.xeros.content.skills.ExpLock;
import io.xeros.content.skills.SkillInterfaces;
import io.xeros.content.skills.agility.AgilityHandler;
import io.xeros.content.skills.agility.impl.BarbarianAgility;
import io.xeros.content.skills.agility.impl.GnomeAgility;
import io.xeros.content.skills.agility.impl.Lighthouse;
import io.xeros.content.skills.agility.impl.Shortcuts;
import io.xeros.content.skills.agility.impl.WildernessAgility;
import io.xeros.content.skills.agility.impl.rooftop.RooftopAlkharid;
import io.xeros.content.skills.agility.impl.rooftop.RooftopCanafis;
import io.xeros.content.skills.agility.impl.rooftop.RooftopDraynor;
import io.xeros.content.skills.agility.impl.rooftop.RooftopFalador;
import io.xeros.content.skills.agility.impl.rooftop.RooftopPollnivneach;
import io.xeros.content.skills.agility.impl.rooftop.RooftopRellekka;
import io.xeros.content.skills.farming.Farming;
import io.xeros.content.skills.fletching.Fletching;
import io.xeros.content.skills.herblore.Herblore;
import io.xeros.content.skills.hunter.Hunter;
import io.xeros.content.skills.mining.Mining;
import io.xeros.content.skills.prayer.Prayer;
import io.xeros.content.skills.slayer.Slayer;
import io.xeros.content.skills.smithing.Smelting;
import io.xeros.content.skills.smithing.Smithing;
import io.xeros.content.skills.smithing.SmithingInterface;
import io.xeros.content.skills.thieving.Thieving;
import io.xeros.content.titles.Titles;
import io.xeros.content.tournaments.TourneyManager;
import io.xeros.content.trails.TreasureTrails;
import io.xeros.model.entity.Entity;
import io.xeros.model.entity.HealthStatus;
import io.xeros.model.entity.npc.NPC;
import io.xeros.model.entity.npc.NPCHandler;
import io.xeros.model.entity.npc.pets.PetHandler;
import io.xeros.model.entity.player.mode.Mode;
import io.xeros.model.entity.player.mode.ModeType;
import io.xeros.model.entity.player.mode.RegularMode;
import io.xeros.model.items.bank.Bank;
import io.xeros.model.items.bank.BankPin;
import io.xeros.model.lobby.LobbyManager;
import io.xeros.model.lobby.LobbyType;
import io.xeros.model.multiplayersession.MultiplayerSessionStage;
import io.xeros.model.multiplayersession.MultiplayerSessionType;
import io.xeros.model.multiplayersession.duel.Duel;
import io.xeros.model.multiplayersession.duel.DuelSession;
import io.xeros.model.multiplayersession.trade.Trade;
import io.xeros.model.shops.ShopAssistant;
import io.xeros.model.world.Clan;
import io.xeros.net.Packet;
import io.xeros.net.login.LoginReturnCode;
import io.xeros.net.login.RS2LoginProtocol;
import io.xeros.net.outgoing.UnnecessaryPacketDropper;
import io.xeros.sql.etc.HiscoresUpdateQuery;
import io.xeros.sql.outlast.OutlastLeaderboardAdd;
import io.xeros.util.ISAACCipher;
import io.xeros.util.Misc;
import io.xeros.util.Stopwatch;
import io.xeros.util.Stream;
import io.xeros.util.discord.Discord;
import io.xeros.util.logging.player.ChangeAddressLog;
import io.xeros.util.logging.player.ConnectionLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.time.temporal.ChronoUnit.DAYS;

public class Player extends Entity {

    private static Logger logger = LoggerFactory.getLogger(Player.class);

    public static final int playerHat = 0;
    public static final int playerCape = 1;
    public static final int playerAmulet = 2;
    public static final int playerWeapon = 3;
    public static final int playerChest = 4;
    public static final int playerShield = 5;
    public static final int playerLegs = 7;
    public static final int playerHands = 9;
    public static final int playerFeet = 10;
    public static final int playerRing = 12;
    public static final int playerArrows = 13;
    public static final int playerAttack = 0;
    public static final int playerDefence = 1;
    public static final int playerStrength = 2;
    public static final int playerHitpoints = 3;
    public static final int playerRanged = 4;
    public static final int playerPrayer = 5;
    public static final int playerMagic = 6;
    public static final int playerCooking = 7;
    public static final int playerWoodcutting = 8;
    public static final int playerFletching = 9;
    public static final int playerFishing = 10;
    public static final int playerFiremaking = 11;
    public static final int playerMining = 14;
    public static final int playerHerblore = 15;
    public static final int playerAgility = 16;
    public static final int playerThieving = 17;
    public static final int playerSlayer = 18;
    public static final int playerFarming = 19;
    public static final int playerRunecrafting = 20;


    public int[] tempInventory = new int[28], tempInventoryN = new int[28], tempEquipment = new int[28], tempEquipmentN = new int[28];
    public boolean rubyBoltSpecial;
    public int bryophytaStaffCharges;
    public long lastManualSeedPlant, lastForcedSeedPlant;
    public int flowerPokerWins, flowerPokerLoses, flowerPokerGames;
    public long biggestFlowerPokerPotWon;
    public long biggestFlowerPokerPotLost;

    public void saveItemsForMinigame() {
        /**
         * Clones items
         */
        this.tempInventory = this.playerItems.clone();
        this.tempInventoryN = this.playerItemsN.clone();
        this.tempEquipment = this.playerEquipment.clone();
        this.tempEquipmentN = this.playerEquipmentN.clone();
        /**
         * Deletes
         */
        this.getItems().deleteAllItems();
        /**
         * Refreshes items
         */
        getItems().addContainerUpdate(ContainerUpdate.INVENTORY);
        getItems().addContainerUpdate(ContainerUpdate.EQUIPMENT);
    }

    public void restoreItemsForMinigame() {
        /**
         * Clones items
         */
        this.playerItems = this.tempInventory.clone();
        this.playerItemsN = this.tempInventoryN.clone();
        this.playerEquipment = this.tempEquipment.clone();
        this.playerEquipmentN = this.tempEquipmentN.clone();
        /**
         * Restores
         */
        this.tempInventory = new int[28];
        this.tempInventoryN = new int[28];
        this.tempEquipment = new int[14];
        this.tempEquipmentN = new int[14];
        /**
         * Refreshes items
         */
        getItems().addContainerUpdate(ContainerUpdate.INVENTORY);
        getItems().addContainerUpdate(ContainerUpdate.EQUIPMENT);
    }

    private Channel session;
    public Stream inStream;
    public Stream outStream;
    private static final Stream appearanceUpdateBlockCache;
    private long lastPacketReceived = System.currentTimeMillis();
    private Queue<Integer> previousPackets = new ConcurrentLinkedQueue<>();
    public int mapRegionX;
    public int mapRegionY;
    public int absX;
    public int absY;
    public int lastX;
    public int lastY;
    public int currentX;
    public int currentY;
    public int heightLevel;
    public int dir1 = -1;
    public int dir2 = -1;
    public int poimiX;
    public int poimiY;
    public int playerListSize;
    public int wQueueReadPtr;
    public int wQueueWritePtr;
    private int teleportToX = -1;
    private int teleportToY = -1;
    public int face = -1;
    public int FocusPointX = -1;
    public int FocusPointY = -1;
    public int newWalkCmdSteps;
    public int playerMagicBook;
    public final int walkingQueueSize = 50;
    public int[] walkingQueueX = new int[walkingQueueSize];
    public int[] walkingQueueY = new int[walkingQueueSize];
    private int[] newWalkCmdX = new int[walkingQueueSize];
    private int[] newWalkCmdY = new int[walkingQueueSize];
    protected int[] travelBackX = new int[walkingQueueSize];
    protected int[] travelBackY = new int[walkingQueueSize];

    public static final int maxPlayerListSize = Configuration.MAX_PLAYERS;
    public static final int maxNPCListSize = Configuration.MAX_NPCS_IN_LOCAL_LIST;
    public Player[] playerList = new Player[maxPlayerListSize];
    public byte[] playerInListBitmap = new byte[(Configuration.MAX_PLAYERS + 7) >> 3];

    public NPC[] npcList = new NPC[maxNPCListSize];
    public int npcListSize;

    private final HashSet<GroundItem> localGroundItems = new HashSet<>();

    private byte[] chatText = new byte[4096];
    private byte chatTextSize;

    private int migrationVersion;
    public EntityReference lastAttackedEntity = EntityReference.getReference(null);
    public EntityReference lastDefend;
    public long lastDefendTime;
    public int oldNpcIndex;
    public int playerAttackingIndex;
    public int npcAttackingIndex;
    public int mask400Var1 = -1;
    public int mask400Var2 = -1;
    public int forceMovementDirection = -1;
    public int prayerId = -1;
    public int headIcon = -1;
    public int headIconPk = -1;
    protected RightGroup rights;
    public AttackEntity attacking = new AttackEntity(this);
    private DialogueBuilder dialogueBuilder = null;
    public StringInput stringInputHandler;
    public AmountInput amountInputHandler;
    private long aggressionTimer = System.currentTimeMillis();
    private boolean printAttackStats = Server.isTest();
    private boolean printDefenceStats = Server.isTest();
    private boolean helpCcMuted = false;
    private boolean gambleBanned = false;
    public long lastDialogueSkip = 0;
    public boolean lastDialogueNewSystem;
    public boolean gargoyleStairsUnlocked;
    private Controller controller;
    private Controller loadedController;
    private boolean joinedIronmanGroup;
    private long lastDatabaseAccess;

    private PlayerLock lock = new Unlocked();

    /**
     * THe Combat configurations for the player
     */
    private final CombatConfig combatConfigs = new CombatConfig(this);

    public CombatConfig getCombatConfigs() { return this.combatConfigs; }

    public void resetAggressionTimer() {
        aggressionTimer = System.currentTimeMillis();
    }

    public boolean isAggressionTimeout(Player player) {
        if (Boundary.isIn(player, Boundary.GODWARS_AREA) || Boundary.isIn(player, Boundary.CERBERUS_BOSSROOMS)) {
            return false;
        }
        return System.currentTimeMillis() - aggressionTimer >= TimeUnit.MINUTES.toMillis(15);
    }

    private boolean receivedCalendarCosmeticJune2021;
    public long serpHelmCombatTicks;
    //FOE
    public int currentExchangeItem;
    public int currentExchangeItemAmount;
    // Interface checks
    public boolean inTradingPost;
    public boolean inBank;
    public boolean inUimBank;
    public boolean unlockedUltimateChest;
    public boolean inPresets;
    public boolean inDonatorBox;
    public boolean inLamp;
    // Raids
    public int xericDamage;
    public int raidCount;
    public int tobCompletions;
    public int pollOption;
    // Bank
    private Bank bank;
    public boolean placeHolderWarning;
    public int lastPlaceHolderWarning;
    public boolean placeHolders;
    public int previousTab;
    // Tournaments
    public int tourneyX;
    public int tourneyY;
    public boolean canAttack = true;
    public ArrayList<Integer> tourneyItemsReceived = new ArrayList<>();
    // Presets
    public boolean presetViewingDefault;
    public int presetViewingIndex;
    public Preset currentPreset;
    public io.xeros.content.preset.Set lastPreset;
    public boolean viewingInitialPreset;
    public boolean viewingPresets;
    public long lastPresetClick;
    // Tournaments
    private List<SkillExperience> outlastSkillBackup = new ArrayList<>();
    public int magicBookBackup;
    // Collection log

    private CollectionLog viewingCollectionLog;
    private CollectionLog collectionLog = new CollectionLog();

    public List<GameItem> dropItems;
    public CollectionLog.CollectionTabType collectionLogTab;
    public int previousSelectedCell;
    public int previousSelectedTab;
    // Vote panel
    public long dropBoostStart;
    public boolean usedReferral;
    // Teleporting
    public int lastTeleportX;
    public int lastTeleportY;
    public int lastTeleportZ;
    public int homeTeleport = 50;
    public int teleGfx;
    public int teleGfxTime;
    public int teleGfxDelay;
    public int teleEndAnimation;
    public int teleHeight;
    public int teleSound;
    public int teleX;
    public int teleY;
    public int teleAction;
    private final Inventory inventory = new Inventory(this);
    // Config
    public boolean debugMessage = Server.isDebug(); // On by default on debug mode
    public boolean barbarian;
    public boolean breakVials;
    public boolean collectCoins;
    private boolean runningToggled = true;
    // Trading post.
    public long lastTradingPostView;
    public boolean inSelecting;
    public boolean isListing;
    public int item;
    public int uneditItem;
    public int quantity;
    public int price;
    public int pageId = 1;
    public int searchId;
    public String lookup;
    public List<Integer> saleResults;
    public List<Integer> saleItems = Lists.newArrayList();
    public List<Integer> saleAmount = Lists.newArrayList();
    public List<Integer> salePrice = Lists.newArrayList();
    public int[] historyItems = new int[15];
    public int[] historyItemsN = new int[15];
    public int[] historyPrice = new int[15];
    private final RooftopAlkharid rooftopAlkharid = new RooftopAlkharid();
    private final RooftopFalador rooftopFalador = new RooftopFalador();
    private final RooftopDraynor rooftopDraynor = new RooftopDraynor();
    private final RooftopCanafis rooftopCanafis = new RooftopCanafis();
    private final RooftopPollnivneach rooftopPollnivneach = new RooftopPollnivneach();
    private final RooftopRellekka rooftopRellekka = new RooftopRellekka();
    private final BarbarianAgility barbarianAgility = new BarbarianAgility();
    // Boxes
    public int boxCurrentlyUsing;
    private final YoutubeMysteryBox youtubeMysteryBox = new YoutubeMysteryBox(this);
    private final UltraMysteryBox ultraMysteryBox = new UltraMysteryBox(this);
    private final NormalMysteryBox normalMysteryBox = new NormalMysteryBox(this);
    private final SuperMysteryBox superMysteryBox = new SuperMysteryBox(this);
    private final FoeMysteryBox foeMysteryBox = new FoeMysteryBox(this);
    private final SlayerMysteryBox slayerMysteryBox = new SlayerMysteryBox(this);
    private final CoinBagSmall coinBagSmall = new CoinBagSmall(this);
    private final CoinBagMedium coinBagMedium = new CoinBagMedium(this);
    private final CoinBagLarge coinBagLarge = new CoinBagLarge(this);
    private final CoinBagBuldging coinBagBuldging = new CoinBagBuldging(this);
    private final VoteMysteryBox voteMysteryBox = new VoteMysteryBox();
    private final PvmCasket pvmCasket = new PvmCasket();
    private final DailyGearBox dailyGearBox = new DailyGearBox(this);
    private final DailySkillBox dailySkillBox = new DailySkillBox(this);
    // Combat
    private final EntityDamageQueue entityDamageQueue = new EntityDamageQueue(this);
    private BountyHunter bountyHunter = new BountyHunter(this);
    private final Zulrah zulrah = new Zulrah(this);
    private Entity targeted;
    // Minigames
    private final MageArena mageArena = new MageArena(this);
    private final PestControlRewards pestControlRewards = new PestControlRewards(this);
    private final WarriorsGuild warriorsGuild = new WarriorsGuild(this);
    // Items
    private RunePouch runePouch = new RunePouch(this);
    private HerbSack herbSack = new HerbSack(this);
    private GemBag gemBag = new GemBag(this);
    private final RechargeItems rechargeItems = new RechargeItems(this);
    private LootingBag lootingBag = new LootingBag(this);
    public int[] degradableItem = new int[Degrade.getMaximumItems()];
    public boolean[] claimDegradableItem = new boolean[Degrade.getMaximumItems()];
    // Skilling
    private final ExpLock explock = new ExpLock(this);
    private final PrestigeSkills prestigeskills = new PrestigeSkills(this);
    private final Mining mining = new Mining(this);
    public Smelting.Bars bar;
    // Instances..
    private PlayerParty party = null;
    private final TobContainer tobContainer = new TobContainer(this);
    private final TeleportInterface teleportInterface = new TeleportInterface(this);
    private final Questing questing = new Questing(this);
    private final NotificationsTab notificationsTab = new NotificationsTab(this);
    private final DonationRewards donationRewards = new DonationRewards(this);
    private final WogwContributeInterface wogwContributeInterface = new WogwContributeInterface(this);
    private final Farming farming = new Farming(this);
    private final DailyRewards dailyRewards = new DailyRewards(this);
    private Cannon cannon;
    public final Stopwatch last_trap_layed = new Stopwatch();
    public List<Integer> dropInterfaceSearchList = new ArrayList<>();
    private final QuickPrayers quick = new QuickPrayers();
    private final QuestTab questTab = new QuestTab(this);
    private final EventCalendar eventCalendar = new EventCalendar(this);
    private final RandomEventInterface randomEventInterface = new RandomEventInterface(this);
    private final NPCDeathTracker npcDeathTracker = new NPCDeathTracker(this);
    private final BossTimers bossTimers = new BossTimers(this);
    private final UnnecessaryPacketDropper packetDropper = new UnnecessaryPacketDropper();
    private LocalDate lastVote = LocalDate.of(1970, 1, 1);
    private LocalDate lastVotePanelPoint = LocalDate.of(1970, 1, 1);
    private long lastContainerSearch;
    private AchievementHandler achievementHandler;
    private String macAddress;
    private String uuid = "";
    private final Duel duelSession = new Duel(this);
    private Player itemOnPlayer;
    private Killstreak killstreaks;
    private Mode mode = new RegularMode(ModeType.STANDARD);
    private ModeRevertType modeRevertType = ModeRevertType.STANDARD;
    private final ModeSelection modeSelection = new ModeSelection(this);
    private final Trade trade = new Trade(this);
    public ItemAssistant itemAssistant = new ItemAssistant(this);
    private final ShopAssistant shopAssistant = new ShopAssistant(this);
    private final PlayerAssistant playerAssistant = new PlayerAssistant(this);
    private final CombatItems combatItems = new CombatItems(this);
    private final ActionHandler actionHandler = new ActionHandler(this);
    private final DialogueHandler dialogueHandler = new DialogueHandler(this);
    private final FriendsList friendsList = new FriendsList(this);
    private final Queue<io.xeros.net.Packet> queuedPackets = new ConcurrentLinkedQueue<>();
    private final Queue<Packet> priorityPackets = new ConcurrentLinkedQueue<>();
    private final Potions potions = new Potions(this);
    private final Food food = new Food(this);
    private final SkillInterfaces skillInterfaces = new SkillInterfaces(this);
    private final ChargeTrident chargeTrident = new ChargeTrident(this);
    private PlayerMovementState movementState = PlayerMovementState.getDefault();
    private Slayer slayer;
    private final AgilityHandler agilityHandler = new AgilityHandler();
    private final PointItems pointItems = new PointItems(this);
    private final GnomeAgility gnomeAgility = new GnomeAgility();
    private final WildernessAgility wildernessAgility = new WildernessAgility();
    private final Shortcuts shortcuts = new Shortcuts();
    private final Lighthouse lighthouse = new Lighthouse();
    private final Agility agility = new Agility(this);
    private final Prayer prayer = new Prayer(this);
    private final Smithing smith = new Smithing(this);
    private FightCave fightcave;
    private final SmithingInterface smithInt = new SmithingInterface(this);
    private final Herblore herblore = new Herblore(this);
    private final Thieving thieving = new Thieving(this);
    private final Fletching fletching = new Fletching(this);
    private final Godwars godwars = new Godwars(this);
    private final TreasureTrails trails = new TreasureTrails(this);
    private Optional<ItemCombination> currentCombination = Optional.empty();
    private List<God> equippedGodItems;
    private Titles titles = new Titles(this);

    // Consumable item timers
    private final TickTimer foodTimer = new TickTimer();
    private final TickTimer potionTimer = new TickTimer();

    /**
     * The {@link TickTimer} associated with combo eating
     */
    private final TickTimer comboTimer = new TickTimer();

    public Clan clan;
    private final CollectionBox collectionBox = new CollectionBox();

    private final PerduLostPropertyShop perduLostPropertyShop = new PerduLostPropertyShop();
    private final FlowerPoker flowerPoker = new FlowerPoker(this);


    /**
     * Actions queued from any thread.
     */
    private final Queue<Consumer<Player>> queuedActions = new ConcurrentLinkedQueue<>();
    private final Queue<Consumer<Player>> queuedLoginActions = new ArrayDeque<>();
    private final List<TickableContainer<Player>> tickables = new ArrayList<>();
    private TickableContainer<Player> tickable = null;
    public int diariesCompleted;
    // Combat vars
    public int underAttackByPlayer;
    public int underAttackByNpc;
    public int autoRet;
    public int specBarId;
    public int playerFollowingIndex;
    public int skullTimer;
    public int lastNpcAttacked;
    public int autocastId;
    public int followDistance;
    public int npcFollowingIndex;
    public int arrowUsedOnAttack = -1;
    public int killcount;
    public int deathcount;
    public int hydraAttackCount;
    public int waveId;
    public int rfdWave;
    public int rfdChat;
    public int rfdGloves;
    public int fightCavesWaveType;
    public int rfdRound;
    public int roundNpc;
    public int horrorFromDeep;
    public int sireHits;
    public int slayerTasksCompleted;
    public int pestControlDamage;
    public int gwdAltarTimer;
    public int dreamSpellTimer;
    public double specAmount = 10;
    public double prayerPoint = 1.0;
    // PvP Weapons

    /**
     * Manages PvP weapons for players
     */
    private PvpWeapons pvpWeapons = new PvpWeapons(this);

    /**
     * Manages the charges for the Tome of Fire
     */
    private TomeOfFire tomeOfFire = new TomeOfFire(this);

    public int braceletEtherCount;
    public int elvenCharge;
    public int crystalBowArrowCount;
    // Skill
    public int unfPotHerb;
    public int unfPotAmount;
    public int smeltAmount;
    public int smeltEventId = 5567;
    public int smithingCounter;
    public int grimyHerbToClean;
    public int grimyHerbAmount;
    // Points
    public int pkp;
    public int bossPoints;
    public boolean bossPointsRefund;
    public int achievementPoints;
    public int raidPoints;
    public int votePoints;
    public int bloodPoints;
    public int pcPoints;
    public int donatorPoints;
    public int loyaltyPoints;
    public int voteKeyPoints;
    // Interfaces
    public int lastClickedItem;
    public int unNoteItemId;
    public int dialogueId;
    public int dialogueOptions;
    public int nextChat;
    public int talkingNpc = -1;
    public int dialogueAction;
    public int xInterfaceId;
    public int xRemoveId;
    public int xRemoveSlot;
    public int enterAmountInterfaceId;
    public int safeBoxSlots = 15;
    public int lootValue;
    public int emoteCommandId;
    public int gfxCommandId;
    public int countUntilPoison;
    public int diceItem;
    public int dicePage;
    public int specRestore;
    public int petSummonId;
    public int clickedNpcIndex;
    public int diceMin;
    public int diceMax;
    public int otherDiceId;
    public int playTime;
    public int totalLevel;
    public int killStreak;
    public int xpMaxSkills;
    public int exchangePoints;
    public int totalEarnedExchangePoints;
    public int referallFlag;
    public int amDonated;
    public int showcase;
    public int streak;
    public int lastLoginDate;
    public int clanId = -1;
    // Timers
    public int wildLevel;
    public int teleTimer;
    public int respawnTimer;
    public int teleBlockLength;
    public int operateEquipmentItemId;
    public int antiqueItemResetSkillId;
    public int leatherType = -1;
    public int amountToCook;
    public int rangeEndGFX;
    public int recoilHits;
    public int slaughterCharge;
    public int freezeDelay;
    public int killerId;
    public int weaponUsedOnAttack;
    public int npcClickIndex;
    public int npcType;
    public int oldSpellId;
    private int spellId = -1;
    public int hitDelay;
    public int bowSpecShot;
    public int clickNpcType;
    public int clickObjectType;
    public int myShopId;
    public int tradeStatus;
    public int[] playerAppearance = new int[13];
    public int wearId;
    public int wearSlot;
    public int wearItemInterfaceId;
    public int npcId2;
    public int combatLevel;
    public int playerStandIndex = 808;
    public int playerTurnIndex = 823;
    public int playerWalkIndex = 819;
    public int playerTurn180Index = 820;
    public int playerTurn90CWIndex = 821;
    public int playerTurn90CCWIndex = 822;
    public int playerRunIndex = 824;
    public int destroyingItemId;
    public int itemX;
    public int itemY;
    public int itemId;
    public int objectId;
    public int objectX;
    public int objectY;
    public int objectXOffset;
    public int objectYOffset;
    public int objectDistance;

    public int tablet;
    public int wellItem = -1;
    public int wellItemPrice = -1;
    /**
     * Combat
     */
    public int graniteMaulSpecialCharges;
    private int chatTextColor;
    private int chatTextEffects;
    private int dragonfireShieldCharge;
    private int runEnergy = 100;
    private int x1 = -1;
    private int y1 = -1;
    private int x2 = -1;
    private int y2 = -1;
    private int privateChat;
    private int shayPoints;
    private int arenaPoints;
    private int toxicStaffOfTheDeadCharge;
    private int toxicBlowpipeCharge;
    private int toxicBlowpipeAmmo;
    private int toxicBlowpipeAmmoAmount;
    private int serpentineHelmCharge;
    private int tridentCharge;
    private int toxicTridentCharge;
    private int arcLightCharge;
    private int sangStaffCharge;

    public int getRunningDistanceTravelled() {
        return runningDistanceTravelled;
    }

    private int runningDistanceTravelled;
    private int openInterface;
    public static int playerCrafting = 12;
    public static int playerSmithing = 13;
    protected int numTravelBackSteps;
    protected AtomicInteger packetsReceived = new AtomicInteger();
    public AtomicInteger attemptedPackets = new AtomicInteger();
    /**
     * Arrays
     */
    public ArrayList<int[]> coordinates;
    public int[] masterClueRequirement = new int[4];
    public int[] waveInfo = new int[3];
    public int[] voidStatus = new int[5];
    public int[] playerStats = new int[8];
    public int[] playerBonus = new int[Bonus.values().length];
    public int[] playerEquipment = new int[14];
    public int[] playerEquipmentN = new int[14];
    public int[] playerLevel = new int[25];
    public int[] playerXP = new int[25];
    public long[] gained200mTime = new long[25];
    public int[] runeEssencePouch = new int[3];
    public int[] pureEssencePouch = new int[3];
    public int[] prestigeLevel = new int[25];
    public boolean[] skillLock = new boolean[25];

    // This is done really badly
    // When your grabbing an item here and comparing it, e.g. playerItems[5] == 4151, do playerItems[5] == 4151 + 1
    // You can also do playerItems[5] - 1 == 4151
    public int[] playerItems = new int[28];

    public int[] playerItemsN = new int[28];
    public int[] counters = new int[20];
    public int[] raidsDamageCounters = new int[15];

    public int[] GateKeeperCounter = new int[1]; //idk why we would put it in in raidsCounters but I hope I don't break nothing loool

    public boolean[] maxCape = new boolean[5];
    public int[][] playerSkillProp = new int[20][15];
    public boolean receivedStarter;
    public boolean combatFollowing;
    /**
     * Strings
     */
    public String CERBERUS_ATTACK_TYPE = "";
    public String forcedText = "null";
    public String connectedFrom = "";
    private String loginName;
    private String displayName;
    private long displayNameLong;
    public String playerPass;
    public String barType = "";
    public String playerTitle = "";
    public String rottenPotatoOption = "";
    private String lastClanChat = "";
    private String revertOption = "";
    private String konarSlayerLocation;
    public String lastTask = "";

    /**
     * Booleans
     */
    public boolean[] playerSkilling = new boolean[20];
    public boolean[] clanWarRule = new boolean[10];
    public boolean teleportingToDistrict;
    public boolean usingGraniteCannonballs = false;
    public boolean morphed;
    public boolean isIdle;
    public boolean boneOnAltar;
    public boolean dropRateInKills = true;
    public ItemToDestroy destroyItem;
    public boolean acceptAid;
    public boolean settingUnnoteAmount;
    public boolean settingLootValue;
    public boolean didYouKnow = true;
    public boolean documentGraphic;
    public boolean isStuck;
    public boolean hasOverloadBoost;
    public boolean hasDivineCombatBoost;
    public boolean hasDivineRangeBoost;
    public boolean hasDivineMagicBoost;
    public boolean keepTitle;
    public boolean killTitle;
    public boolean settingMin;
    public boolean settingMax;
    public boolean settingBet;
    public boolean playerIsCrafting;
    public boolean viewingRunePouch;
    public boolean hasFollower;
    public boolean updateItems;
    public boolean claimedReward;
    public boolean craftDialogue;
    public boolean battlestaffDialogue;
    public boolean braceletDialogue;
    public boolean isAnimatedArmourSpawned;
    public boolean isSmelting;
    public boolean expLock;
    public boolean buyingX;
    public boolean leverClicked;
    public boolean isBanking = true;
    public boolean isCooking;
    public boolean initialized;
    private boolean forceLogout;
    private boolean disconnected;
    public boolean ruleAgreeButton;
    public boolean isActive;
    public boolean isOverloading;
    public boolean isSkulled;
    public boolean hasMultiSign;
    public boolean saveCharacter;
    public boolean mouseButton;
    public boolean splitChat;
    public boolean chatEffects = true;
    public boolean autocasting;
    public boolean autocastingDefensive;
    public boolean dbowSpec;
    public boolean properLogout;
    private boolean destructed;
    public boolean vengOn;
    private boolean completedTutorial;
    public boolean accountFlagged;
    public boolean doricOption;
    public boolean doricOption2;
    public boolean caOption2;
    public boolean caOption2a;
    public boolean caOption4a;
    public boolean caOption4c;
    public boolean caPlayerTalk1;
    public boolean rfdOption;
    public boolean spawned;
    public boolean hasBankpin;
    public boolean appearanceUpdateRequired = true;
    public boolean canChangeAppearance;
    public boolean isFullBody;
    public boolean isFullHelm;
    public boolean isFullMask;
    public boolean isOperate;
    public boolean usingLamp;
    public boolean normalLamp;
    public boolean antiqueLamp;
    public boolean setPin;
    public boolean teleporting;
    public boolean isWc;
    public boolean multiAttacking;
    public boolean rangeEndGFXHeight;
    public boolean playerIsFiremaking;
    public boolean stopPlayerSkill;
    public boolean stopPlayerPacket;
    public boolean ignoreDefence;
    public boolean usingArrows;
    public boolean usingOtherRangeWeapons;
    public boolean usingCross;
    public boolean usingBallista;
    public boolean spellSwap;
    public boolean protectItem;
    public boolean usingSpecial;
    public boolean usingRangeWeapon;
    public boolean usingBow;
    public boolean usingMagic;
    public boolean usingClickCast;
    public boolean usingMelee;
    public boolean isMoving;
    public boolean walkingToItem;
    public boolean isShopping;
    public boolean updateShop;
    public boolean forcedChatUpdateRequired;
    public boolean inDuel;
    public boolean inTrade;
    public boolean tradeResetNeeded;
    public boolean smeltInterface;
    public boolean usingGlory;
    public boolean usingSkills;
    public boolean fishing;
    public boolean takeAsNote;
    public boolean swaping;
    public boolean isNpc;
    public boolean inPits;
    public boolean didTeleport;
    public boolean mapRegionDidChange;
    public boolean inArdiCC;
    public boolean attackSkill;
    public boolean strengthSkill;
    public boolean defenceSkill;
    public boolean mageSkill;
    public boolean rangeSkill;
    public boolean prayerSkill;
    public boolean healthSkill;
    public boolean pkDistrict;
    public boolean crystalDrop;
    public boolean hourlyBoxToggle = true;
    public boolean fracturedCrystalToggle = true;
    public boolean boltTips;
    public boolean arrowTips;
    public boolean spectatingTournament;
    public boolean javelinHeads;
    public boolean hasPotionBoost;
    public boolean canHarvestHespori = false;
    public boolean canLeaveHespori = false;
    public boolean canEnterHespori;
    private boolean dropWarning = true;
    private boolean alchWarning = true;
    private boolean chatTextUpdateRequired;
    private boolean newWalkCmdIsRunning;
    private boolean forceMovement;
    private boolean godmode;
    private boolean safemode;
    private boolean forceMovementActive;
    public boolean insidePost;

    /**
     * @return the forceMovement
     */
    public boolean isForceMovementActive() {
        return forceMovementActive;
    }

    protected boolean faceUpdateRequired;
    private final AchievementDiaryManager diaryManager = new AchievementDiaryManager(this);
    public long totalGorillaDamage;
    public long totalMissedGorillaHits;
    public long totalHunllefDamage;
    public long totalMissedHunllefHits;
    public long lastImpling;
    public long lastWheatPass;
    public long lastCloseOfInterface;
    public long lastPerformedEmote;
    public long lastPickup;
    public long lastTeleport;
    public long lastMarkDropped;
    public long lastObstacleFail;
    public long lastDropTableSelected;
    public long lastDropTableSearch;
    public long buySlayerTimer;
    public long buyPestControlTimer;
    public long fightCaveLeaveTimer;
    public long infernoLeaveTimer;
    public long lastFire;
    public long lastMove;
    public long bonusXpTime;
    public long lastSmelt;
    public long lastMysteryBox;
    public long lastYell;
    public long diceDelay;
    public long lastPlant;
    public long lastCast;
    public long miscTimer;
    public long jailEnd;
    public long muteEnd;
    public long stopPrayerDelay;
    public long lastAntifirePotion;
    public long antifireDelay;
    public long staminaDelay;
    public long lastHeart;
    public long openCasketTimer;
    public long lastSpear = System.currentTimeMillis();
    public long lastProtItem = System.currentTimeMillis();
    public long lastVeng = System.currentTimeMillis();
    public long switchDelay = System.currentTimeMillis();
    public long potDelay = System.currentTimeMillis();
    public long protMageDelay = System.currentTimeMillis();
    public long protMeleeDelay = System.currentTimeMillis();
    public long protRangeDelay = System.currentTimeMillis();
    public long lastLockPick = System.currentTimeMillis();
    public long alchDelay = System.currentTimeMillis();
    public long specDelay = System.currentTimeMillis();
    public long teleBlockStartMillis;
    public long godSpellDelay = System.currentTimeMillis();
    public long singleCombatDelay = System.currentTimeMillis();
    public long singleCombatDelay2 = System.currentTimeMillis();
    public long reduceStat = System.currentTimeMillis();
    public long logoutDelay = System.currentTimeMillis();
    public long cerbDelay = System.currentTimeMillis();
    public long chestDelay = System.currentTimeMillis();
    private long revertModeDelay;
    private long experienceCounter;
    private long bestZulrahTime;
    private long lastOverloadBoost;
    private long nameAsLong;
    private long lastDragonfireShieldAttack;
    public long clickDelay;
    public long lastHealChest = System.currentTimeMillis();
    public boolean hasPetSpawned;
    private boolean receivedVoteStreakRefund; // TODO DELETE ME AFTER September 29th 2021
    /**
     * The amount of time before we are out of combat.
     */
    private long inCombatDelay = Configuration.IN_COMBAT_TIMER;

    public void setInCombatDelay(long inCombatDelay) {
        this.inCombatDelay = inCombatDelay;
    }

    /**
     * Others
     */
    public ArrayList<String> lastConnectedFrom = new ArrayList<>();
    public ArrayList<Integer> attackedPlayers = new ArrayList<Integer>();

    @Override
    public String toString() {
        return String.format("player[loginName=%s, displayName=%s, ip=%s, mac=%s, uuid=%s]", getLoginName(),
                getDisplayName(), getIpAddress(), getMacAddress(), getUUID());
    }

    public String getNamesDescription() {
        return String.format("[login=%s, display=%s]", getLoginName(), getDisplayName());
    }

    /**
     * Gets a description of a player including their state.
     * Append to every logged error. Needs expanded to including other states.
     *
     * @return player state description string.
     */
    public String getStateDescription() {
        return String.format("[loginName=%s, displayName=%s, position=%s]", getLoginName(), getDisplayName(), getPosition());
    }

    public Position getPosition() {
        return new Position(absX, absY, heightLevel);
    }

    public boolean isManagement() {
        return getRights().isOrInherits(Right.ADMINISTRATOR, Right.OWNER);
    }

    private boolean bot = false;

    public boolean isBot() {
        return bot;
    }

    public static Player createBot(String username, Right right) {
        return createBot(username, right, Configuration.START_POSITION);
    }

    public static Player createBot(String username, Right right, Position position) {
        username = username.toLowerCase();
        Player player = new Player(null);
        player.getRights().setPrimary(right);
        player.setMode(right.getMode());
        player.saveCharacter = true;
        player.completedTutorial = true;
        player.setLoginName(username);
        player.setDisplayName(player.getLoginName());
        player.macAddress = "";
        player.bot = true;
        player.nameAsLong = Misc.playerNameToInt64(username);
        player.playerPass = "playerbot123";
        player.setIpAddress("");
        player.addQueuedAction(plr -> plr.moveTo(position));

        Server.getIoExecutorService().submit(() -> {
            try {
                LoginReturnCode code = RS2LoginProtocol.loadPlayer(player, player.getLoginNameLower(), LoginReturnCode.SUCCESS, true);
                if (code != LoginReturnCode.SUCCESS) {
                    logger.error("Could not login bot, return code was {}", code);
                    return;
                }

                PlayerHandler.addLoginQueue(player);
                if (Server.isDebug()) { // This is so bots get packet writing for profiling on debug
                    player.outStream = new Stream(new byte[Configuration.BUFFER_SIZE]);
                    player.outStream.currentOffset = 0;
                    player.inStream = new Stream(new byte[Configuration.BUFFER_SIZE]);
                    player.inStream.currentOffset = 0;
                    player.outStream.packetEncryption = new ISAACCipher(new int[]{0, 0, 0, 0});
                    player.inStream.packetEncryption = new ISAACCipher(new int[]{0, 0, 0, 0});
                }
            } catch (Exception e) {
                logger.error("Error loading bot {}", player, e);
            }
        });

        return player;
    }

    public Player(Channel channel) {
        this.session = channel;
        freezeTimer = -6;
        rights = new RightGroup(this, Right.PLAYER);
        for (int i = 0; i < playerItems.length; i++) {
            playerItems[i] = 0;
        }
        for (int i = 0; i < playerItemsN.length; i++) {
            playerItemsN[i] = 0;
        }
        resetSkills();

        ChangeAppearance.generateRandomAppearance(this);

        playerEquipment[playerHat] = -1;
        playerEquipment[playerCape] = -1;
        playerEquipment[playerAmulet] = -1;
        playerEquipment[playerChest] = -1;
        playerEquipment[playerShield] = -1;
        playerEquipment[playerLegs] = -1;
        playerEquipment[playerHands] = -1;
        playerEquipment[playerFeet] = -1;
        playerEquipment[playerRing] = -1;
        playerEquipment[playerArrows] = -1;
        playerEquipment[playerWeapon] = -1;
        heightLevel = 0;
        setTeleportToX(Configuration.START_LOCATION_X);
        setTeleportToY(Configuration.START_LOCATION_Y);
        absX = absY = -1;
        mapRegionX = mapRegionY = -1;
        currentX = currentY = 0;
        resetWalkingQueue();
        if (channel != null) {
            outStream = new Stream(new byte[Configuration.BUFFER_SIZE]);
            outStream.currentOffset = 0;
            inStream = new Stream(new byte[Configuration.BUFFER_SIZE]);
            inStream.currentOffset = 0;
        }
    }

    public PlayerAddresses getValidAddresses() {
        String ip = getIpAddress();
        String mac = null;
        String uuid = null;
        if (getMacAddress() != null && getMacAddress().length() > 0 && !getMacAddress().equals(getIpAddress()))
            mac = getMacAddress();
        if (getUUID() != null && getUUID().length() > 0)
            uuid = getUUID();
        return new PlayerAddresses(ip, mac, uuid);
    }

    /**
     * Reset skills to default.
     */
    public void resetSkills() {
        for (int i = 0; i < playerLevel.length; i++) {
            if (i == 3) {
                playerLevel[i] = 10;
            } else {
                playerLevel[i] = 1;
            }
        }
        for (int i = 0; i < playerXP.length; i++) {
            if (i == 3) {
                playerXP[i] = 1300;
            } else {
                playerXP[i] = 0;
            }
        }
    }

    /**
     * Actions to take when a player's mac/uuid address has a change detected.
     */
    public void setAddressChanged(String type, String previous, String current, boolean staffAlertMessage) {
        addQueuedLoginAction(plr -> {
            String message = Misc.replaceBracketsWithArguments("{} changed {} address", getNamesDescription(), type);
            if (staffAlertMessage) {
                Discord.writeAddressSwapMessage(message);
            } else {
                Discord.writeServerSyncMessage(message);
            }

            Server.getLogging().write(new ChangeAddressLog(this, type, previous, current));

            if (plr.getBankPin().hasBankPin()) {
                setRequiresPinUnlock(true);
                plr.sendMessage("@dre@You're logging in from a different computer, please enter your account pin.");
            }
        });
    }

    public void lock(PlayerLock lock) {
        this.lock = lock;
        debug("Locked: " + lock.getClass().getName());
    }

    public void unlock() {
        lock = new Unlocked();
        debug("Unlocked.");
    }

    public PlayerLock getLock() {
        return lock;
    }

    /**
     * Check if the player has hit the database access rate limit. If not it will set the database access time.
     * @return true if server should refuse access, false and set access time if they aren't at limit.
     */
    public boolean hitDatabaseRateLimit(boolean message) {
        if (System.currentTimeMillis() - lastDatabaseAccess <= 3_000) {
            if (message) sendMessage("You are doing that too often, please wait.");
            return true;
        }
        lastDatabaseAccess = System.currentTimeMillis();
        return false;
    }

    public boolean isApartOfGroupIronmanGroup() {
        return GroupIronmanRepository.getGroupForOnline(this).isPresent();
    }

    public boolean isInIronmanGroupWith(final Player other) {
        var group = GroupIronmanRepository.getGroupForOnline(this);
        if (group.isEmpty()) {
            return false;
        }
        return group.get().getOnline().contains(other);
    }

    public long getTotalXp() {
        long temp = 0;
        for (int i = 0; i < playerXP.length; i++) {
            temp += playerXP[i];
        }
        return temp;
    }

    public boolean viewable(NPC npc, boolean updatingLocalList) {
        if (updatingLocalList) { // Only check against these when updating local npc list, not when adding to local list
            if (npc.teleporting) {
                return false;
            }
        }

        return withinDistance(npc) && !npc.isInvisible() && npc.getInstance() == getInstance()
                && !npc.needRespawn && npc.getIndex() > 0;
    }

    /**
     * Check if this player is on the computer with the specified parameters
     * (or has the same name so is the same player).
     */
    public boolean isSameComputer(long usernameAsLong, String ipAddress, String macAddress) {
        if (Server.isDebug()) {
            logger.debug("Skipping Player#isSameComputer addresses check, only checking usernames.");
            return usernameAsLong == getNameAsLong();
        }
        return usernameAsLong == getNameAsLong() || ipAddress.equals(getIpAddress()) || macAddress.equals(getMacAddress());
    }

    public boolean isSameComputer(Player other) {
        return isSameComputer(other.getNameAsLong(), other.getIpAddress(), other.getMacAddress());
    }

    public boolean ignoreNewPlayerRestriction() {
        return getRights().ignoreNewPlayerRestriction();
    }

    public boolean ignoreNewPlayerRestriction(Player other) {
        return getRights().ignoreNewPlayerRestriction() || other.getRights().ignoreNewPlayerRestriction()
                || getMode().isGroupIronman() || other.getMode().isGroupIronman();
    }

    public boolean hasNewPlayerRestriction() {
        if (Server.isDebug()) return false;
        return !ignoreNewPlayerRestriction() && playTime < Configuration.NEW_PLAYER_RESTRICT_TIME_TICKS;
    }

    /**
     * Check if a player is busy (interface open, etc).
     *
     * @return <code>true</code> if the player is busy and shouldn't be disturbed.
     */
    public boolean isBusy() {
        return openInterface > 0;
    }

    public Bank getBank() {
        if (bank == null) bank = new Bank(this);
        return bank;
    }

    private BankPin pin;
    private boolean requiresPinUnlock;

    public BankPin getBankPin() {
        if (pin == null) pin = new BankPin(this);
        return pin;
    }

    public void sendMessage(String s, int color) {
        sendMessage("<col=" + color + ">" + s + "</col>");
    }

    public int tournamentWins, tournamentPoints, outlastKills, outlastDeaths, tournamentTotalGames;
    public Player tournamentTarget;

    public long tournamentTargetCooldown;

    public void resetVengeance() {
        vengOn = false;
        lastCast = System.currentTimeMillis();
    }

    public void flushOutStream() {
        if (!initialized || session == null || !session.isActive() || outStream == null || outStream.currentOffset == 0)
            return;
        byte[] temp = new byte[outStream.currentOffset];
        System.arraycopy(outStream.buffer, 0, temp, 0, temp.length);
        io.xeros.net.Packet packet = new io.xeros.net.Packet(-1, io.xeros.net.Packet.Type.FIXED, Unpooled.wrappedBuffer(temp));
        session.writeAndFlush(packet);
        getOutStream().currentOffset = 0;
    }

    public void setLoadedController(Controller loadedController) {
        this.loadedController = loadedController;
    }

    private void loadController() {
        if (loadedController != null && loadedController.inBoundaryOrNoBoundaries(this)) {
            setController(loadedController);
        } else {
            setController(ControllerRepository.getOrDefault(this));
        }
        loadedController = null;
    }

    public void setController(Controller controller) {
        if (this.controller != null)
            this.controller.removed(this);
        this.controller = controller;
        controller.added(this);
        if (!isBot())
            logger.debug("Set controller to {}, {}", controller.getKey(), controller);
    }

    public Controller getController() {
        return controller;
    }

    /**
     * If the current controller allows switching automatically
     * {@link Controller#allowSwitch()}, then we will check all boundary
     * controllers and set the controller is a new one is returned.
     */
    public void updateController() {
        if (getController().allowSwitch()) {
            Controller newController = ControllerRepository.getOrDefault(this);
            if (newController != controller) {
                setController(newController);
            }
        }
    }

    public int getSpellId() {
        return spellId;
    }

    public boolean usingGodSpell() {
        return oldSpellId >= 28 && oldSpellId <= 30;
    }

    public boolean hasUnlockedGodSpell(int spell) {
        if (spell == 28)
            return saradominStrikeCasts >= 100;
        else if (spell == 29)
            return clawsOfGuthixCasts >= 100;
        else if (spell == 30)
            return flamesOfZamorakCasts >= 100;
        return true;
    }

    public void setSpellId(int spellId) {
        this.spellId = spellId;
    }

    public int getCombatLevelDifference(Player other) {
        return Math.abs(calculateCombatLevel() - other.calculateCombatLevel());
    }

    public void moveTo(Position position) {
        stopMovement();
        this.teleportToX = position.getX();
        this.teleportToY = position.getY();
        this.heightLevel = position.getHeight();
    }

    public void climbLadderTo(Position position) {
        climbLadderTo(position, null);
    }

    public void climbLadderTo(Position position, Consumer<Player> finishConsumer) {
        startAnimation(position.getHeight() < getHeight() ? 827 : 828);
        setTickable((container, player) -> {
            if (container.getTicks() == 1) {
                container.stop();
                player.moveTo(position);
                if (finishConsumer != null) {
                    finishConsumer.accept(this);
                }
            }
        });
    }

    public int getTeleportToX() {
        return teleportToX;
    }

    public void setTeleportToX(int teleportToX) {
        this.teleportToX = teleportToX;
    }

    public int getTeleportToY() {
        return teleportToY;
    }

    public void setTeleportToY(int teleportToY) {
        this.teleportToY = teleportToY;
    }

    public boolean protectingRange() {
        return this.prayerActive[17];
    }

    public boolean protectingMagic() {
        return this.prayerActive[16];
    }

    public boolean protectingMelee() {
        return this.prayerActive[18];
    }

    public boolean isProtectionPrayersShiftRight() {
        return protectionPrayersShiftRight;
    }

    public void setProtectionPrayersShiftRight(boolean protectionPrayersShiftRight) {
        this.protectionPrayersShiftRight = protectionPrayersShiftRight;
    }

    public boolean isDisconnected() {
        return disconnected;
    }

    public void setDisconnected(boolean disconnected) {
        this.disconnected = disconnected;
    }

    public boolean hunllefDead;
    public int VERIFICATION;

    public void TournamentHiscores(Player c) {
        c.getDH().sendDialogues(983, 311);
    }

    public long disconnectTime;

    public void start(DialogueBuilder dialogueBuilder) {
        this.dialogueBuilder = dialogueBuilder;
        dialogueBuilder.initialise();
        lastDialogueNewSystem = true;
    }

    public boolean canUseGodSpellsOutsideOfMageArena() {
        return this.flamesOfZamorakCasts >= 100 && this.clawsOfGuthixCasts >= 100 && this.saradominStrikeCasts >= 100;
    }

    public void openedInterface(int interfaceId) {
        setOpenInterface(interfaceId);
    }

    public void closedInterface() {
        setOpenInterface(0);
    }

    public boolean isInterfaceOpen(int interfaceId) {
        return getOpenInterface() == interfaceId;
    }

    public void attemptLogout() {
        DuelSession duelSession = (DuelSession) Server.getMultiplayerSessionListener().getMultiplayerSession(this, MultiplayerSessionType.DUEL);
        if (Objects.nonNull(duelSession) && duelSession.getStage().getStage() > MultiplayerSessionStage.REQUEST) {
            if (duelSession.getStage().getStage() >= MultiplayerSessionStage.FURTHER_INTERATION) {
                sendMessage("You are not permitted to logout during a duel. If you forcefully logout you will");
                sendMessage("lose all of your staked items to your opponent.");
            }
        }
        if (!isIdle && underAttackByNpc > 0) {
            sendMessage("You can\'t log out until 10 seconds after the end of combat.");
            return;
        }
        if (underAttackByPlayer > 0) {
            sendMessage("You can\'t log out until 10 seconds after the end of combat.");
            return;
        }

        if (getLock().cannotLogout(this)) {
            sendMessage("You can't logout at the moment.");
            return;
        }

        if (!isDisconnected() && System.currentTimeMillis() - logoutDelay > 1000) {
            properLogout = true;
            setDisconnected(true);
            ConnectedFrom.addConnectedFrom(this, connectedFrom);
        }
    }

    /**
     * Force the player to immediately disconnect from the game.
     */
    public void forceLogout() {
        forceLogout = true;
    }


    public void setDisconnected() {
        setDisconnected(true);
        disconnectTime = System.currentTimeMillis();
    }

    public boolean isReadyToLogout() {
        if (forceLogout)
            return true;
        if (getLock().cannotLogout(this))
            return false;
        return properLogout
                || isDisconnected() && System.currentTimeMillis() - logoutDelay > 10000
                || isDisconnected() && disconnectTime != 0 && (System.currentTimeMillis() - disconnectTime >= 30000)
                || !bot && System.currentTimeMillis() - lastPacketReceived > 30000;
    }

    public void destruct() {
        if (destructed)
            return;
        destructed = true;
        getPA().sendLogout();

        if (session != null && session.isOpen()) {
            session.writeAndFlush(new PacketBuilder(109).toPacket()).addListener(ChannelFutureListener.CLOSE);
            session = null;
        }

        if (party != null) {
            party.remove(this);
        }
        if (getPA().viewingOtherBank) {
            getPA().resetOtherBank();
        }
        if (this.clan != null) {
            this.clan.removeMember(this);
        }

        getFriendsList().onLogout();
        GroupIronmanRepository.onLogout(this);

        getController().onLogout(this);

        clearUpPlayerNPCsForLogout();

        declineTrades();

        if (Configuration.BOUNTY_HUNTER_ACTIVE) {
            if (getBH().hasTarget()) {
                getBH().setWarnings(getBH().getWarnings() + 1);
            }
        }

        potions.resetOverload();
        potions.resetRangeDivine();
        potions.resetMageDivine();
        potions.resetCombatDivine();
        if (getCannon() != null) {
            getCannon().pickup(this, false);
        }
        if (combatLevel >= 100) {
            if (Highpkarena.getState(this) != null) {
                Highpkarena.removePlayer(this, true);
            }
        } else if (combatLevel >= 80 && combatLevel <= 99) {
            if (Lowpkarena.getState(this) != null) {
                Lowpkarena.removePlayer(this, true);
            }
        }
        Hunter.abandon(this, null, true);

        if (getXeric() != null) {
            getXeric().removePlayer(this);
        }
        if (Boundary.isIn(this, Boundary.XERIC_LOBBY)) {
            XericLobby.removePlayer(this);
        }
        if (getRaidsInstance() != null) {
            getRaidsInstance().logout(this);
        }
        if (Vorkath.inVorkath(this)) {
            this.getPA().movePlayer(2272, 4052, 0);
        }
        if (Vorkath.inVorkath(this)) {
            getPA().movePlayer(2272, 4052, 0);
        }
        if (getPA().viewingOtherBank) {
            getPA().resetOtherBank();
        }
        if (Boundary.isIn(this, PestControl.GAME_BOUNDARY)) {
            PestControl.removeGameMember(this);
        }
        if (Boundary.isIn(this, PestControl.LOBBY_BOUNDARY)) {
            PestControl.removeFromLobby(this);
        }
        Server.getMultiplayerSessionListener().removeOldRequests(this);
        Server.getEventHandler().stop(this);
        CycleEventHandler.getSingleton().stopEvents(this);

        if (!getRights().contains(Right.OWNER) && !getRights().contains(Right.OWNER)) {
            Server.getDatabaseManager().batch(Server.getConfiguration().getHiscoresDatabase(), new HiscoresUpdateQuery(this));
            Server.getDatabaseManager().batch(new OutlastLeaderboardAdd(new OutlastLeaderboardEntry(this)));
        }
        removeFromInstance();
        if (clan != null) {
            clan.removeMember(this);
        }
        inStream = null;
        //outStream = null;
        playerListSize = 0;
        npcListSize = 0;
        for (int i = 0; i < maxPlayerListSize; i++) playerList[i] = null;
        for (int i = 0; i < maxNPCListSize; i++) npcList[i] = null;
        if (Server.isTest() && !isBot()) {
            logger.info(Misc.formatPlayerName(getLoginName()) + " is logging out..");
        }
    }

    public void declineTrades() {
        if (Server.getMultiplayerSessionListener().inSession(this, MultiplayerSessionType.TRADE)) {
            Server.getMultiplayerSessionListener().finish(this, MultiplayerSessionFinalizeType.WITHDRAW_ITEMS);
        }

        if (Server.getMultiplayerSessionListener().inSession(this, MultiplayerSessionType.FLOWER_POKER)) {
            Server.getMultiplayerSessionListener().finish(this, MultiplayerSessionFinalizeType.WITHDRAW_ITEMS);
        }

        DuelSession duelSession = (DuelSession) Server.getMultiplayerSessionListener().getMultiplayerSession(this, MultiplayerSessionType.DUEL);
        if (Objects.nonNull(duelSession) && duelSession.getStage().getStage() > MultiplayerSessionStage.REQUEST) {
            if (duelSession.getStage().getStage() < MultiplayerSessionStage.FURTHER_INTERATION) {
                duelSession.finish(MultiplayerSessionFinalizeType.WITHDRAW_ITEMS);
            } else {
                Player winner = duelSession.getOther(this);
                duelSession.setWinner(winner);
                duelSession.finish(MultiplayerSessionFinalizeType.GIVE_ITEMS);
            }
        }
    }

    public boolean isOnline() {
        return getIndex() > 0;
    }

    public void finishLogin() {
        Server.getLogging().write(new ConnectionLog(this, true, null));
        queuedLoginActions.forEach(it -> it.accept(this));

        if (getMode() == Mode.forType(ModeType.STANDARD) && getRights().isIronman()) {
            System.err.println(getLoginName() + " has ironman rights with standard mode, removing ironman rights.");
            Right.IRONMAN_SET.forEach(it -> getRights().remove(it));
            getRights().updatePrimary();
        }

        // Normalise death tracker to fix previous mapping errors
        getNpcDeathTracker().normalise();
        // Old equipment correction?
        for (int i = 0; i < playerEquipment.length; i++) {
            if (playerEquipment[i] == 0) {
                playerEquipment[i] = -1;
                playerEquipmentN[i] = 0;
            }
        }
        if (getOutStream() != null) {
            getOutStream().createFrame(249);
            getOutStream().writeByteA(1);
            getOutStream().writeWordBigEndianA(getIndex());
        }

        loadController();
        getController().onLogin(this);

        if (PlayerHandler.updateRunning)
            getPA().sendUpdateTimer();

        getHealth().requestUpdate();
        GroupIronmanRepository.onLogin(this);
        getFriendsList().onLogin();
        CompletionistCape.onLogin(this);
        getAchievements().onLogin();
        getDiaryManager().setDiariesCompleted();
        graceSum();
        setStopPlayer(false);
        inPresets = false;
        inTradingPost = false;
        inBank = false;
        inLamp = false;
        inDonatorBox = false;
        getSuperMysteryBox().canMysteryBox();
        getNormalMysteryBox().canMysteryBox();
        getUltraMysteryBox().canMysteryBox();
        getYoutubeMysteryBox().canMysteryBox();
        getFoeMysteryBox().canMysteryBox();
        getSlayerMysteryBox().canMysteryBox();
        getPA().updateRunEnergy();
        isFullHelm = ItemDef.forId(playerEquipment[playerHat]).getEquipmentModelType() == EquipmentModelType.FULL_HELMET;
        isFullMask = ItemDef.forId(playerEquipment[playerHat]).getEquipmentModelType() == EquipmentModelType.FULL_MASK;
        isFullBody = ItemDef.forId(playerEquipment[playerChest]).getEquipmentModelType() == EquipmentModelType.FULL_BODY;
        getPA().updateRunningToggle();
        getPA().setConfig(427, acceptAid ? 1 : 0);
        potions.resetOverload();

        if (completedTutorial) {
            sendMessage("@bla@Welcome back to " + Configuration.SERVER_NAME + ", " + getDisplayNameFormatted() + ".");
        } else {
            sendMessage("@bla@Welcome to " + Configuration.SERVER_NAME + ", don't forget to join the <col=255>::discord</col>!");
        }

        if (Reclaim.isReclaimPeriod() && Server.isPublic()) {
            sendMessage("The donation reclaim period is open, use ::reclaim to reclaim old donations.");
        }

        if (Calendar.getInstance().get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY || Calendar.getInstance().get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY || Calendar.getInstance().get(Calendar.DAY_OF_WEEK) == Calendar.FRIDAY) {
            sendMessage("@bla@Bonus XP Weekend is currently [@gre@ACTIVE@bla@]");
        } else if (bonusXpTime > 0) {
            getPA().sendGameTimer(ClientGameTimer.BONUS_XP, TimeUnit.MINUTES, VotePanelManager.getBonusXPTimeInMinutes(this));
        } else if (xpScrollTicks > 0) {
            getPA().sendGameTimer(ClientGameTimer.BONUS_XP, TimeUnit.MINUTES, (int) (xpScrollTicks / 100));
        }
        if (skillingPetRateTicks > 0) {
            getPA().sendGameTimer(ClientGameTimer.BONUS_SKILLING_PET_RATE, TimeUnit.MINUTES, (int) (skillingPetRateTicks / 100));
        }
        if (fasterCluesTicks > 0) {
            getPA().sendGameTimer(ClientGameTimer.BONUS_CLUES, TimeUnit.MINUTES, (int) (fasterCluesTicks / 100));
        }
        if (combatLevel >= 126) {
            getEventCalendar().progress(EventChallenge.HAVE_126_COMBAT);
        }
        if (getRights().getPrimary().equals(Right.HELPER)) {
            PlayerHandler.executeGlobalMessage("[@red@Staff@bla@] <col=255>" + getDisplayNameFormatted() + "@bla@ has just logged in!");
        } else if (getRights().getPrimary().equals(Right.MODERATOR)) {
            PlayerHandler.executeGlobalMessage("[@red@Staff@bla@] <col=255>" + getDisplayNameFormatted() + "@bla@ has just logged in!");
        } else if (getRights().getPrimary().equals(Right.GAME_DEVELOPER)) {
            PlayerHandler.executeGlobalMessage("[@red@Staff@bla@] <col=255>" + getDisplayNameFormatted() + "@bla@ has just logged in!");
        } else if (getRights().getPrimary().equals(Right.ADMINISTRATOR)) {
            PlayerHandler.executeGlobalMessage("[@red@Staff@bla@] <col=255>" + getDisplayNameFormatted() + "@bla@ has just logged in!");
        } else if (getRights().getPrimary().equals(Right.OWNER)) {
            PlayerHandler.executeGlobalMessage("[@red@Staff@bla@] <col=255>" + getDisplayNameFormatted() + "@bla@ has just logged in!");
        }
        if (getSlayer().superiorSpawned) {
            getSlayer().superiorSpawned = false;
        }
        if (getMode().isIronmanType()) {
            ArrayList<RankUpgrade> orderedList = new ArrayList<>(Arrays.asList(RankUpgrade.values()));
            orderedList.sort((one, two) -> Integer.compare(two.amount, one.amount));
            orderedList.stream().filter(r -> amDonated >= r.amount).findFirst().ifPresent(rank -> {
                RightGroup rights = getRights();
                Right right = rank.rights;
                if (!rights.contains(right)) {
                    sendMessage("@blu@Congratulations, your rank has been upgraded to " + right.toString() + ".");
                    sendMessage("@blu@This rank is hidden, but you will have all it\'s perks.");
                    rights.add(right);
                }
            });
        }
        combatLevel = calculateCombatLevel();
        for (int p = 0; p < CombatPrayer.PRAYER.length; p++) {
            // reset prayer glows
            prayerActive[p] = false;
            getPA().sendFrame36(CombatPrayer.PRAYER_GLOW[p], 0);
        }
        accountFlagged = getPA().checkForFlags();
        getPA().sendFrame36(108, 0);
        getPA().sendFrame36(172, 1);
        getPA().resetScreenShake(); // reset screen
        PollTab.updatePollTabDisplay(this);
        setSidebarInterface(0, 2423);
        setSidebarInterface(1, 13917); // Skilltab > 3917
        setSidebarInterface(2, QuestTab.INTERFACE_ID);
        setSidebarInterface(3, 3213);
        setSidebarInterface(4, 1644);
        setSidebarInterface(5, 15608);
        switch (playerMagicBook) {
            case 0:
                setSidebarInterface(6, 938); // modern
                break;
            case 1:
                setSidebarInterface(6, 838); // ancient
                break;
            case 2:
                setSidebarInterface(6, 29999); // ancient
                break;
        }
        if (hasFollower) {
            if (petSummonId > 0) {
                PetHandler.Pets pet = PetHandler.forItem(petSummonId);
                if (pet != null) {
                    PetHandler.spawn(this, pet, true, false);
                }
            }
        }
        for (int m = 0; m < activeMageArena2BossId.length; m++) {
            activeMageArena2BossId[m] = 0;
        }

        if (mageArena2Spawns == null)
            MageArenaII.assignSpawns(this);

        if (splitChat) {
            getPA().sendFrame36(502, 1);
            getPA().sendFrame36(287, 1);
        }
        setSidebarInterface(7, 18128);
        setSidebarInterface(8, 5065);
        setSidebarInterface(9, 5715);
        setSidebarInterface(10, 2449);
        setSidebarInterface(11, 42500); // wrench tab
        setSidebarInterface(12, 147); // run tab
        getPA().showOption(4, 0, "Follow");
        getPA().showOption(5, 0, "Trade with");
        getItems().sendInventoryInterface(3214);
        getItems().setEquipment(playerEquipment[playerHat], 1, playerHat, false);
        getItems().setEquipment(playerEquipment[playerCape], 1, playerCape, false);
        getItems().setEquipment(playerEquipment[playerAmulet], 1, playerAmulet, false);
        getItems().setEquipment(playerEquipment[playerArrows], playerEquipmentN[playerArrows], playerArrows, false);
        getItems().setEquipment(playerEquipment[playerChest], 1, playerChest, false);
        getItems().setEquipment(playerEquipment[playerShield], 1, playerShield, false);
        getItems().setEquipment(playerEquipment[playerLegs], 1, playerLegs, false);
        getItems().setEquipment(playerEquipment[playerHands], 1, playerHands, false);
        getItems().setEquipment(playerEquipment[playerFeet], 1, playerFeet, false);
        getItems().setEquipment(playerEquipment[playerRing], 1, playerRing, false);
        getItems().setEquipment(playerEquipment[playerWeapon], playerEquipmentN[playerWeapon], playerWeapon, false);
        getItems().calculateBonuses();
        MeleeData.setWeaponAnimations(this);
        if (getPrivateChat() > 2) {
            setPrivateChat(0);
        }
        if (getOutStream() != null) {
            getOutStream().createFrame(221);
            getOutStream().writeByte(2);
            getOutStream().createFrame(206);
            getOutStream().writeByte(0);
            getOutStream().writeByte(getPrivateChat());
            getOutStream().writeByte(0);
        }
        getFarming().handleLogin();
        getQuestTab().openTab(QuestTab.Tab.INFORMATION);
        getItems().addSpecialBar(playerEquipment[playerWeapon]);
        spawnedbarrows = false;
        saveCharacter = true;
        Server.playerHandler.updatePlayer(this, outStream);
        Server.playerHandler.updateNPC(this, outStream);
        flushOutStream();
        totalLevel = getPA().calculateTotalLevel();
        getPA().updateQuestTab(); //diary tab
        /**
         * Welcome messages
         */
        getQuestTab().updateInformationTab();
        getPA().sendFrame126("Combat Level: " + combatLevel + "", 3983);
        getPA().sendFrame126("Total level:", 19209);
        getPA().sendFrame126(totalLevel + "", 3984);
        getPA().resetFollow();
        getPA().clearClanChat();
        getPA().resetFollow();
        getPA().setClanData();
        updateRank();
        getBank().onLogin();
        getRunePouch().sendPouchRuneInventory();
        getPA().updatePoisonStatus();
        getQuesting().updateQuestList();
        if (TourneyManager.getSingleton().isInArenaBoundsOnLogin(this)) {
            TourneyManager.getSingleton().handleLoginWithinArena(this);
        }
        if (TourneyManager.getSingleton().isInLobbyBoundsOnLogin(this)) {
            TourneyManager.getSingleton().handleLoginWithinLobby(this);
        }
        if (totalLevel >= 2000) {
            getEventCalendar().progress(EventChallenge.HAVE_2000_TOTAL_LEVEL);
        }
        if (totalLevel >= 2178) {
            Achievements.increase(this, AchievementType.MAX, 1);
        }

        if (!completedTutorial) {
            Server.clanManager.getHelpClan().addMember(this);

            if (Server.isDebug() && !bot) {
                getRights().add(Right.ADMINISTRATOR);
                getRights().add(Right.OWNER);
                getRights().add(Right.ONYX_CLUB);
                getRights().setPrimary(Right.OWNER);
            }

            getRights().remove(Right.IRONMAN);
            getRights().remove(Right.ULTIMATE_IRONMAN);
            getRights().remove(Right.HC_IRONMAN);
            Server.clanManager.getHelpClan().addMember(this);
            start(new TutorialDialogue(this, false));
            mode = Mode.forType(ModeType.STANDARD);
            receivedVoteStreakRefund = true;
            setMigrationVersion(PlayerMigrationRepository.getLatestVersion());
        } else {
            if (mode == null) {
                mode = Mode.forType(ModeType.STANDARD);
            }
            Server.clanManager.joinOnLogin(this);
        }

        if (!receivedVoteStreakRefund) {
            receivedVoteStreakRefund = true;
            VoteUser user = VotePanelManager.getUser(this);
            if (user != null && user.getDayStreak() < 4) {
                user.setDayStreak(4);
                sendMessage("<clan=6> @dre@You've received a 4 Vote Panel streak, thanks for being patient!");
                VotePanelManager.saveToJSON();
            }
        }

        getPA().sendFrame36(172, autoRet);
        addEvents();
        if (Configuration.BOUNTY_HUNTER_ACTIVE) {
            bountyHunter.updateTargetUI();
        }
        for (int i = 0; i < 22; i++) {
            getPA().setSkillLevel(i, playerLevel[i], playerXP[i]);
            getPA().refreshSkill(i);
        }
        health.setMaximumHealth(getPA().getLevelForXP(playerXP[playerHitpoints]));
        BankPin pin = getBankPin();
        if (pin.requiresUnlock()) {
            pin.open(2);
        }
        if (health.getCurrentHealth() < 10) {
            health.setCurrentHealth(10);
        }
        // Update experience counter on login
        int[] ids = new int[playerLevel.length];
        for (int skillId = 0; skillId < ids.length; skillId++) {
            ids[skillId] = skillId;
        }
        if (experienceCounter > 0L) {
            playerAssistant.sendExperienceDrop(false, experienceCounter, ids);
        }

        rechargeItems.onLogin();
        for (int i = 0; i < getQuick().getNormal().length; i++) {
            if (getQuick().getNormal()[i]) {
                getPA().sendConfig(QuickPrayers.CONFIG + i, 1);
            } else {
                getPA().sendConfig(QuickPrayers.CONFIG + i, 0);
            }
        }
        PollTab.updateInterface(this);
        if (EventCalendar.isEventRunning()) {
            sendMessage(EventCalendar.LOGIN_MESSAGE);
        }
        if (Server.getConfiguration().getServerState().getLoginMessages() != null) {
            Arrays.stream(Server.getConfiguration().getServerState().getLoginMessages()).forEach(this::sendMessage);
        }
        getDailyRewards().onLogin();
        PlayerSave.login(this);
        correctCoordinates();
        BossPoints.doRefund(this);
        EventChallengeMonthlyReward.onLogin(this);
        LeaderboardUtils.checkRewards(this);

        if (!getBankPin().hasBankPin() && isCompletedTutorial()) {
            sendMessage("@dre@You don't have an account pin, it's highly recommended you set one with ::pin.");
        }

        CompromisedAccounts.onLogin(this);
        PlayerMigrationRepository.migrate(this);
    }

    public void sendMessage(String s, long delay) {
        String key = "message_delay" + s;
        if (System.currentTimeMillis() - getAttributes().getLong(key, 0) >= delay) {
            getAttributes().setLong(key, System.currentTimeMillis());
            sendMessage(s);
        }
    }

    public void debug(String message, Object...args) {
        debug(Misc.replaceBracketsWithArguments(message, args));
    }

    public void debug(String message) {
        if (debugMessage) {
            sendMessage(message);
        }
    }

    public void sendMessage(String s, Object... args) {
        sendMessage(Misc.replaceBracketsWithArguments(s, args));
    }

    public void sendMessageIf(boolean bool, String s, Object... args) {
        if (bool) {
            sendMessage(s, args);
        }
    }

    public void sendMessage(String s) {
        if (s.length() >= 220) {
            logger.error("String is greater than a 130 characters! ({}), player={} {}", s.length(), this, new Exception());
        }

        if (getOutStream() != null) {
            getOutStream().createFrameVarSize(253);
            getOutStream().writeString(s);
            getOutStream().endFrameVarSize();
        }
    }

    public void sendStatement(String... statement) {
        start(new DialogueBuilder(this).statement(statement));
    }

    /**
     * A cache of the side bar interfaces currently set for the player
     */
    private Map<Integer, Integer> sideBarInterfaces = new HashMap<>();

    public void setSidebarInterface(int menuId, int form) {
        if (getOutStream() != null) {
            int cachedMenuForm = sideBarInterfaces.getOrDefault(menuId, -1);
            // Only send sidebar interface if it changes
            if (cachedMenuForm == form)
                return;

            getOutStream().createFrame(71);
            getOutStream().writeUnsignedWord(form);
            getOutStream().writeByteA(menuId);
            sideBarInterfaces.put(menuId, form);
        }
    }

    public boolean firstMove;

    public void addEvents() {
        Server.getEventHandler().submit(new MinigamePlayersEvent(this));
        Server.getEventHandler().submit(new SkillRestorationEvent(this));
        Server.getEventHandler().submit(new RunEnergyEvent(this, 1));
        CycleEventHandler.getSingleton().addEvent(this, bountyHunter, 1);
    }

    public void update() {
        Server.playerHandler.updatePlayer(this, outStream);
        Server.playerHandler.updateNPC(this, outStream);
        flushOutStream();
    }

    public void healEverything() {
        setRunEnergy(100, true);
        getHealth().removeAllStatuses();
        if (getHealth().getCurrentHealth() < getHealth().getMaximumHealth()) {
            getHealth().reset();
        }
        getPA().refreshSkill(5);
        specAmount = 10.0;
        specRestore = 120;
        getItems().addSpecialBar(playerEquipment[playerWeapon], false);
        playerLevel[5] = getPA().getLevelForXP(playerXP[5]);
        setProtectionPrayersShiftRight(false);
    }

    public void heal(int amount) {
        setRunEnergy(100, true);
        int heal = amount;
        if (heal > getHealth().getMaximumHealth()) {
            getHealth().reset();
        }
        getHealth().increase(amount);
        getPA().refreshSkill(3);

    }

    public void resetOnDeath() {
        PlayerSave.saveGame(this);
        resetDamageTaken();
        totalHunllefDamage = 0;
        attacking.reset();
        getPA().frame1();
        getPA().resetTb();
        isSkulled = false;
        attackedPlayers.clear();
        headIconPk = -1;
        skullTimer = -1;
        getHealth().reset();
        getHealth().removeAllStatuses();
        getHealth().removeAllImmunities();
        getPA().requestUpdates();
        tradeResetNeeded = true;
        MeleeData.setWeaponAnimations(this);
        Arrays.stream(ClientGameTimer.values()).filter(timer -> timer.isResetOnDeath()).forEach(timer -> getPA().sendGameTimer(timer, TimeUnit.SECONDS, 0));
    }

    /**
     * Update {@link #equippedGodItems}, which is a list of all gods of which the
     * player has at least 1 item equipped.
     */
    public void updateGodItems() {
        equippedGodItems = new ArrayList<>();
        for (God god : God.values()) {
            for (Integer itemId : GodwarsEquipment.EQUIPMENT.get(god)) {
                if (getItems().isWearingItem(itemId)) {
                    equippedGodItems.add(god);
                    break;
                }
            }
        }
    }

    public List<God> getEquippedGodItems() {
        return equippedGodItems;
    }

    public int totalRaidsFinished;
    public int[] BLACK_MASKS = {Items.BLACK_MASK, Items.BLACK_MASK_1, Items.BLACK_MASK_2, Items.BLACK_MASK_3, Items.BLACK_MASK_4, Items.BLACK_MASK_5, Items.BLACK_MASK_6, Items.BLACK_MASK_7, Items.BLACK_MASK_8, Items.BLACK_MASK_9, Items.BLACK_MASK_10};
    public int[] SLAYER_HELMETS = {11864, 11865, 19639, 19641, 19643, 19645, 19647, 19649, 21888, 21890, 21264, 21266, 23075, 24444, 24370};
    public int[] IMBUED_SLAYER_HELMETS = {Items.SLAYER_HELMET_I, Items.TWISTED_SLAYER_HELMET_I, Items.TURQUOISE_SLAYER_HELMET_I, Items.RED_SLAYER_HELMET_I, Items.PURPLE_SLAYER_HELMET_I, Items.PURPLE_SLAYER_HELMET_I, Items.BLACK_SLAYER_HELMET_I, Items.GREEN_SLAYER_HELMET_I, Items.HYDRA_SLAYER_HELMET_I};
    public int graceSum;

    public void graceSum() {
        graceSum = 0;
        for (int grace : AgilityHandler.graceful_ids) {
            if (getItems().isWearingItem(grace)) {
                graceSum++;
            }
        }
        if (SkillcapePerks.AGILITY.isWearing(this) || SkillcapePerks.isWearingMaxCape(this)) {
            graceSum++;
        }
    }

    public void checkInstanceCoords() {
        if (getInstance() != null && (getInstance().getBoundaries().stream().noneMatch(boundary -> boundary.in(this)) || getInstance().isDisposed())) {
            logger.debug("Remove player because not in instance boundary or instance was disposed {}", this);
            getInstance().remove(this);
        }
    }

    private void lowerEnergy() {
        graceSum();
        int ticks = 1 + graceSum;
        if (staminaDelay != -1)
            ticks += 7;
        if (runningDistanceTravelled >= ticks) {
            runningDistanceTravelled = 0;
            setRunEnergy(getRunEnergy() - 1, true);
        }
    }

    public void addQueuedAction(Consumer<Player> action) {
        queuedActions.add(action);
    }

    /**
     * Add an action that will happen first in the {@link Player#finishLogin()}} method.
     * If the player has already logged in this will have no effect.
     */
    public void addQueuedLoginAction(Consumer<Player> action) {
        queuedLoginActions.add(action);
    }

    public void processQueuedActions() {
        Consumer<Player> action;
        while ((action = queuedActions.poll()) != null) {
            action.accept(this);
        }
    }

    private void processTickables() {
        List<TickableContainer<Player>> removeList = new ArrayList<>();
        List<TickableContainer<Player>> tickablesCopy = new ArrayList<>(tickables);
        for (TickableContainer<Player> tickable : tickablesCopy) {
            if (tickable.isStopped() || !tickable.tick(this)) {
                removeList.add(tickable);
            }
        }
        tickables.removeAll(removeList);
    }

    public TickableContainer<Player> addTickable(Tickable<Player> tickable) {
        TickableContainer<Player> container = new TickableContainer<>(tickable);
        tickables.add(container);
        return container;
    }

    public TickableContainer<Player> setTickable(Tickable<Player> tickable) {
        if (this.tickable != null) {
            this.tickable.stop();
        }
        if (tickable != null) {
            this.tickable = addTickable(tickable);
            return this.tickable;
        } else {
            this.tickable = null;
            return null;
        }
    }

    public int tournamentFogDuration;
    public int tournamentDamageFromFog;

    public boolean wasInRaids = false;

    public void raidsClipFix() {
        if (Boundary.RAIDS.in(this)) {
            wasInRaids = true;
        } else if ((wasInRaids)) {
            Raids raidInstance = this.getRaidsInstance();
            if (raidInstance != null) {
                sendMessage("@blu@Sending you back to starting room...");
                Location startRoom = raidInstance.getStartLocation();
                getPA().movePlayer(startRoom.getX(), startRoom.getY(), raidInstance.currentHeight);
                raidInstance.resetRoom(this);
                wasInRaids = false;
                PlayerSave.saveGame(this);
            }
        }
    }

    public void interruptActions(boolean stopWalk, boolean closeInterfaces, boolean stopAll) {
        /**
         * Just an idea
         */
        if (stopAll) {
            getPA().stopSkilling();
            getPA().resetVariables();
            resetWalkingQueue();
            getPA().removeAllWindows();
            getPA().closeAllWindows();
            interruptActions();
        }
        if (stopWalk) {
            resetWalkingQueue();
        }
        if (closeInterfaces) {
            getPA().removeAllWindows();
            getPA().closeAllWindows();
        }
    }

    /**
     * Reset things like skilling (tickables, combat, etc) when needed.
     */
    public void interruptActions() {
        for (int i = 0; i < playerSkilling.length; i++) {
            playerSkilling[i] = false;
        }
        setTickable(null);
    }

    public void process() {
        getDonationRewards().tick();
        raidsClipFix();
        processQueuedActions();
        processTickables();
        lowerEnergy();
        getDailyRewards().notifyWhenReady(false);
        if (getCannon() != null) {
            getCannon().tick(this);
        }
        // If player hasn't completed tutorial, no dialogues are open and mode selection interface isn't open, then we open it.
        if (!isCompletedTutorial()
                && (getDialogueBuilder() == null || getDialogueBuilder().getCurrent() == null)
                && !isInterfaceOpen(ModeSelection.INTERFACE_ID)) {
            modeSelection.openInterface();
        }
        if (teleBlockStartMillis > 0 && System.currentTimeMillis() - teleBlockStartMillis >= teleBlockLength) {
            teleBlockLength = 0;
            teleBlockStartMillis = 0;
            sendMessage("The spell blocking your teleport has expired.");
        }
        if (hasOverloadBoost) {
            if (Boundary.isIn(this, Boundary.DUEL_ARENA) || Boundary.isIn(this, Boundary.WILDERNESS)) {
                getPotions().resetOverload();
                getPA().sendGameTimer(ClientGameTimer.OVERLOAD, TimeUnit.MINUTES, 0);
            }
        }
        if ((underAttackByPlayer > 0 || underAttackByNpc > 0)) {
            if (this.serpHelmCombatTicks < 8) this.serpHelmCombatTicks++;
            this.getCombatItems().checkCombatTickBasedItems();
        }
        if (xpScrollTicks > 0) {
            xpScrollTicks--;
            if (xpScrollTicks <= 0) {
                xpScrollTicks = 0;
                xpScroll = false;
                sendMessage("@red@Your xp scroll has run out!");
            }
        }
        if (fasterCluesTicks > 0) {
            fasterCluesTicks--;
            if (fasterCluesTicks <= 0) {
                fasterCluesTicks = 0;
                fasterCluesScroll = false;
                sendMessage("@red@Your faster clue scroll has run out!");
            }
        }
        if (skillingPetRateTicks > 0) {
            skillingPetRateTicks--;
            if (skillingPetRateTicks <= 0) {
                skillingPetRateTicks = 0;
                skillingPetRateScroll = false;
                sendMessage("@red@Your skilling pet rate bonus has ran out!");
            }
        }
        if (getInstance() != null) {
            getInstance().tick(this);
        }
        if (isRunningToggled() && runEnergy <= 0) {
            updateRunningToggled(false);
        }
        if (staminaDelay > 0) {
            staminaDelay--;
        }
        if (gwdAltarTimer > 0) {
            gwdAltarTimer--;
        }
        if (gwdAltarTimer == 1) {
            sendMessage("You can now operate the godwars prayer altar again.");
        }
        if (updateItems) {
            itemAssistant.updateItems();
            updateItems = false;
        }
        if (bonusXpTime > 0) {
            bonusXpTime--;
        }
        if (bonusXpTime == 1) {
            sendMessage("@blu@Your time is up. Your XP is no longer boosted by the voting reward.");
        }
        if (isDead && respawnTimer == -6) {
            PlayerDeath.applyDead(this);
        }
        if (respawnTimer == 9) {
            respawnTimer = -6;
            PlayerDeath.giveLife(this);
        } else if (respawnTimer == 12) {
            // Set killer in combat delay
            if (underAttackByPlayer > 0 && underAttackByPlayer < PlayerHandler.players.length && PlayerHandler.players[underAttackByPlayer] != null) {
                PlayerHandler.players[underAttackByPlayer].setInCombatDelay(Configuration.IN_COMBAT_TIMER);
            }
            // Animation
            respawnTimer--;
            startAnimation(2304);
        }
        if (Boundary.isIn(this, Boundary.ZULRAH) && getZulrahEvent().isInToxicLocation()) {
            appendDamage(null, 1 + Misc.random(3), Hitmark.VENOM);
        }
        if (respawnTimer > -6) {
            respawnTimer--;
        }
        if (hitDelay > 0) {
            hitDelay--;
        }
        getAgilityHandler().agilityProcess(this);
        if (specRestore > 0) {
            specRestore--;
        }
        if (playTime < Integer.MAX_VALUE && !isIdle) {
            playTime++;
        }
        //getPA().sendFrame126("@or1@Players Online: @gre@" + PlayerHandler.getPlayerCount() + "", 10222);
        if (System.currentTimeMillis() - specDelay > Configuration.INCREASE_SPECIAL_AMOUNT) {
            specDelay = System.currentTimeMillis();
            if (specAmount < 10) {
                specAmount += 1;
                if (specAmount > 10) specAmount = 10;
                getItems().updateSpecialBar();
                getItems().addSpecialBar(playerEquipment[playerWeapon]);
            }
        }
        CombatPrayer.handlePrayerDrain(this);
        if (underAttackByPlayer > 0) {
            if (System.currentTimeMillis() - singleCombatDelay > inCombatDelay) {
                underAttackByPlayer = 0;
                setInCombatDelay(Configuration.IN_COMBAT_TIMER);
            }
        }
        if (underAttackByNpc > 0) {
            if (System.currentTimeMillis() - singleCombatDelay2 > inCombatDelay) {
                underAttackByNpc = 0;
                setInCombatDelay(Configuration.IN_COMBAT_TIMER);
            }
        }
        if (hasOverloadBoost) {
            if (System.currentTimeMillis() - lastOverloadBoost > 15000) {
                getPotions().doOverloadBoost();
                lastOverloadBoost = System.currentTimeMillis();
            }
        }
        sendAreaInterfaces();
        if (Boundary.isIn(this, Boundary.ICE_PATH)) {
            getPA().sendFrame99(2);
            if (getRunEnergy() > 0) setRunEnergy(0, true);
            if (heightLevel > 0) getPA().icePath();
        }
        if (!getPosition().inWild()) {
            wildLevel = 0;
        }
        if (Boundary.isIn(this, Boundary.EDGEVILLE_PERIMETER) && !Boundary.isIn(this, Boundary.EDGE_BANK) && getHeight() == 8) {
            wildLevel = 126;
        }
        if (!hasMultiSign && getPosition().inMulti()) {
            hasMultiSign = true;
            getPA().multiWay(1);
        }
        if (hasMultiSign && !getPosition().inMulti()) {
            hasMultiSign = false;
            getPA().multiWay(-1);
        }
        if (!getPosition().inMulti() && getPosition().inWild()) getPA().sendFrame70(30, 0, 196);
        else if (getPosition().inMulti() && getPosition().inWild()) getPA().sendFrame70(0, 0, 196);
        if (this.skullTimer > 0) {
            --skullTimer;
            if (skullTimer == 1) {
                isSkulled = false;
                attackedPlayers.clear();
                headIconPk = -1;
                skullTimer = -1;
                getPA().requestUpdates();
            }
        }
        if (freezeTimer > -6) {
            freezeTimer--;
            if (frozenBy != null) {
                Entity frozenByReference = frozenBy.get();
                if (frozenByReference == null) {
                    freezeTimer = -1;
                    frozenBy = null;
                } else if (distance(frozenByReference.getPosition()) > 13) {
                    freezeTimer = -1;
                    frozenBy = null;
                }
            }
        }
        if (teleTimer > 0) {
            teleTimer--;
            if (!isDead) {
                if (teleTimer == 1) {
                    teleTimer = 0;
                }
                if (teleTimer == 5) {
                    if (isDead) {
                        return;
                    }
                    setTeleportToX(teleX);
                    setTeleportToY(teleY);
                    heightLevel = teleHeight;
                    if (teleEndAnimation > 0) {
                        startAnimation(teleEndAnimation);
                        teleTimer = 2;
                    } else {
                        teleTimer = 0;
                    }
                }
                if (teleTimer == teleGfxTime && teleGfx > 0) {
                    teleTimer--;
                    gfx100(teleGfx);
                    if (teleSound != 0) Server.playerHandler.sendSound(teleSound, this);
                }
            } else {
                teleTimer = 0;
            }
        }
        if (attackTimer > 0) {
            attackTimer--;
        }
        if (targeted != null) {
            if (distanceToPoint(targeted.getX(), targeted.getY()) > 10) {
                getPA().sendEntityTarget(0, targeted);
                targeted = null;
            }
        }
        getItems().processContainerUpdates();
    }

    public void processCombat() {
        if (npcAttackingIndex > 0 && clickNpcType == 0 || playerAttackingIndex > 0) {
            // Attempt to execute a granite maul special if queued
            if (getCombatItems().doQueuedGraniteMaulSpecials())
                return;
        }
        if (npcAttackingIndex > 0 && clickNpcType == 0) {
            attacking.attackEntity(NPCHandler.npcs[npcAttackingIndex]);
        }
        if (playerAttackingIndex > 0) {
            attacking.attackEntity(PlayerHandler.players[playerAttackingIndex]);
        }
    }

    private void sendAreaInterfaces() {
        if (!getController().isDefault()) // Only do this on default, otherwise use controller enter and exit to set these
            return;

        // Player options in this if-else
        if (Boundary.isIn(this, FlowerPoker.BOUNDARIES)) {
            getPA().showOption(1, 0, "<img=29>Gamble with");
        } else if (getPosition().inDuelArena() || Boundary.isIn(this, Boundary.DUEL_ARENA)) {
            if (Boundary.isIn(this, Boundary.DUEL_ARENA)) {
                getPA().showOption(3, 0, "Attack");
                getPA().showOption(1, 0, "null");
            } else {
                getPA().showOption(1, 0, "Challenge");
                getPA().showOption(3, 0, "null");
            }
        } else if (getPosition().inWild() || getPosition().inClanWars() && getPosition().inWild() || inPits) {
            getPA().showOption(3, 0, "Attack");
        } else {
            getPA().showOption(3, 0, "null");
            getPA().showOption(1, 0, "null");
        }

        // Walkable interfaces in this if-else
        if (getPosition().inWild() && !getPosition().inClanWars()) {
            int modY = absY > 6400 ? absY - 6400 : absY;
            wildLevel = (((modY - 3520) / 8) + 1);
            if (Configuration.SINGLE_AND_MULTI_ZONES) {
                //System.out.println("ATTEMPTING TO SEND LEVEL: " + wildLevel);
                getPA().sendFrame126("@yel@Level: " + wildLevel, 199);
            } else {
                getPA().multiWay(-1);
                getPA().sendFrame126("@yel@Level: " + wildLevel, 199);
            }
            if (Configuration.BOUNTY_HUNTER_ACTIVE && !getPosition().inClanWars()) {
                getPA().walkableInterface(28000);
                getPA().sendInterfaceHidden(1, 28070);
                getPA().sendInterfaceHidden(0, 196);
            } else {
                getPA().walkableInterface(197);
            }
            if (Boundary.isIn(this, Boundary.DEEP_WILDY_CAVES)) {
                getPA().sendFrame126("", 199);
                getPA().sendFrame126("@yel@Level: " + wildLevel, 250);
            } else {
                getPA().sendFrame126("", 250);
            }
        } else if (getPosition().inClanWars() && getPosition().inWild()) {
            getPA().walkableInterface(197);
            getPA().sendFrame126("@yel@3-126", 199);
            wildLevel = 126;
        } else if (Boundary.isIn(this, Boundary.SCORPIA_LAIR)) {
            getPA().sendFrame126("@yel@Level: 54", 199);
            // getPA().walkableInterface(197);
            wildLevel = 54;
        } else if (getItems().isWearingItem(10501, 3) && !getPosition().inWild()) {
            getPA().showOption(3, 0, "Throw-At");
        } else if (getPosition().inEdgeville()) {
            if (Configuration.BOUNTY_HUNTER_ACTIVE) {
                if (bountyHunter.hasTarget()) {
                    getPA().walkableInterface(28000);
                    getPA().sendInterfaceHidden(0, 28070);
                    getPA().sendInterfaceHidden(1, 196);
                    bountyHunter.updateOutsideTimerUI();
                } else {
                    getPA().walkableInterface(-1);
                }
            } else {
                getPA().sendFrame99(0);
                getPA().walkableInterface(-1);
                getPA().showOption(3, 0, "Null");
            }
            getPA().showOption(3, 0, "null");
        } else if (Boundary.isIn(this, PestControl.LOBBY_BOUNDARY)) {
            getPA().walkableInterface(21119);
            PestControl.drawInterface(this, "lobby");
        } else if (Boundary.isIn(this, PestControl.GAME_BOUNDARY)) {
            getPA().walkableInterface(21100);
            PestControl.drawInterface(this, "game");
        } else if ((getPosition().inDuelArena() || Boundary.isIn(this, Boundary.DUEL_ARENA))) {
            getPA().walkableInterface(201);
            wildLevel = 126;
        } else if (getPosition().inGodwars()) {
            godwars.drawInterface();
            getPA().walkableInterface(16210);
        } else if (Boundary.isIn(this, Boundary.SKOTIZO_BOSSROOM)) {
            getPA().walkableInterface(29230);
        } else if (getPosition().inRaidLobby()) {
            getPA().walkableInterface(6673);
        } else if (getInstance() == null || !getInstance().handleInterfaceUpdating(this)) {
            getPA().walkableInterface(-1);
        }
    }

    public Stream getInStream() {
        return inStream;
    }

    public Stream getOutStream() {
        return outStream;
    }

    public ItemAssistant getItems() {
        return itemAssistant;
    }

    public PlayerAssistant getPA() {
        return playerAssistant;
    }

    public CollectionLog getCollectionLog() {
        return collectionLog;
    }

    public CollectionLog getGroupIronmanCollectionLog() {
        if (getRights().contains(Right.GROUP_IRONMAN)) {
            GroupIronmanGroup group = GroupIronmanRepository.getGroupForOnline(this).orElse(null);
            if (group != null && group.getCollectionLog() != null) {
                return group.getCollectionLog();
            }
        }

        return null;
    }

    public CollectionLog getViewingCollectionLog() {
        return viewingCollectionLog;
    }

    public void setViewingCollectionLog(CollectionLog viewingCollectionLog) {
        this.viewingCollectionLog = viewingCollectionLog;
    }

    public DialogueHandler getDH() {
        return dialogueHandler;
    }

    public ChargeTrident getCT() {
        return chargeTrident;
    }

    public ShopAssistant getShops() {
        return shopAssistant;
    }

    public CombatItems getCombatItems() {
        return combatItems;
    }

    public ActionHandler getActions() {
        return actionHandler;
    }

    public Channel getSession() {
        return session;
    }

    public Potions getPotions() {
        return potions;
    }

    public Food getFood() {
        return food;
    }

    public PlayerAssistant getPlayerAssistant() {
        return playerAssistant;
    }

    public SkillInterfaces getSI() {
        return skillInterfaces;
    }

    public int getRuneEssencePouch(int index) {
        return runeEssencePouch[index];
    }

    public void setRuneEssencePouch(int index, int runeEssencePouch) {
        this.runeEssencePouch[index] = runeEssencePouch;
    }

    public int getPureEssencePouch(int index) {
        return pureEssencePouch[index];
    }

    public void setPureEssencePouch(int index, int pureEssencePouch) {
        this.pureEssencePouch[index] = pureEssencePouch;
    }

    public Slayer getSlayer() {
        if (slayer == null) {
            slayer = new Slayer(this);
        }
        return slayer;
    }

    public Agility getAgility() {
        return agility;
    }

    public Thieving getThieving() {
        return thieving;
    }

    public Herblore getHerblore() {
        return herblore;
    }

    public Godwars getGodwars() {
        return godwars;
    }

    public TreasureTrails getTrails() {
        return trails;
    }

    public GnomeAgility getGnomeAgility() {
        return gnomeAgility;
    }

    public PointItems getPoints() {
        return pointItems;
    }

    public void setMovementState(PlayerMovementState movementState) {
        this.movementState = movementState;
    }

    public PlayerMovementState getMovementState() {
        return movementState == null ? PlayerMovementState.getDefault() : movementState;
    }

    public WildernessAgility getWildernessAgility() {
        return wildernessAgility;
    }

    public Shortcuts getAgilityShortcuts() {
        return shortcuts;
    }

    public RooftopPollnivneach getRooftopPollnivneach() {
        return this.rooftopPollnivneach;
    }

    public RooftopCanafis getRooftopCanafis() {
        return this.rooftopCanafis;
    }

    public RooftopAlkharid getRooftopAlkharid() {
        return this.rooftopAlkharid;
    }

    public RooftopFalador getRooftopFalador() {
        return this.rooftopFalador;
    }

    public RooftopDraynor getRoofTopDraynor() {
        return this.rooftopDraynor;
    }

    public RooftopRellekka getRooftopRellekka() {
        return this.rooftopRellekka;
    }

    public Lighthouse getLighthouse() {
        return lighthouse;
    }

    public BarbarianAgility getBarbarianAgility() {
        return barbarianAgility;
    }

    public AgilityHandler getAgilityHandler() {
        return agilityHandler;
    }

    public Smithing getSmithing() {
        return smith;
    }

    public FightCave getFightCave() {
        if (fightcave == null) fightcave = new FightCave(this);
        return fightcave;
    }

    public Skotizo getSkotizo() {
        if (getInstance() != null && getInstance() instanceof Skotizo) {
            return (Skotizo) getInstance();
        }

        return null;
    }

    public SmithingInterface getSmithingInt() {
        return smithInt;
    }

    public int getPrestigePoints() {
        return prestigePoints;
    }

    /*
     * public Fletching getFletching() { return fletching; }
     */
    public Prayer getPrayer() {
        return prayer;
    }

    public void queueMessage(Packet packet, boolean priority) {
        attemptedPackets.incrementAndGet();
        packetsReceived.incrementAndGet();
        if (priority)
            priorityPackets.add(packet);
        else
            queuedPackets.add(packet);
    }

    public void processQueuedPackets(boolean priority) {
        processQueuedPackets(priority ? priorityPackets : queuedPackets);
    }

    private void processQueuedPackets(Queue<Packet> queue) {
        Packet p;
        attemptedPackets.set(0);
        packetsReceived.set(0);
        while ((p = queue.poll()) != null) {
            lastPacketReceived = System.currentTimeMillis();
            inStream.currentOffset = 0;
            inStream.buffer = p.getPayload().array();

            if (p.getOpcode() > 0) {
                PacketHandler.processPacket(this, p.getOpcode(), p.getLength());
            }
        }
    }

    public void correctCoordinates() {
        if (Boundary.isIn(new Position(teleportToX, teleportToY, heightLevel), Boundary.OBOR_AREA)) {
            setTeleportToX(3095);
            setTeleportToY(9833);
            heightLevel = 0;
        }
        if (Boundary.isIn(new Position(teleportToX, teleportToY, heightLevel), Boundary.BRYOPHYTA_ROOM)) {
            setTeleportToX(3174);
            setTeleportToY(9900);
            heightLevel = 0;
        }
        if (Boundary.isIn(new Position(teleportToX, teleportToY, heightLevel), Boundary.KRAKEN_BOSS_ROOM)) {
            setTeleportToX(2280);
            setTeleportToY(10016);
            heightLevel = 0;
        }
        if (Boundary.isIn(new Position(teleportToX, teleportToY, heightLevel), Boundary.GROTESQUE_LAIR)) {
            setTeleportToX(3428);
            setTeleportToY(3541);
            heightLevel = 2;
        }
        if (Boundary.isIn(new Position(teleportToX, teleportToY, heightLevel), Boundary.PEST_CONTROL_AREA)) {
            setTeleportToX(2657);
            setTeleportToY(2639);
            heightLevel = 0;
        }
        if (Boundary.isIn(this, Boundary.XERIC) || Boundary.isIn(this, Boundary.XERIC_LOBBY)) {
            setTeleportToX(3033);
            setTeleportToY(6068);
            setHeight(0);
        }
        if (Boundary.isIn(new Position(teleportToX, teleportToY, heightLevel), Boundary.FIGHT_CAVE)) {
            heightLevel = getIndex() * 4;
            sendMessage("Wave " + (this.waveId + 1) + " will start in approximately 5-10 seconds. ");
            getFightCave().spawn();
        }
        if (Boundary.isIn(new Position(teleportToX, teleportToY, heightLevel), Boundary.INFERNO)) {
            Inferno.moveToExit(this);
        }
        if (Boundary.isIn(new Position(teleportToX, teleportToY, heightLevel), Boundary.ZULRAH)) {
            moveToHome();
        }
        if (Boundary.isIn(new Position(teleportToX, teleportToY, heightLevel), Boundary.HESPORI)) {
            moveToHome();
            Hespori.deleteEventItems(this);
        }
        if (Boundary.isIn(new Position(teleportToX, teleportToY, heightLevel), NightmareConstants.BOUNDARY)) {
            moveTo(NightmareConstants.NIGHTMARE_PLAYER_EXIT_POSITION);
        }
        if (Boundary.isIn(new Position(teleportToX, teleportToY, heightLevel), NightmareConstants.LOBBY_BOUNDARY)) {
            moveToHome();
            heightLevel = 0;
        }
        if (Boundary.isIn(new Position(teleportToX, teleportToY, heightLevel), Boundary.RAIDROOMS)) {
            moveToHome();
            heightLevel = 0;
        }
        if (Boundary.isIn(new Position(teleportToX, teleportToY, heightLevel), Boundary.CRYSTAL_CAVE_STAIRS)) {
            moveToHome();
            heightLevel = 0;
        }
        if (Boundary.isIn(new Position(teleportToX, teleportToY, heightLevel), Boundary.CRYSTAL_CAVE_ENTRANCE)) {
            moveToHome();
            heightLevel = 0;
        }
//        if (Boundary.isIn(new Position(teleportToX, teleportToY, heightLevel), Boundary.GRAND_EXCHANGE)) {
//            moveToHome();
//            heightLevel = 0;
//        }
        if (Boundary.isIn(new Position(teleportToX, teleportToY, heightLevel), Boundary.VORKATH)) {
            moveTo(new Position(2272, 4051, 0));
        }
        if (Arrays.stream(Boundary.CERBERUS_BOSSROOMS).anyMatch(cerb -> {
            return Boundary.isIn(new Position(teleportToX, teleportToY, heightLevel), cerb);
        })) {
            moveTo(Cerberus.EXIT);
        }
        if (Boundary.isIn(new Position(teleportToX, teleportToY, heightLevel), Boundary.DAGANNOTH_MOTHER_HFTD)) {
            moveTo(new Position(2515, 4629, 0));
        }
        for (Boundary boundary : TobConstants.ALL_BOUNDARIES) {
            if (Boundary.isIn(new Position(teleportToX, teleportToY, heightLevel), boundary)) {
                moveTo(TobConstants.FINISHED_TOB_POSITION);
            }
        }
    }

    private void moveToHome() {
        setTeleportToX(Configuration.START_LOCATION_X);
        setTeleportToY(Configuration.START_LOCATION_Y);
        this.heightLevel = 0;
    }

    public void updateRank() {
        if (amDonated <= 0) {
            amDonated = 0;
        }

        if (amDonated >= 50 && amDonated < 100) {
            if (getRights().isOrInherits(Right.YOUTUBER) || getRights().isOrInherits(Right.IRONMAN)
                    || getRights().isOrInherits(Right.ULTIMATE_IRONMAN)
                    || getRights().isOrInherits(Right.OSRS)
                    || getRights().isOrInherits(Right.HELPER)
                    || getRights().isOrInherits(Right.MODERATOR)
                    || getRights().isOrInherits(Right.HC_IRONMAN)) {
                getRights().add(Right.REGULAR_DONATOR);
            } else
            //sendMessage("Your hidden donator rank is now active.");
            {
                getRights().setPrimary(Right.REGULAR_DONATOR);
                //sendMessage("Please relog to receive your donator rank.");
            }
        }
        if (amDonated >= 100 && amDonated < 250) {
            if (getRights().isOrInherits(Right.YOUTUBER) || getRights().isOrInherits(Right.IRONMAN) || getRights().isOrInherits(Right.ULTIMATE_IRONMAN) || getRights().isOrInherits(Right.OSRS) || getRights().isOrInherits(Right.HELPER) || getRights().isOrInherits(Right.MODERATOR) || getRights().isOrInherits(Right.HC_IRONMAN)) {
                getRights().add(Right.EXTREME_DONOR);
            } else
            //sendMessage("Your hidden extreme donator rank is now active.");
            {
                getRights().setPrimary(Right.EXTREME_DONOR);
                //sendMessage("Please relog to receive your extreme donator rank.");
            }
        }
        if (amDonated >= 250 && amDonated < 500) {
            if (getRights().isOrInherits(Right.YOUTUBER) || getRights().isOrInherits(Right.IRONMAN) || getRights().isOrInherits(Right.ULTIMATE_IRONMAN) || getRights().isOrInherits(Right.OSRS) || getRights().isOrInherits(Right.HELPER) || getRights().isOrInherits(Right.MODERATOR) || getRights().isOrInherits(Right.HC_IRONMAN)) {
                getRights().add(Right.LEGENDARY_DONATOR);
            } else
            //sendMessage("Your hidden legendary donator rank is now active.");
            {
                getRights().setPrimary(Right.LEGENDARY_DONATOR);
                //sendMessage("Please relog to receive your legendary donator rank.");
            }
        }
        if (amDonated >= 500 && amDonated < 1000) {
            if (getRights().isOrInherits(Right.YOUTUBER) || getRights().isOrInherits(Right.IRONMAN) || getRights().isOrInherits(Right.ULTIMATE_IRONMAN) || getRights().isOrInherits(Right.OSRS) || getRights().isOrInherits(Right.HELPER) || getRights().isOrInherits(Right.MODERATOR) || getRights().isOrInherits(Right.HC_IRONMAN)) {
                getRights().add(Right.DIAMOND_CLUB);
            } else
            //sendMessage("Your hidden diamond club rank is now active.");
            {
                getRights().setPrimary(Right.DIAMOND_CLUB);
                //sendMessage("Please relog to receive your diamond club rank.");
            }
        }
        if (amDonated >= 1000) {
            if (getRights().isOrInherits(Right.YOUTUBER) || getRights().isOrInherits(Right.IRONMAN) || getRights().isOrInherits(Right.ULTIMATE_IRONMAN) || getRights().isOrInherits(Right.OSRS) || getRights().isOrInherits(Right.HELPER) || getRights().isOrInherits(Right.MODERATOR) || getRights().isOrInherits(Right.HC_IRONMAN)) {
                getRights().add(Right.ONYX_CLUB);
            } else
            //sendMessage("Your hidden onyx club donator rank is now active.");
            {
                getRights().setPrimary(Right.ONYX_CLUB);
                //sendMessage("Please relog to receive your Onyx Club donator rank.");
            }
        }
        //sendMessage("Your updated total amount donated is now $" + amDonated + ".");
    }

    public int getPrivateChat() {
        return privateChat;
    }

    public void setPrivateChat(int option) {
        this.privateChat = option;
    }

    public Trade getTrade() {
        return trade;
    }

    public FlowerPokerHand flowerPokerHand;

    public FlowerPoker getFlowerPokerRequest() {
        return flowerPoker;
    }

    public FlowerPokerHand getFlowerPoker() {
        if (flowerPokerHand == null)
            this.flowerPokerHand = new FlowerPokerHand(this);
        return flowerPokerHand;
    }

    public boolean isFping() {
        return flowerPokerHand != null && flowerPokerHand.other != null;
    }

    public AchievementHandler getAchievements() {
        if (achievementHandler == null) achievementHandler = new AchievementHandler(this);
        return achievementHandler;
    }

    public long getLastContainerSearch() {
        return lastContainerSearch;
    }

    public void setLastContainerSearch(long lastContainerSearch) {
        this.lastContainerSearch = lastContainerSearch;
    }

    public CoinBagSmall getCoinBagSmall() {
        return coinBagSmall;
    }

    public CoinBagMedium getCoinBagMedium() {
        return coinBagMedium;
    }

    public CoinBagLarge getCoinBagLarge() {
        return coinBagLarge;
    }

    public CoinBagBuldging getCoinBagBuldging() {
        return coinBagBuldging;
    }

    public SuperMysteryBox getSuperMysteryBox() {
        return superMysteryBox;
    }

    public FoeMysteryBox getFoeMysteryBox() {
        return foeMysteryBox;
    }

    public SlayerMysteryBox getSlayerMysteryBox() {
        return slayerMysteryBox;
    }

    public VoteMysteryBox getVoteMysteryBox() {
        return voteMysteryBox;
    }

    public PvmCasket getPvmCasket() {
        return pvmCasket;
    }

    public DailyGearBox getDailyGearBox() {
        return dailyGearBox;
    }

    public DailySkillBox getDailySkillBox() {
        return dailySkillBox;
    }

    public EntityDamageQueue getDamageQueue() {
        return entityDamageQueue;
    }

    public long[] reduceSpellDelay = new long[6];
    public int reduceSpellId;
    public static boolean[] canUseReducingSpell = {true, true, true, true, true, true};
    public boolean usingPrayer;
    public boolean isSelectingQuickprayers;
    public boolean[] prayerActive = {false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false};
    private boolean protectionPrayersShiftRight;

    /**
     * Retrieves the bounty hunter instance for this client object. We use lazy
     * initialization because we store values from the player save file in the
     * bountyHunter object upon login. Without lazy initialization the value would
     * be overwritten.
     *
     * @return the bounty hunter object
     */
    public BountyHunter getBH() {
        if (Objects.isNull(bountyHunter)) {
            bountyHunter = new BountyHunter(this);
        }
        return bountyHunter;
    }

    public UnnecessaryPacketDropper getPacketDropper() {
        return packetDropper;
    }

    public Optional<ItemCombination> getCurrentCombination() {
        return currentCombination;
    }

    public void setCurrentCombination(Optional<ItemCombination> combination) {
        this.currentCombination = combination;
    }

    public String getMacAddress() {
        return macAddress;
    }

    public void setMacAddress(String macAddress) {
        this.macAddress = macAddress;
    }

    public String getIpAddress() {
        return connectedFrom;
    }

    public void setIpAddress(String ipAddress) {
        this.connectedFrom = ipAddress;
    }

    public Duel getDuel() {
        return duelSession;
    }

    public void setItemOnPlayer(Player player) {
        this.itemOnPlayer = player;
    }

    public Player getItemOnPlayer() {
        return itemOnPlayer;
    }

    public Killstreak getKillstreak() {
        if (killstreaks == null) {
            killstreaks = new Killstreak(this);
        }
        return killstreaks;
    }

    /**
     * Returns the single instance of the {@link NPCDeathTracker} class for this
     * player.
     *
     * @return the tracker clas
     */
    public NPCDeathTracker getNpcDeathTracker() {
        return npcDeathTracker;
    }

    /**
     * The zulrah event
     *
     * @return event
     */
    public Zulrah getZulrahEvent() {
        return zulrah;
    }

    /**
     * The single {@link WarriorsGuild} instance for this player
     *
     * @return warriors guild
     */
    public WarriorsGuild getWarriorsGuild() {
        return warriorsGuild;
    }

    /**
     * The single instance of the {@link PestControlRewards} class for this player
     *
     * @return the reward class
     */
    public PestControlRewards getPestControlRewards() {
        return pestControlRewards;
    }

    public Mining getMining() {
        return mining;
    }

    public SpellBook getSpellBook() {
        switch (playerMagicBook) {
            case 0:
                return SpellBook.MODERN;
            case 1:
                return SpellBook.ANCIENT;
            case 2:
                return SpellBook.LUNAR;
            default:
                throw new IllegalArgumentException("Book out of bounds: " + playerMagicBook);
        }
    }

    public void setSpellBook(SpellBook spellBook) {
        switch (spellBook) {
            case MODERN:
                setSidebarInterface(6, 938);
                playerMagicBook = 0;
                sendMessage("You feel a drain on your memory.");
                getPA().resetAutocast();
                break;
            case ANCIENT:
                playerMagicBook = 1;
                setSidebarInterface(6, 838);
                sendMessage("An ancient wisdomin fills your mind.");
                getPA().resetAutocast();
                break;
            case LUNAR:
                sendMessage("You switch to the lunar spellbook.");
                setSidebarInterface(6, 29999);
                playerMagicBook = 2;
                getPA().resetAutocast();
                break;
        }
        getPA().resetAutocast();
    }

    public boolean isAutoButton(int button) {
        for (int j = 0; j < CombatSpellData.AUTOCAST_IDS.length; j += 2) {
            if (CombatSpellData.AUTOCAST_IDS[j] == button) return true;
        }
        return false;
    }

    public void assignAutocast(int button) {
        for (int j = 0; j < CombatSpellData.AUTOCAST_IDS.length; j++) {
            if (CombatSpellData.AUTOCAST_IDS[j] == button) {
                Player c = PlayerHandler.players[this.getIndex()];
                autocasting = true;
                autocastId = CombatSpellData.AUTOCAST_IDS[j + 1];
                if (c.autocastingDefensive) {
                    c.getPA().sendFrame36(109, 1);
                    c.getPA().sendFrame36(108, 0);
                } else {
                    c.getPA().sendFrame36(108, 1);
                    c.getPA().sendFrame36(109, 0);
                }
                c.setSidebarInterface(0, 328);
                break;
            }
        }
    }

    public int getLocalX() {
        return getX() - 8 * getMapRegionX();
    }

    public int getLocalY() {
        return getY() - 8 * getMapRegionY();
    }

    public boolean fullVoidRange() {
        if (getItems().isWearingItem(11664) && getItems().isWearingItem(8840) && getItems().isWearingItem(8839) && getItems().isWearingItem(8842)) {
            return true;
        }
        return getItems().isWearingItem(11664) && getItems().isWearingItem(13073) && getItems().isWearingItem(13072) && getItems().isWearingItem(8842);
    }

    public boolean fullEliteVoidRange() {
        if (getItems().isWearingItem(26475) && getItems().isWearingItem(26471) && getItems().isWearingItem(26469) && getItems().isWearingItem(26467)) {
            return true;
        }
        return getItems().isWearingItem(11664) && getItems().isWearingItem(13073) && getItems().isWearingItem(13072) && getItems().isWearingItem(8842);
    }

    public boolean fullEliteVoidMage() {
        if (getItems().isWearingItem(26473) && getItems().isWearingItem(26471) && getItems().isWearingItem(26469) && getItems().isWearingItem(26467)) {
            return true;
        }
        return getItems().isWearingItem(11663) && getItems().isWearingItem(13073) && getItems().isWearingItem(13072) && getItems().isWearingItem(8842);
    }

    public boolean fullVoidMage() {
        // return playerEquipment[playerHat] == 11663 && playerEquipment[playerLegs] ==
        // 8840 || playerEquipment[playerLegs] == 13073 && playerEquipment[playerChest]
        // == 8839
        // || playerEquipment[playerChest] == 13072 && playerEquipment[playerHands] ==
        // 8842;
        if (getItems().isWearingItem(11663) && getItems().isWearingItem(8840) && getItems().isWearingItem(8839) && getItems().isWearingItem(8842)) {
            return true;
        }
        return getItems().isWearingItem(11663) && getItems().isWearingItem(13073) && getItems().isWearingItem(13072) && getItems().isWearingItem(8842);
    }

    public boolean fullVoidMelee() {
        if (getItems().isWearingItem(11665) && getItems().isWearingItem(8840) && getItems().isWearingItem(8839) && getItems().isWearingItem(8842)) {
            return true;
        }
        return getItems().isWearingItem(11665) && getItems().isWearingItem(13073) && getItems().isWearingItem(13072) && getItems().isWearingItem(8842);
    }

    public boolean maxRequirements(Player c) {
        int amount = 0;
        for (int i = 0; i <= 21; i++) {
            if (getLevelForXP(c.playerXP[i]) >= 99) {
                amount++;
            }
            if (amount == 22) {
                return true;
            }
        }
        return false;
    }

    public boolean maxedCertain(Player c, int min, int max) {
        int amount = 0;
        int total = min + max;
        for (int i = min; i <= max; i++) {
            if (getLevelForXP(c.playerXP[i]) >= 99) {
                amount++;
            }
            if (amount == total) {
                return true;
            }
        }
        return false;
    }

    public boolean maxedSkiller(Player c) {
        int amount = 0;
        for (int id = 0; id <= 7; id++) {
            if (getLevelForXP(c.playerXP[id]) >= 2 && id != 3) {
                amount++;
            }
        }
        for (int i = 8; i <= 22; i++) {
            if (c.playerLevel[i] >= 99) {
                amount++;
            }
        }
        return amount == 15;
    }

    public void updateshop(int i) {
        Player p = PlayerHandler.players[getIndex()];
        p.getShops().resetShop(i);
    }

    public void println_debug(String str) {
        System.err.println("[player-" + getIndex() + "][User: " + getLoginName() + "]: " + str);
    }

    public boolean withinDistance(Player otherPlr) {
        if (heightLevel != otherPlr.heightLevel) return false;
        int deltaX = otherPlr.absX - absX;
        int deltaY = otherPlr.absY - absY;
        return deltaX <= 15 && deltaX >= -16 && deltaY <= 15 && deltaY >= -16;
    }

    public boolean withinDistance(NPC npc) {
        if (heightLevel != npc.heightLevel) return false;
        if (npc.needRespawn) return false;
        int deltaX = npc.absX - absX;
        int deltaY = npc.absY - absY;
        return deltaX <= 15 && deltaX >= -16 && deltaY <= 15 && deltaY >= -16;
    }

    public int getHeightLevel() {
        return heightLevel;
    }

    public int distanceToPoint(int pointX, int pointY) {
        return (int) Math.sqrt(Math.pow(absX - pointX, 2) + Math.pow(absY - pointY, 2));
    }

    @Override
    public void resetWalkingQueue() {
        wQueueReadPtr = wQueueWritePtr = 0;
        for (int i = 0; i < walkingQueueSize; i++) {
            walkingQueueX[i] = currentX;
            walkingQueueY[i] = currentY;
        }
    }

    public void addToWalkingQueue(int x, int y) {
        int next = (wQueueWritePtr + 1) % walkingQueueSize;
        if (next == wQueueWritePtr) return;
        walkingQueueX[wQueueWritePtr] = x;
        walkingQueueY[wQueueWritePtr] = y;
        wQueueWritePtr = next;
    }

    public boolean goodDistance(int objectX, int objectY, int playerX, int playerY, int distance) {
        return Misc.goodDistance(objectX, objectY, playerX, playerY, distance);
    }

    public int otherDirection;
    public boolean invincible;

    public boolean isWalkingQueueEmpty() {
        return wQueueReadPtr == wQueueWritePtr || Misc.direction(currentX, currentY, walkingQueueX[wQueueReadPtr], walkingQueueY[wQueueReadPtr]) == -1;
    }

    public int getNextWalkingDirection() {
        if (wQueueReadPtr == wQueueWritePtr) return -1;
        int dir;
        do {
            dir = Misc.direction(currentX, currentY, walkingQueueX[wQueueReadPtr], walkingQueueY[wQueueReadPtr]);
            if (dir == -1 && otherDirection != dir) {
                otherDirection = dir;
            }
            if (dir == -1) {
                wQueueReadPtr = (wQueueReadPtr + 1) % walkingQueueSize;
            } else if ((dir & 1) != 0) {
                println_debug("Invalid waypoint in walking queue!");
                resetWalkingQueue();
                return -1;
            }
        } while ((dir == -1) && (wQueueReadPtr != wQueueWritePtr));
        if (dir == -1) {
            return -1;
        }
        dir >>= 1;
        lastX = absX;
        lastY = absY;
        currentX += Misc.directionDeltaX[dir];
        currentY += Misc.directionDeltaY[dir];

        this.getRegionProvider().removeNpcClipping(RegionProvider.NPC_TILE_FLAG, absX, absY, this.getHeight());
        absX += Misc.directionDeltaX[dir];
        absY += Misc.directionDeltaY[dir];

        if (this.getRegionProvider().isOccupiedByNpc(absX, absY, this.getHeight())) {
            this.getRegionProvider().removeNpcClipping(RegionProvider.NPC_TILE_FLAG, absX, absY, this.getHeight());
        } else {
            this.getRegionProvider().addNpcClipping(RegionProvider.NPC_TILE_FLAG, absX, absY, this.getHeight());
        }

        updateController();
        return dir;
    }

    public boolean isRunning() {
        return isNewWalkCmdIsRunning() || dir2 > -1;
    }

    public void stopMovement() {
        resetWalkingQueue();
    }

    public void preProcessing() {
        newWalkCmdSteps = 0;
    }

    public void postProcessing() {
        if (newWalkCmdSteps > 0) {
            int firstX = getNewWalkCmdX()[0];
            int firstY = getNewWalkCmdY()[0];
            int lastDir = 0;
            boolean found = false;
            numTravelBackSteps = 0;
            int ptr = wQueueReadPtr;
            int dir = Misc.direction(currentX, currentY, firstX, firstY);
            if (dir != -1 && (dir & 1) != 0) {
                do {
                    lastDir = dir;
                    if (--ptr < 0) ptr = walkingQueueSize - 1;
                    travelBackX[numTravelBackSteps] = walkingQueueX[ptr];
                    travelBackY[numTravelBackSteps++] = walkingQueueY[ptr];
                    dir = Misc.direction(walkingQueueX[ptr], walkingQueueY[ptr], firstX, firstY);
                    if (lastDir != dir) {
                        found = true;
                        break;
                    }
                } while (ptr != wQueueWritePtr);
            } else found = true;
            if (!found) println_debug("Fatal: couldn\'t find connection vertex! Dropping packet.");
            else {
                wQueueWritePtr = wQueueReadPtr;
                addToWalkingQueue(currentX, currentY);
                if (dir != -1 && (dir & 1) != 0) {
                    for (int i = 0; i < numTravelBackSteps - 1; i++) {
                        addToWalkingQueue(travelBackX[i], travelBackY[i]);
                    }
                    int wayPointX2 = travelBackX[numTravelBackSteps - 1];
                    int wayPointY2 = travelBackY[numTravelBackSteps - 1];
                    int wayPointX1;
                    int wayPointY1;
                    if (numTravelBackSteps == 1) {
                        wayPointX1 = currentX;
                        wayPointY1 = currentY;
                    } else {
                        wayPointX1 = travelBackX[numTravelBackSteps - 2];
                        wayPointY1 = travelBackY[numTravelBackSteps - 2];
                    }
                    dir = Misc.direction(wayPointX1, wayPointY1, wayPointX2, wayPointY2);
                    if (dir == -1 || (dir & 1) != 0) {
                        println_debug("Fatal: The walking queue is corrupt! wp1=(" + wayPointX1 + ", " + wayPointY1 + "), " + "wp2=(" + wayPointX2 + ", " + wayPointY2 + ")");
                    } else {
                        dir >>= 1;
                        found = false;
                        int x = wayPointX1;
                        int y = wayPointY1;
                        while (x != wayPointX2 || y != wayPointY2) {
                            x += Misc.directionDeltaX[dir];
                            y += Misc.directionDeltaY[dir];
                            if ((Misc.direction(x, y, firstX, firstY) & 1) == 0) {
                                found = true;
                                break;
                            }
                        }
                        if (!found) {
                            println_debug("Fatal: Internal error: unable to determine connection vertex!" + "  wp1=(" + wayPointX1 + ", " + wayPointY1 + "), wp2=(" + wayPointX2 + ", " + wayPointY2 + "), " + "first=(" + firstX + ", " + firstY + ")");
                        } else addToWalkingQueue(wayPointX1, wayPointY1);
                    }
                } else {
                    for (int i = 0; i < numTravelBackSteps; i++) {
                        addToWalkingQueue(travelBackX[i], travelBackY[i]);
                    }
                }
                for (int i = 0; i < newWalkCmdSteps; i++) {
                    addToWalkingQueue(getNewWalkCmdX()[i], getNewWalkCmdY()[i]);
                }
            }
        }
    }

    public void getNextPlayerMovement() {
        mapRegionDidChange = false;
        didTeleport = false;
        dir1 = dir2 = -1;
        if (getTeleportToX() != -1 && getTeleportToY() != -1) {
            mapRegionDidChange = true;
            if (mapRegionX != -1 && mapRegionY != -1) {
                int relX = getTeleportToX() - mapRegionX * 8;
                int relY = getTeleportToY() - mapRegionY * 8;
                if (relX >= 2 * 8 && relX < 11 * 8 && relY >= 2 * 8 && relY < 11 * 8) mapRegionDidChange = false;
            }
            if (mapRegionDidChange) {
                mapRegionX = (getTeleportToX() >> 3) - 6;
                mapRegionY = (getTeleportToY() >> 3) - 6;
            }
            currentX = getTeleportToX() - 8 * mapRegionX;
            currentY = getTeleportToY() - 8 * mapRegionY;

            this.getRegionProvider().removeNpcClipping(RegionProvider.NPC_TILE_FLAG, absX, absY, heightLevel);
            absX = getTeleportToX();
            absY = getTeleportToY();
            this.getRegionProvider().addNpcClipping(RegionProvider.NPC_TILE_FLAG, absX, absY, heightLevel);
            lastX = absX;
            lastY = absY - 1;
            updateController();
            getFarming().doConfig();
            resetWalkingQueue();
            setTeleportToX(-1);
            setTeleportToY(-1);
            didTeleport = true;
            postTeleportProcessing();
            runningDistanceTravelled = 0;
        } else {
            if (freezeTimer > 0) {
                resetWalkingQueue();
                return;
            }
            dir1 = getNextWalkingDirection();
            if (dir1 == -1) {
                runningDistanceTravelled = 0;
                return;
            }
            if (isRunningToggled() && getMovementState().isRunningEnabled()) {
                dir2 = getNextWalkingDirection();
                runningDistanceTravelled++;
            } else {
                runningDistanceTravelled = 0;
            }
            int deltaX = 0;
            int deltaY = 0;
            if (currentX < 2 * 8) {
                deltaX = 4 * 8;
                mapRegionX -= 4;
                mapRegionDidChange = true;
            } else if (currentX >= 11 * 8) {
                deltaX = -4 * 8;
                mapRegionX += 4;
                mapRegionDidChange = true;
            }
            if (currentY < 2 * 8) {
                deltaY = 4 * 8;
                mapRegionY -= 4;
                mapRegionDidChange = true;
            } else if (currentY >= 11 * 8) {
                deltaY = -4 * 8;
                mapRegionY += 4;
                mapRegionDidChange = true;
            }
            if (mapRegionDidChange) {
                currentX += deltaX;
                currentY += deltaY;
                for (int i = 0; i < walkingQueueSize; i++) {
                    walkingQueueX[i] += deltaX;
                    walkingQueueY[i] += deltaY;
                }
            }
        }
        if (firstMove) {
            firstMove = false;
            checkLocationOnLogin();
        }
    }

    public void checkLocationOnLogin() {
        if (Boundary.isIn(this, PestControl.GAME_BOUNDARY)) {
            getPA().movePlayerUnconditionally(2657, 2639, 0);
        }
        if (Boundary.isIn(this, Boundary.FIGHT_CAVE)) {
            getPA().movePlayerUnconditionally(2401, 5087, (getIndex() + 1) * 4);
            sendMessage("Wave " + (this.waveId + 1) + " will start in approximately 5-10 seconds. ");
            getFightCave().spawn();
        }
        if (Boundary.isIn(this, Boundary.ZULRAH)) {
            getPA().movePlayerUnconditionally(Configuration.EDGEVILLE_X, Configuration.EDGEVILLE_Y, 0);
        }
        for (LobbyType lobbyType : LobbyType.values()) {
            LobbyManager.get(lobbyType).ifPresent(lobby -> {
                if (lobby.inLobby(this)) {
                    if (lobby.canJoin(this)) lobby.attemptJoin(this);
                    else getPA().movePlayerUnconditionally(3033, 6068, 0);//TODO Make this independent for all lobbies
                }
            });
        }
        if (Boundary.isIn(this, Boundary.RAIDS) || Boundary.isIn(this, Boundary.OLM)) {
            RaidConstants.checkLogin(this);
        }
    }

    public void postTeleportProcessing() {
        if (getPosition().inGodwars()) {
            if (equippedGodItems == null) {
                updateGodItems();
            }
        } else if (equippedGodItems != null) {
            equippedGodItems = null;
            godwars.initialize();
        }
    }

    public void updateThisPlayerMovement(Stream str) {
        if (didTeleport) {
            str.createFrameVarSizeWord(81);
            str.initBitAccess();
            str.writeBits(1, 1);
            str.writeBits(2, 3);
            str.writeBits(2, heightLevel);
            str.writeBits(1, 1);
            str.writeBits(1, (isUpdateRequired()) ? 1 : 0);
            str.writeBits(7, currentY);
            str.writeBits(7, currentX);
            return;
        }
        if (dir1 == -1) {
            // don't have to update the character position, because we're just
            // standing
            str.createFrameVarSizeWord(81);
            str.initBitAccess();
            isMoving = false;
            if (isUpdateRequired()) {
                // tell client there's an update block appended at the end
                str.writeBits(1, 1);
                str.writeBits(2, 0);
            } else {
                str.writeBits(1, 0);
            }
        } else {
            str.createFrameVarSizeWord(81);
            str.initBitAccess();
            str.writeBits(1, 1);
            if (dir2 == -1) {
                isMoving = true;
                str.writeBits(2, 1);
                str.writeBits(3, Misc.xlateDirectionToClient[dir1]);
                if (isUpdateRequired()) str.writeBits(1, 1);
                else str.writeBits(1, 0);
            } else {
                isMoving = true;
                str.writeBits(2, 2);
                str.writeBits(3, Misc.xlateDirectionToClient[dir1]);
                str.writeBits(3, Misc.xlateDirectionToClient[dir2]);
                if (isUpdateRequired()) str.writeBits(1, 1);
                else str.writeBits(1, 0);
            }
        }
    }

    public void updatePlayerMovement(Stream str) {
        // synchronized(this) {
        if (dir1 == -1) {
            if (isUpdateRequired() || isChatTextUpdateRequired()) {
                str.writeBits(1, 1);
                str.writeBits(2, 0);
            } else str.writeBits(1, 0);
        } else if (dir2 == -1) {
            str.writeBits(1, 1);
            str.writeBits(2, 1);
            str.writeBits(3, Misc.xlateDirectionToClient[dir1]);
            str.writeBits(1, (isUpdateRequired() || isChatTextUpdateRequired()) ? 1 : 0);
        } else {
            str.writeBits(1, 1);
            str.writeBits(2, 2);
            str.writeBits(3, Misc.xlateDirectionToClient[dir1]);
            str.writeBits(3, Misc.xlateDirectionToClient[dir2]);
            str.writeBits(1, (isUpdateRequired() || isChatTextUpdateRequired()) ? 1 : 0);
        }
    }

    public void addNewNPC(NPC npc, Stream str, Stream updateBlock, boolean flag) {
        int id = npc.getIndex();
        npcList[npcListSize++] = npc;
        str.writeBits(14, id);
        int y = npc.absY - absY;
        int x = npc.absX - absX;
        str.writeBits(5, y < 0 ? y + 32 : y);
        str.writeBits(5, x < 0 ? x + 32 : x);
        str.writeBits(1, flag ? 1 : 0);
        str.writeBits(14, npc.getNpcId());
        boolean pet = PetHandler.getItemIdForNpcId(npc.getNpcId()) != 0;
        if (pet && npc.spawnedBy == getIndex()) {
            str.writeBits(2, 2);
        } else if (pet) {
            str.writeBits(2, 1);
        } else {
            str.writeBits(2, 0);
        }
        boolean savedUpdateRequired = npc.isUpdateRequired();
        npc.setUpdateRequired(true);
        npc.appendNPCUpdateBlock(this, updateBlock);
        npc.setUpdateRequired(savedUpdateRequired);
        str.writeBits(1, 1);
    }

    public void addNewPlayer(Player plr, Stream str, Stream updateBlock) {
        if (playerListSize >= Configuration.MAX_PLAYERS_IN_LOCAL_LIST) {
            return;
        }
        int id = plr.getIndex();
        playerInListBitmap[id >> 3] |= 1 << (id & 7);
        playerList[playerListSize++] = plr;
        str.writeBits(11, id);
        str.writeBits(1, 1);
        boolean savedFlag = plr.isAppearanceUpdateRequired();
        boolean savedUpdateRequired = plr.isUpdateRequired();
        plr.setAppearanceUpdateRequired(true);
        plr.setUpdateRequired(true);
        plr.appendPlayerUpdateBlock(updateBlock);
        plr.setAppearanceUpdateRequired(savedFlag);
        plr.setUpdateRequired(savedUpdateRequired);
        str.writeBits(1, 1);
        int z = plr.absY - absY;
        if (z < 0) z += 32;
        str.writeBits(5, z);
        z = plr.absX - absX;
        if (z < 0) z += 32;
        str.writeBits(5, z);
    }

    protected void appendPlayerAppearance(Stream str) {
        appearanceUpdateBlockCache.currentOffset = 0;
        appearanceUpdateBlockCache.writeByte(playerAppearance[0]);
        StringBuilder sb = new StringBuilder(titles.getCurrentTitle());
        if (titles.getCurrentTitle().equalsIgnoreCase("None")) {
            sb.delete(0, sb.length());
        }
        appearanceUpdateBlockCache.writeString(sb.toString());
        sb = new StringBuilder(rights.getPrimary().getColor());
        if (titles.getCurrentTitle().equalsIgnoreCase("None")) {
            sb.delete(0, sb.length());
        }
        appearanceUpdateBlockCache.writeString(sb.toString());
        appearanceUpdateBlockCache.writeByte(getHealth().getStatus().getMask());
        appearanceUpdateBlockCache.writeByte(headIcon);
        appearanceUpdateBlockCache.writeByte(headIconPk);
        if (isNpc == false) {
            if (playerEquipment[playerHat] > 1) {
                appearanceUpdateBlockCache.writeUnsignedWord(512 + playerEquipment[playerHat]);
            } else {
                appearanceUpdateBlockCache.writeByte(0);
            }
            if (playerEquipment[playerCape] > 1) {
                appearanceUpdateBlockCache.writeUnsignedWord(512 + playerEquipment[playerCape]);
            } else {
                appearanceUpdateBlockCache.writeByte(0);
            }
            if (playerEquipment[playerAmulet] > 1) {
                appearanceUpdateBlockCache.writeUnsignedWord(512 + playerEquipment[playerAmulet]);
            } else {
                appearanceUpdateBlockCache.writeByte(0);
            }
            if (playerEquipment[playerWeapon] > 1) {
                appearanceUpdateBlockCache.writeUnsignedWord(512 + playerEquipment[playerWeapon]);
            } else {
                appearanceUpdateBlockCache.writeByte(0);
            }
            if (playerEquipment[playerChest] > 1) {
                appearanceUpdateBlockCache.writeUnsignedWord(512 + playerEquipment[playerChest]);
            } else {
                appearanceUpdateBlockCache.writeUnsignedWord(256 + playerAppearance[2]);
            }
            if (playerEquipment[playerShield] > 1) {
                appearanceUpdateBlockCache.writeUnsignedWord(512 + playerEquipment[playerShield]);
            } else {
                appearanceUpdateBlockCache.writeByte(0);
            }
            if (ItemDef.forId(playerEquipment[playerChest]).getEquipmentModelType() != EquipmentModelType.FULL_BODY) {
                appearanceUpdateBlockCache.writeUnsignedWord(256 + playerAppearance[3]);
            } else {
                appearanceUpdateBlockCache.writeByte(0);
            }
            if (playerEquipment[playerLegs] > 1) {
                appearanceUpdateBlockCache.writeUnsignedWord(512 + playerEquipment[playerLegs]);
            } else {
                appearanceUpdateBlockCache.writeUnsignedWord(256 + playerAppearance[5]);
            }
            if (ItemDef.forId(playerEquipment[playerHat]).getEquipmentModelType() != EquipmentModelType.FULL_MASK && ItemDef.forId(playerEquipment[playerHat]).getEquipmentModelType() != EquipmentModelType.FULL_HELMET) {
                appearanceUpdateBlockCache.writeUnsignedWord(256 + playerAppearance[1]);
            } else {
                appearanceUpdateBlockCache.writeByte(0);
            }
            if (playerEquipment[playerHands] > 1) {
                appearanceUpdateBlockCache.writeUnsignedWord(512 + playerEquipment[playerHands]);
            } else {
                appearanceUpdateBlockCache.writeUnsignedWord(256 + playerAppearance[4]);
            }
            if (playerEquipment[playerFeet] > 1) {
                appearanceUpdateBlockCache.writeUnsignedWord(512 + playerEquipment[playerFeet]);
            } else {
                appearanceUpdateBlockCache.writeUnsignedWord(256 + playerAppearance[6]);
            }
            if (playerAppearance[0] != 1 && ItemDef.forId(playerEquipment[playerHat]).getEquipmentModelType() != EquipmentModelType.FULL_MASK) {
                appearanceUpdateBlockCache.writeUnsignedWord(256 + playerAppearance[7]);
            } else {
                appearanceUpdateBlockCache.writeByte(0);
            }
        } else {
            appearanceUpdateBlockCache.writeUnsignedWord(-1);
            appearanceUpdateBlockCache.writeUnsignedWord(npcId2);
        }
        appearanceUpdateBlockCache.writeByte(playerAppearance[8]);
        appearanceUpdateBlockCache.writeByte(playerAppearance[9]);
        appearanceUpdateBlockCache.writeByte(playerAppearance[10]);
        appearanceUpdateBlockCache.writeByte(playerAppearance[11]);
        appearanceUpdateBlockCache.writeByte(playerAppearance[12]);
        appearanceUpdateBlockCache.writeUnsignedWord(playerStandIndex); // standAnimIndex
        appearanceUpdateBlockCache.writeUnsignedWord(playerTurnIndex); // standTurnAnimIndex
        appearanceUpdateBlockCache.writeUnsignedWord(playerWalkIndex); // walkAnimIndex
        appearanceUpdateBlockCache.writeUnsignedWord(playerTurn180Index); // turn180AnimIndex
        appearanceUpdateBlockCache.writeUnsignedWord(playerTurn90CWIndex); // turn90CWAnimIndex
        appearanceUpdateBlockCache.writeUnsignedWord(playerTurn90CCWIndex); // turn90CCWAnimIndex
        appearanceUpdateBlockCache.writeUnsignedWord(playerRunIndex); // runAnimIndex
        appearanceUpdateBlockCache.writeString(getDisplayName());
        appearanceUpdateBlockCache.writeByte(isInvisible() ? 1 : 0);
        combatLevel = calculateCombatLevel();
        appearanceUpdateBlockCache.writeByte(combatLevel); // combat level
        Set<Right> rightsSet = rights.getSet();
        appearanceUpdateBlockCache.writeByte(rightsSet.size());
        for (Right right : rightsSet) {
            appearanceUpdateBlockCache.writeByte(right.ordinal());
        }
        appearanceUpdateBlockCache.writeUnsignedWord(0);
        str.writeByteC(appearanceUpdateBlockCache.currentOffset);
        str.writeBytes(appearanceUpdateBlockCache.buffer, appearanceUpdateBlockCache.currentOffset, 0);
    }

    public int calculateCombatLevel() {
        int j = getLevelForXP(playerXP[playerAttack]);
        int k = getLevelForXP(playerXP[playerDefence]);
        int l = getLevelForXP(playerXP[playerStrength]);
        int i1 = getLevelForXP(playerXP[playerHitpoints]);
        int j1 = getLevelForXP(playerXP[playerPrayer]);
        int k1 = getLevelForXP(playerXP[playerRanged]);
        int l1 = getLevelForXP(playerXP[playerMagic]);
        int combatLevel = (int) (((k + i1) + Math.floor(j1 / 2)) * 0.24798) + 1;
        double d = (j + l) * 0.325;
        double d1 = Math.floor(k1 * 1.5) * 0.325;
        double d2 = Math.floor(l1 * 1.5) * 0.325;
        if (d >= d1 && d >= d2) {
            combatLevel += d;
        } else if (d1 >= d && d1 >= d2) {
            combatLevel += d1;
        } else if (d2 >= d && d2 >= d1) {
            combatLevel += d2;
        }
        return combatLevel;
    }

    /**
     * Permanently set a skill level and update health and reset it to full if applicable.
     */
    public void setLevel(Skill skill, int experience, boolean clientUpdate) {
        playerXP[skill.getId()] = experience;
        playerLevel[skill.getId()] = getLevelForXP(experience);

        if (skill == Skill.HITPOINTS) {
            getHealth().setCurrentHealth(getLevel(Skill.HITPOINTS));
            getHealth().setMaximumHealth(getLevel(Skill.HITPOINTS));
            getHealth().reset();
        }

        if (clientUpdate)
            getPA().refreshSkill(skill.getId());
    }

    public int getLevel(Skill skill) {
        if (skill == Skill.DEFENCE) {
            if (ToragsEffect.INSTANCE.canUseEffect(this)) {
                return (int) ToragsEffect.modifyDefenceLevel(this);
            }
        }

        return playerLevel[skill.getId()];
    }

    public int getExperience(Skill skill) {
        return playerXP[skill.getId()];
    }

    /**
     * Restore a skill level, doesn't go over max.
     */
    public void restore(Skill skill, int amount) {
        playerLevel[skill.getId()] += amount;
        int maxLevel = getLevelForXP(playerXP[skill.getId()]);
        if (playerLevel[skill.getId()] > maxLevel) {
            playerLevel[skill.getId()] = maxLevel;
        }
        getPA().refreshSkill(skill.getId());
    }

    public int getLevelForXP(int exp) {
        int points = 0;
        int output = 0;
        for (int lvl = 1; lvl <= 99; lvl++) {
            points += Math.floor(lvl + 300.0 * Math.pow(2.0, lvl / 7.0));
            output = (int) Math.floor(points / 4);
            if (output >= exp) return lvl;
        }
        return 99;
    }

    protected void appendPlayerChatText(Stream str) {
        str.writeWordBigEndian(((getChatTextColor() & 255) << 8) + (getChatTextEffects() & 255));
        str.writeByte(rights.getPrimary().getValue());
        str.writeByteC(getChatTextSize());
        str.writeBytes_reverse(getChatText(), getChatTextSize(), 0);
    }

    public void forcedChat(String text) {
        forcedText = text;
        forcedChatUpdateRequired = true;
        setUpdateRequired(true);
        setAppearanceUpdateRequired(true);
    }

    public void appendForcedChat(Stream str) {
        str.writeString(forcedText);
    }

    public void appendMask100Update(Stream str) {
        str.writeWordBigEndian(getGraphic().getId());
        str.writeDWord(getGraphic().getHeight() + (getGraphic().getDelay() & 65535));
    }

    public void gfx100(int gfx) {
        startGraphic(new Graphic(gfx, Graphic.GraphicHeight.MIDDLE));
    }

    public void gfx0(int gfx) {
        startGraphic(new Graphic(gfx, Graphic.GraphicHeight.LOW));
    }

    /**
     * Animations
     */
    public void startAnimation(int animId) {
        startAnimation(new Animation(animId));
    }

    public void startAnimation(int animId, int time) {
        startAnimation(new Animation(animId, time));
    }

    public void stopAnimation() {
        startAnimation(new Animation(65535));
    }

    public void appendAnimationRequest(Stream str) {
        str.writeWordBigEndian((getAnimation() == null || getAnimation().getId() == -1) ? 65535 : getAnimation().getId());
        str.writeByteC(getAnimation().getDelay());
    }

    public void faceEntity(Entity entity) {
        faceUpdate(entity.getIndex() + (entity.isPlayer() ? 32768 : 0));
    }

    public void faceUpdate(int index) {
        face = index;
        faceUpdateRequired = true;
        setUpdateRequired(true);
    }

    public void appendFaceUpdate(Stream str) {
        str.writeWordBigEndian(face);
    }

    public void facePosition(Position position) {
        facePosition(position.getX(), position.getY());
    }

    public void facePosition(int pointX, int pointY) {
        FocusPointX = 2 * pointX + 1;
        FocusPointY = 2 * pointY + 1;
        setUpdateRequired(true);
    }

    private void appendSetFocusDestination(Stream str) {
        // synchronized(this) {
        str.writeWordBigEndianA(FocusPointX);
        str.writeWordBigEndian(FocusPointY);
    }

    @Override
    public boolean isFreezable() {
        return true;
    }

    @Override
    public void appendHeal(int amount, Hitmark h) {
        if (teleTimer <= 0) {
            if (!invincible) {
                getHealth().increase(amount);
            }
            if (amount > 0 && h != null && h == Hitmark.MISS) {
                h = Hitmark.HIT;
            }
            if (!hitUpdateRequired) {
                hitUpdateRequired = true;
                hitDiff = amount;
                hitmark1 = h;
            } else if (!hitUpdateRequired2) {
                hitUpdateRequired2 = true;
                hitDiff2 = amount;
                hitmark2 = h;
            }
        } else {
            if (hitUpdateRequired) {
                hitUpdateRequired = false;
            }
            if (hitUpdateRequired2) {
                hitUpdateRequired2 = false;
            }
        }
        setUpdateRequired(true);
    }

    @Override
    public void appendDamage(Entity entity, int damage, Hitmark h) {
        // Attempting a fix to dying after teleport here.
        if (entity != null && distance(entity.getPosition()) > 36) {
            return;
        }

        // Converts all hits to 0, but processes effects

        if (getAttributes().getBoolean("GODMODE")) {
            damage = 0;
        }

        // Fix for being killed inside Theatre of Blood cage after death at final boss
        if (getAttributes().getBoolean(TobInstance.TOB_DEAD_ATTR_KEY, false)) {
            if (Boundary.isIn(this, TobConstants.ALL_BOUNDARIES) && getInstance() != null) {
                return;
            }

            getAttributes().removeBoolean(TobInstance.TOB_DEAD_ATTR_KEY); // Remove cause not in TOB anymore
        }

        // Degrade items when hitting by normal hits
        if (h == Hitmark.HIT || h == Hitmark.MISS) {
            Degrade.degradeDefending(this);
        }

        if (damage < 0) {
            damage = 0;
            h = Hitmark.MISS;
        }
        if (getHealth().getCurrentHealth() - damage < 0) {
            damage = getHealth().getCurrentHealth();
        }


        MeleeExtras.handleRedemption(this, damage);


        if (teleTimer <= 0) {
            if (!invincible) {
                getHealth().reduce(damage);
            }
            if (damage > 0 && h != null && h == Hitmark.MISS) {
                h = Hitmark.HIT;
            }
            if (!hitUpdateRequired) {
                hitUpdateRequired = true;
                hitDiff = damage;
                hitmark1 = h;
            } else if (!hitUpdateRequired2) {
                hitUpdateRequired2 = true;
                hitDiff2 = damage;
                hitmark2 = h;
            }
            lastDamageTaken = damage;
        } else {
            if (hitUpdateRequired) {
                hitUpdateRequired = false;
            }
            if (hitUpdateRequired2) {
                hitUpdateRequired2 = false;
            }
        }
        setUpdateRequired(true);
    }

    @Override
    protected void appendHitUpdate(Stream str) {
        str.writeByte(hitDiff);
        if (hitmark1 == null) {
            str.writeByteA(0);
        } else {
            str.writeByteA(hitmark1.getId());
        }
        if (getHealth().getCurrentHealth() <= 0) {
            isDead = true;
        }
        str.writeByteC(getHealth().getCurrentHealth());
        str.writeByte(getHealth().getMaximumHealth());
    }

    @Override
    protected void appendHitUpdate2(Stream str) {
        str.writeByte(hitDiff2);
        if (hitmark2 == null) {
            str.writeByteS(0);
        } else {
            str.writeByteS(hitmark2.getId());
        }
        if (getHealth().getCurrentHealth() <= 0) {
            isDead = true;
        }
        str.writeByte(getHealth().getCurrentHealth());
        str.writeByteC(getHealth().getMaximumHealth());
    }

    /**
     * Direction, 2 = South, 0 = North, 3 = West, 2 = East?
     *
     * @param xOffset
     * @param yOffset
     * @param speed1
     * @param speed2
     * @param direction
     * @param emote
     */
    private int xOffsetWalk;
    private int yOffsetWalk;
    public int dropSize;
    public boolean sellingX;
    public int currentPrestigeLevel;
    public int prestigeNumber;
    public boolean canPrestige;
    public int prestigePoints;
    public boolean newStarter;
    public boolean spawnedbarrows;
    public boolean absorption;
    public boolean announce = true;
    public boolean lootPickUp;
    public boolean xpScroll;
    public long xpScrollTicks;
    public boolean skillingPetRateScroll;
    public long skillingPetRateTicks;
    public boolean fasterCluesScroll;
    public long fasterCluesTicks;
    public boolean augury;
    public boolean rigour;
    public boolean usedFc;
    public int startDate = -1;
    public int LastLoginYear;
    public int LastLoginMonth;
    public int LastLoginDate;
    public int LoginStreak;
    /*
     * diary completion
     */
    public boolean d1Complete;
    public boolean d2Complete;
    public boolean d3Complete;
    public boolean d4Complete;
    public boolean d5Complete;
    public boolean d6Complete;
    public boolean d7Complete;
    public boolean d8Complete;
    public boolean d9Complete;
    public boolean d10Complete;
    public boolean d11Complete;

    /**
     * 0 North 1 East 2 South 3 West
     */
    public void setForceMovement(int xOffset, int yOffset, int speedOne, int speedTwo, String directionSet, int animation) {
        if (isForceMovementActive() || forceMovement) {
            return;
        }
        stopMovement();
        xOffsetWalk = xOffset - absX;
        yOffsetWalk = yOffset - absY;
        playerStandIndex = animation;
        playerRunIndex = animation;
        playerWalkIndex = animation;
        forceMovementActive = true;
        getPA().requestUpdates();
        setAppearanceUpdateRequired(true);
        Server.getEventHandler().submit(new Event<Player>("force_movement", this, 2) {
            @Override
            public void execute() {
                if (attachment == null || attachment.isDisconnected()) {
                    super.stop();
                    return;
                }
                attachment.setUpdateRequired(true);
                attachment.forceMovement = true;
                attachment.x1 = currentX;
                attachment.y1 = currentY;
                attachment.x2 = currentX + xOffsetWalk;
                attachment.y2 = currentY + yOffsetWalk;
                attachment.mask400Var1 = speedOne;
                attachment.mask400Var2 = speedTwo;
                attachment.forceMovementDirection = directionSet == null ? -1 : directionSet == "NORTH" ? 0 : directionSet == "EAST" ? 1 : directionSet == "SOUTH" ? 2 : directionSet == "WEST" ? 3 : 0;
                super.stop();
            }
        });
        int ticks = Math.abs(xOffsetWalk) + Math.abs(yOffsetWalk);
        if (ticks <= 0) ticks = 1;
        Server.getEventHandler().submit(new Event<Player>("force_movement", this, ticks) {
            @Override
            public void execute() {
                if (attachment == null || attachment.isDisconnected()) {
                    super.stop();
                    return;
                }
                forceMovementActive = false;
                attachment.getPA().movePlayer(xOffset, yOffset, attachment.heightLevel);
                if (attachment.playerEquipment[playerWeapon] == -1) {
                    attachment.playerStandIndex = 808;
                    attachment.playerTurnIndex = 823;
                    attachment.playerWalkIndex = 819;
                    attachment.playerTurn180Index = 820;
                    attachment.playerTurn90CWIndex = 821;
                    attachment.playerTurn90CCWIndex = 822;
                    attachment.playerRunIndex = 824;
                } else {
                    MeleeData.setWeaponAnimations(attachment);
                }
                forceMovement = false;
                super.stop();
            }
        });
    }

    public void appendMask400Update(Stream str) {
        str.writeByteS(x1);
        str.writeByteS(y1);
        str.writeByteS(x2);
        str.writeByteS(y2);
        str.writeWordBigEndianA(mask400Var1);
        str.writeWordA(mask400Var2);
        str.writeByteS(forceMovementDirection);
    }

    public void appendPlayerUpdateBlock(Stream str) {
        if (!isUpdateRequired() && !isChatTextUpdateRequired()) return;
        int updateMask = 0;
        if (forceMovement) {
            updateMask |= 1024;
        }
        if (isGfxUpdateRequired()) {
            updateMask |= 256;
        }
        if (isAnimationUpdateRequired()) {
            updateMask |= 8;
        }
        if (forcedChatUpdateRequired) {
            updateMask |= 4;
        }
        if (isChatTextUpdateRequired()) {
            updateMask |= 128;
        }
        if (isAppearanceUpdateRequired()) {
            updateMask |= 16;
        }
        if (faceUpdateRequired) {
            updateMask |= 1;
        }
        if (FocusPointX != -1) {
            updateMask |= 2;
        }
        if (hitUpdateRequired) {
            updateMask |= 32;
        }
        if (hitUpdateRequired2) {
            updateMask |= 512;
        }
        if (updateMask >= 256) {
            updateMask |= 64;
            str.writeByte(updateMask & 255);
            str.writeByte(updateMask >> 8);
        } else {
            str.writeByte(updateMask);
        }
        if (forceMovement) {
            appendMask400Update(str);
        }
        if (isGfxUpdateRequired()) {
            appendMask100Update(str);
        }
        if (isAnimationUpdateRequired()) {
            appendAnimationRequest(str);
        }
        if (forcedChatUpdateRequired) {
            appendForcedChat(str);
        }
        if (isChatTextUpdateRequired()) {
            appendPlayerChatText(str);
        }
        if (faceUpdateRequired) {
            appendFaceUpdate(str);
        }
        if (isAppearanceUpdateRequired()) {
            appendPlayerAppearance(str);
        }
        if (FocusPointX != -1) {
            appendSetFocusDestination(str);
        }
        if (hitUpdateRequired) {
            appendHitUpdate(str);
        }
        if (hitUpdateRequired2) {
            appendHitUpdate2(str);
        }
    }

    public void clearUpdateFlags() {
        setUpdateRequired(false);
        setChatTextUpdateRequired(false);
        setAppearanceUpdateRequired(false);
        hitUpdateRequired = false;
        hitUpdateRequired2 = false;
        forcedChatUpdateRequired = false;
        FocusPointX = -1;
        FocusPointY = -1;
        faceUpdateRequired = false;
        forceMovement = false;
        face = 65535;
        resetAfterUpdate();
    }

    public long lastWebSlash;

    public int getPacketsReceived() {
        return packetsReceived.get();
    }

    public int getMapRegionX() {
        return mapRegionX;
    }

    public int getMapRegionY() {
        return mapRegionY;
    }

    public int getX() {
        return absX;
    }

    @Override
    public void setX(int x) {
        this.absX = x;
        updateController();
    }

    public int getY() {
        return absY;
    }

    @Override
    public void setY(int y) {
        this.absY = y;
        updateController();
    }

    public int getHeight() {
        return this.heightLevel;
    }

    @Override
    public void setHeight(int height) {
        this.heightLevel = heightLevel;
    }

    @Override
    public int getDefenceLevel() {
        return getLevel(Skill.DEFENCE);
    }

    @Override
    public int getDefenceBonus(CombatType combatType, Entity attacker) {
        if (combatType == CombatType.RANGE) {
            return getItems().getBonus(Bonus.DEFENCE_RANGED);
        } else if (combatType == CombatType.MAGE) {
            return getItems().getBonus(Bonus.DEFENCE_MAGIC);
        } else if (combatType == CombatType.MELEE && attacker.isPlayer()) {
            WeaponMode weaponMode = attacker.asPlayer().getCombatConfigs().getWeaponMode();

            CombatStyle style = weaponMode.getCombatStyle();

            if (style == null) {
                System.err.println("No melee weapon style for: " + weaponMode);
                return getItems().getBonus(Bonus.DEFENCE_SLASH);
            }
            switch (style) {
                case STAB:
                    return getItems().getBonus(Bonus.DEFENCE_STAB);
                case SLASH:
                    return getItems().getBonus(Bonus.DEFENCE_SLASH);
                case CRUSH:
                    return getItems().getBonus(Bonus.DEFENCE_CRUSH);
            }
        }
        return playerBonus[MeleeMaxHit.bestMeleeDef(this)];
    }

    @Override
    public boolean hasBlockAnimation() {
        return true;
    }

    @Override
    public Animation getBlockAnimation() {
        return new Animation(MeleeData.getBlockEmote(this));
    }

    @Override
    public boolean isAutoRetaliate() {
        return autoRet == 1 && playerAttackingIndex == 0 && npcAttackingIndex == 0 && isWalkingQueueEmpty();
    }

    public void unfollow() {
        getPA().resetFollow();
        faceUpdate(0);
    }

    @Override
    public void attackEntity(Entity entity) {
        combatFollowing = true;
        if (entity.isPlayer()) {
            playerAttackingIndex = entity.getIndex();
            playerFollowingIndex = entity.getIndex();
            npcAttackingIndex = 0;
            npcFollowingIndex = 0;
        } else {
            npcAttackingIndex = entity.getIndex();
            npcFollowingIndex = entity.getIndex();
            playerAttackingIndex = 0;
            playerFollowingIndex = 0;
        }
    }

    public boolean isTeleblocked() {
        return System.currentTimeMillis() - teleBlockStartMillis < teleBlockLength;
    }

    public Coordinate getCoordinate() {
        return new Coordinate(absX, absY, heightLevel);
    }

    public void setAppearanceUpdateRequired(boolean appearanceUpdateRequired) {
        this.appearanceUpdateRequired = appearanceUpdateRequired;
    }

    public boolean isAppearanceUpdateRequired() {
        return appearanceUpdateRequired;
    }

    public void setChatTextEffects(int chatTextEffects) {
        this.chatTextEffects = chatTextEffects;
    }

    public int getChatTextEffects() {
        return chatTextEffects;
    }

    public void setChatTextSize(byte chatTextSize) {
        this.chatTextSize = chatTextSize;
    }

    public byte getChatTextSize() {
        return chatTextSize;
    }

    public void setChatTextUpdateRequired(boolean chatTextUpdateRequired) {
        this.chatTextUpdateRequired = chatTextUpdateRequired;
    }

    public boolean isChatTextUpdateRequired() {
        return chatTextUpdateRequired;
    }

    public byte[] getChatText() {
        return chatText;
    }

    public void setChatText(byte[] chatText) {
        this.chatText = chatText;
    }

    public void setChatTextColor(int chatTextColor) {
        this.chatTextColor = chatTextColor;
    }

    public int getChatTextColor() {
        return chatTextColor;
    }

    public int[] getNewWalkCmdX() {
        return newWalkCmdX;
    }

    public int[] getNewWalkCmdY() {
        return newWalkCmdY;
    }

    public void setNewWalkCmdIsRunning(boolean newWalkCmdIsRunning) {
        this.newWalkCmdIsRunning = newWalkCmdIsRunning;
    }

    public boolean isNewWalkCmdIsRunning() {
        return newWalkCmdIsRunning;
    }

    public boolean getRingOfLifeEffect() {
        return maxCape[0];
    }

    public boolean setRingOfLifeEffect(boolean effect) {
        return maxCape[0] = effect;
    }

    public boolean getFishingEffect() {
        return maxCape[1];
    }

    public boolean setFishingEffect(boolean effect) {
        return maxCape[1] = effect;
    }

    public boolean getMiningEffect() {
        return maxCape[2];
    }

    public boolean setMiningEffect(boolean effect) {
        return maxCape[2] = effect;
    }

    public boolean getWoodcuttingEffect() {
        return maxCape[3];
    }

    public boolean setWoodcuttingEffect(boolean effect) {
        return maxCape[3] = effect;
    }

    public int getSkeletalMysticDamageCounter() {
        return raidsDamageCounters[0];
    }

    public void setSkeletalMysticDamageCounter(int damage) {
        this.raidsDamageCounters[0] = damage;
    }

    public int getTektonDamageCounter() {
        return raidsDamageCounters[1];
    }

    public void setTektonDamageCounter(int damage) {
        this.raidsDamageCounters[1] = damage;
    }

    public int getGlodDamageCounter() {
        return raidsDamageCounters[9];
    }

    public void setGlodDamageCounter(int damage) {
        this.raidsDamageCounters[9] = damage;
    }

    /**
     * Gatekeeper damage tracker (Non-Visble)
     * Topher please use this as an example and never delete this note, thank you.
     */
    public int getGateKeeperDamageCounter() {
        return GateKeeperCounter[0];
    }

    public void setGateKeeperDamageCounter(int damage) {
        this.GateKeeperCounter[0] = damage;
    }

    public int getHesporiDamageCounter() {
        return raidsDamageCounters[12];
    }

    public void setHesporiDamageCounter(int damage) {
        this.raidsDamageCounters[12] = damage;
    }

    public int getIceQueenDamageCounter() {
        return raidsDamageCounters[4];
    }

    public void setIceQueenDamageCounter(int damage) {
        this.raidsDamageCounters[4] = damage;
    }

    public int getEasyClueCounter() {
        return counters[0];
    }

    public void setEasyClueCounter(int counters) {
        this.counters[0] = counters;
    }

    public int getMediumClueCounter() {
        return counters[1];
    }

    public void setMediumClueCounter(int counters) {
        this.counters[1] = counters;
    }

    public int getHardClueCounter() {
        return counters[2];
    }

    public void setHardClueCounter(int counters) {
        this.counters[2] = counters;
    }

    public int getMasterClueCounter() {
        return counters[3];
    }

    public void setMasterClueCounter(int counters) {
        this.counters[3] = counters;
    }

    public int getBarrowsChestCounter() {
        return counters[4];
    }

    public void setBarrowsChestCounter(int counters) {
        this.counters[4] = counters;
    }

    public int getDuelWinsCounter() {
        return counters[5];
    }

    public void setDuelWinsCounter(int counters) {
        this.counters[5] = counters;
    }

    public int getDuelLossCounter() {
        return counters[6];
    }

    public void setDuelLossCounter(int counters) {
        this.counters[6] = counters;
    }

    public String getLastClanChat() {
        return lastClanChat;
    }

    public void setLastClanChat(String founder) {
        lastClanChat = founder;
    }

    public long getNameAsLong() {
        return nameAsLong;
    }

    public void setNameAsLong(long hash) {
        this.nameAsLong = hash;
    }

    public void setStopPlayer(boolean stopPlayer) {
    }

    public boolean isDead() {
        return getHealth().getCurrentHealth() <= 0 || this.isDead;
    }

    public void setTrading(boolean trading) {
    }

    public boolean inGodmode() {
        return godmode;
    }

    public void setGodmode(boolean godmode) {
        this.godmode = godmode;
    }

    public boolean inSafemode() {
        return safemode;
    }

    public void setSafemode(boolean safemode) {
        this.safemode = safemode;
    }

    public void setDragonfireShieldCharge(int charge) {
        this.dragonfireShieldCharge = charge;
    }

    public int getDragonfireShieldCharge() {
        return dragonfireShieldCharge;
    }

    public void setLastDragonfireShieldAttack(long lastAttack) {
        this.lastDragonfireShieldAttack = lastAttack;
    }

    public long getLastDragonfireShieldAttack() {
        return lastDragonfireShieldAttack;
    }

    /**
     * Retrieves the rights for this player.
     *
     * @return the rights
     */
    public RightGroup getRights() {
        if (rights == null) {
            rights = new RightGroup(this, Right.PLAYER);
        }
        return rights;
    }

    /**
     * Returns a single instance of the Titles class for this player
     *
     * @return the titles class
     */
    public Titles getTitles() {
        if (titles == null) {
            titles = new Titles(this);
        }
        return titles;
    }

    public RandomEventInterface getInterfaceEvent() {
        return randomEventInterface;
    }

    public UltraMysteryBox getUltraInterface() {
        return ultraMysteryBox;
    }

    public FoeMysteryBox getFoeInterface() {
        return foeMysteryBox;
    }

    public NormalMysteryBox getNormalBoxInterface() {
        return normalMysteryBox;
    }

    public SuperMysteryBox getSuperBoxInterface() {
        return superMysteryBox;
    }

    /**
     * Modifies the current interface open
     *
     * @param openInterface the interface id
     */
    public void setOpenInterface(int openInterface) {
        this.openInterface = openInterface;
    }

    /**
     * The interface that is opened
     *
     * @return the interface id
     */
    public int getOpenInterface() {
        return openInterface;
    }

    /**
     * Determines whether a warning will be shown when dropping an item.
     *
     * @return True if it's the case, False otherwise.
     */
    public boolean showDropWarning() {
        return dropWarning;
    }

    /**
     * Change whether a warning will be shown when dropping items.
     *
     * @param shown True in case a warning must be shown, False otherwise.
     */
    public void setDropWarning(boolean shown) {
        dropWarning = shown;
    }

    public boolean isAlchWarning() {
        return alchWarning;
    }

    public void setAlchWarning(boolean alchWarning) {
        this.alchWarning = alchWarning;
    }

    public boolean getHourlyBoxToggle() {
        return hourlyBoxToggle;
    }

    public void setHourlyBoxToggle(boolean toggle) {
        hourlyBoxToggle = toggle;
    }

    public boolean getFracturedCrystalToggle() {
        return fracturedCrystalToggle;
    }

    public void setFracturedCrystalToggle(boolean toggle1) {
        fracturedCrystalToggle = toggle1;
    }

    public long setBestZulrahTime(long bestZulrahTime) {
        return this.bestZulrahTime = bestZulrahTime;
    }

    public long getBestZulrahTime() {
        return bestZulrahTime;
    }

    public int getArcLightCharge() {
        return arcLightCharge;
    }

    public void setArcLightCharge(int chargeArc) {
        this.arcLightCharge = chargeArc;
    }

    public int getToxicBlowpipeCharge() {
        return toxicBlowpipeCharge;
    }

    public void setToxicBlowpipeCharge(int charge) {
        this.toxicBlowpipeCharge = charge;
    }

    public int getToxicBlowpipeAmmo() {
        return toxicBlowpipeAmmo;
    }

    public void increaseSlaughterCharge(int slaughterCharge) {
        this.slaughterCharge += slaughterCharge;
    }

    public void decreaseSlaughterCharge(int slaughterCharge) {
        this.slaughterCharge -= slaughterCharge;
    }

    public int getToxicBlowpipeAmmoAmount() {
        return toxicBlowpipeAmmoAmount;
    }

    public void setToxicBlowpipeAmmoAmount(int amount) {
        this.toxicBlowpipeAmmoAmount = amount;
    }

    public void setToxicBlowpipeAmmo(int ammo) {
        this.toxicBlowpipeAmmo = ammo;
    }

    public int getSerpentineHelmCharge() {
        return this.serpentineHelmCharge;
    }

    public void setSerpentineHelmCharge(int charge) {
        this.serpentineHelmCharge = charge;
    }

    public int getTridentCharge() {
        return tridentCharge;
    }

    public void setTridentCharge(int tridentCharge) {
        this.tridentCharge = tridentCharge;
    }

    public int getToxicTridentCharge() {
        return toxicTridentCharge;
    }

    public void setToxicTridentCharge(int toxicTridentCharge) {
        this.toxicTridentCharge = toxicTridentCharge;
    }

    public int getSangStaffCharge() {
        return sangStaffCharge;
    }

    public void setSangStaffCharge(int sangStaffCharge) {
        this.sangStaffCharge = sangStaffCharge;
    }

    public Fletching getFletching() {
        return fletching;
    }

    public Mode getMode() {
        return mode;
    }

    public Mode setMode(Mode mode) {
        return this.mode = mode;
    }

    public String getRevertOption() {
        return revertOption;
    }

    public void setRevertOption(String revertOption) {
        this.revertOption = revertOption;
    }

    public long getRevertModeDelay() {
        return revertModeDelay;
    }

    public void setRevertModeDelay(long revertModeDelay) {
        this.revertModeDelay = revertModeDelay;
    }

    /**
     * @param skillId
     * @param amount
     */
    public void replenishSkill(int skillId, int amount) {
        if (skillId < 0 || skillId > playerLevel.length - 1) {
            return;
        }
        int maximum = getLevelForXP(playerXP[skillId]);
        if (playerLevel[skillId] == maximum) {
            return;
        }
        playerLevel[skillId] += amount;
        if (playerLevel[skillId] > maximum) {
            playerLevel[skillId] = maximum;
        }
        playerAssistant.refreshSkill(skillId);
    }

    public int lastSymbolDistanceCheck;

    public List<NPC> derwens_orbs = Lists.newArrayList();

    public int[] activeMageArena2BossId = new int[3];

    public Position[] mageArena2Spawns = null;
    public int[] mageArena2SpawnsX = new int[3];
    public int[] mageArena2SpawnsY = new int[3];
    /**
     * Saved
     */

    public boolean[] mageArenaBossKills = new boolean[3];

    public boolean[] mageArena2Stages = new boolean[5];

    public int flamesOfZamorakCasts, clawsOfGuthixCasts, saradominStrikeCasts;

    public boolean completedMageArena2() {
        int count = 0;
        for (boolean b : mageArenaBossKills) {
            if (b)
                count++;
        }
        return count >= 3 || mageArena2Stages[1];
    }

    public boolean hasMageArena2BossItem(int bossEnumId) {
        int itemIdRequired = -1;
        if (bossEnumId == 0)
            itemIdRequired = 21797;
        else if (bossEnumId == 1)
            itemIdRequired = 21798;
        else
            itemIdRequired = 21799;
        return this.getItems().playerHasItem(itemIdRequired) || getBank().containsItem(itemIdRequired);
    }

    /**
     * Removes custom spawned npcs e.g for minigames
     */
    public void clearUpPlayerNPCsForLogout() {
        for (NPC n : NPCHandler.npcs) {
            if (n != null) {
                if (n.spawnedBy == this.getIndex()) {
                    if (n.isPet)
                        continue;
                    n.unregister();
                }
            }
        }
    }

    public void clearDerwensOrbs() {
        for (NPC n : derwens_orbs) {
            if (n != null) {
                n.unregister();
            }
            derwens_orbs = Lists.newArrayList();
        }
    }

    public void setArenaPoints(int arenaPoints) {
        this.arenaPoints = arenaPoints;
    }

    public int getArenaPoints() {
        return arenaPoints;
    }

    public String getKonarSlayerLocation() {
        return konarSlayerLocation;
    }

    public void setKonarSlayerLocation(String location) {
        this.konarSlayerLocation = location;
    }

    public String getLastTask() {
        return lastTask;
    }

    public void setLastTask(String location) {
        this.lastTask = location;
    }

    public void setShayPoints(int shayPoints) {
        this.shayPoints = shayPoints;
    }

    public int getShayPoints() {
        return shayPoints;
    }

    public void setRaidPoints(int raidPoints) {
        this.raidPoints = raidPoints;
    }

    public int getRaidPoints() {
        return raidPoints;
    }

    public void braceletDecrease(int ether) {
        this.braceletEtherCount -= ether;
    }

    public void braceletIncrease(int ether) {
        this.braceletEtherCount += ether;
    }

    static {
        appearanceUpdateBlockCache = new Stream(new byte[100]);
    }

    @Override
    public boolean susceptibleTo(HealthStatus status) {
        return !getItems().isWearingItem(12931, playerHat) && !getItems().isWearingItem(13199, playerHat) && !getItems().isWearingItem(13197, playerHat);
    }

    @Override
    public void removeFromInstance() {
        if (getInstance() != null) {
            getInstance().remove(this);
        }
    }

    @Override
    public int getEntitySize() {
        return 1;
    }

    public int getToxicStaffOfTheDeadCharge() {
        return toxicStaffOfTheDeadCharge;
    }

    public void setToxicStaffOfTheDeadCharge(int toxicStaffOfTheDeadCharge) {
        this.toxicStaffOfTheDeadCharge = toxicStaffOfTheDeadCharge;
    }

    public long getExperienceCounter() {
        return experienceCounter;
    }

    public void setExperienceCounter(long experienceCounter) {
        this.experienceCounter = experienceCounter;
    }

    public int getRunEnergy() {
        return runEnergy;
    }

    public void setRunEnergy(int runEnergy, boolean update) {
        if (runEnergy < 0) {
            runEnergy = 0;
        }
        this.runEnergy = runEnergy;
        if (update) {
            getPA().updateRunEnergy();
        }
    }

    public Entity getTargeted() {
        return targeted;
    }

    public void setTargeted(Entity targeted) {
        this.targeted = targeted;
    }

    public LootingBag getLootingBag() {
        return lootingBag;
    }

    public PrestigeSkills getPrestige() {
        return prestigeskills;
    }

    public ExpLock getExpLock() {
        return explock;
    }

    public RunePouch getRunePouch() {
        return runePouch;
    }

    public HerbSack getHerbSack() {
        return herbSack;
    }

    public GemBag getGemBag() {
        return gemBag;
    }

    public AchievementDiaryManager getDiaryManager() {
        return diaryManager;
    }

    public QuickPrayers getQuick() {
        return quick;
    }

    public void setInfernoBestTime(long infernoBestTime) {
    }

    public QuestTab getQuestTab() {
        return questTab;
    }

    public EventCalendar getEventCalendar() {
        return eventCalendar;
    }

    public LocalDate getLastVote() {
        return lastVote;
    }

    public void setLastVote(LocalDate lastVote) {
        this.lastVote = lastVote;
    }

    public LocalDate getLastVotePanelPoint() {
        return lastVotePanelPoint;
    }

    public void setLastVotePanelPoint(LocalDate lastVotePanelPoint) {
        this.lastVotePanelPoint = lastVotePanelPoint;
    }

    public int getEnterAmountInterfaceId() {
        return enterAmountInterfaceId;
    }

    public void setEnterAmountInterfaceId(int enterAmountInterfaceId) {
        this.enterAmountInterfaceId = enterAmountInterfaceId;
    }

    public void updateRunningToggled(boolean runningToggled) {
        this.runningToggled = runningToggled;
        getPA().updateRunningToggle();
    }

    public void setRunningToggled(boolean runningToggled) {
        this.runningToggled = runningToggled;
    }

    public boolean isRunningToggled() {
        return runningToggled;
    }

    public RechargeItems getRechargeItems() {
        return rechargeItems;
    }

    public UltraMysteryBox getUltraMysteryBox() {
        return ultraMysteryBox;
    }

    public YoutubeMysteryBox getYoutubeMysteryBox() {
        return youtubeMysteryBox;
    }

    public NormalMysteryBox getNormalMysteryBox() {
        return normalMysteryBox;
    }

    public boolean isInTradingPost() {
        return inTradingPost;
    }

    public void setInTradingPost(boolean inTradingPost) {
        this.inTradingPost = inTradingPost;
    }

    public Inferno getInferno() {
        if (getInstance() != null && getInstance() instanceof Inferno) {
            return (Inferno) getInstance();
        }
        return null;
    }


    public MageArena getMageArena() {
        return mageArena;
    }

    public Cannon getCannon() {
        return cannon;
    }

    public void setCannon(Cannon cannon) {
        this.cannon = cannon;
    }

    public Inventory getInventory() {
        return inventory;
    }

    public DialogueBuilder getDialogueBuilder() {
        return dialogueBuilder;
    }

    public void setDialogueBuilder(DialogueBuilder dialogueBuilder) {
        this.dialogueBuilder = dialogueBuilder;
    }

    public DailyRewards getDailyRewards() {
        return dailyRewards;
    }

    public Farming getFarming() {
        return farming;
    }

    public ModeSelection getModeSelection() {
        return modeSelection;
    }

    public ModeRevertType getModeRevertType() {
        return modeRevertType;
    }

    public void setModeRevertType(ModeRevertType modeRevertType) {
        this.modeRevertType = modeRevertType;
    }

    public BossTimers getBossTimers() {
        return bossTimers;
    }

    public WogwContributeInterface getWogwContributeInterface() {
        return wogwContributeInterface;
    }

    public DonationRewards getDonationRewards() {
        return donationRewards;
    }

    public TobContainer getTobContainer() {
        return tobContainer;
    }

    public boolean inParty(String type) {
        return getParty() != null && getParty().isType(type);
    }

    public PlayerParty getParty() {
        return party;
    }

    public void setParty(PlayerParty party) {
        this.party = party;
    }

    public NotificationsTab getNotificationsTab() {
        return notificationsTab;
    }

    public boolean isPrintAttackStats() {
        return printAttackStats;
    }

    public void setPrintAttackStats(boolean printAttackStats) {
        this.printAttackStats = printAttackStats;
    }

    public boolean isPrintDefenceStats() {
        return printDefenceStats;
    }

    public void setPrintDefenceStats(boolean printDefenceStats) {
        this.printDefenceStats = printDefenceStats;
    }

    public TickTimer getPotionTimer() {
        return potionTimer;
    }

    /**
     * Gets the combo timer associated with combo eating
     * @return The {@link TickTimer} associated with Combo eating
     */
    public TickTimer getComboTimer() {
        return this.comboTimer;
    }

    public TickTimer getFoodTimer() {
        return foodTimer;
    }

    public Questing getQuesting() {
        return questing;
    }

    public boolean isHelpCcMuted() {
        return helpCcMuted;
    }

    public void setHelpCcMuted(boolean helpCcMuted) {
        this.helpCcMuted = helpCcMuted;
    }

    public boolean isGambleBanned() {
        return gambleBanned;
    }

    public void setGambleBanned(boolean gambleBanned) {
        this.gambleBanned = gambleBanned;
    }

    public TeleportInterface getTeleportInterface() {
        return teleportInterface;
    }

    public boolean isValidUUID() {
        return getUUID() != null && getUUID().length() > 0;
    }

    public String getUUID() {
        return uuid;
    }

    public void setUUID(String uuid) {
        this.uuid = uuid;
    }

    /**
     * Gets a queue of the player'sprevious packets (50 maximum).
     * These packets were not neccesarily handled by the server but
     * they contain every packet that was sent from the client to the server.
     *
     * @return
     */
    public Queue<Integer> getPreviousPackets() {
        return previousPackets;
    }

    public boolean isJoinedIronmanGroup() {
        return joinedIronmanGroup;
    }

    public void setJoinedIronmanGroup(boolean joinedIronmanGroup) {
        this.joinedIronmanGroup = joinedIronmanGroup;
    }

    public void drainPrayer() {
        sendMessage("You have run out of prayer points!");
        playerLevel[5] = 0;
        CombatPrayer.resetPrayers(this);
        prayerId = -1;
        getPA().refreshSkill(5);
    }

    public boolean isReceivedCalendarCosmeticJune2021() {
        return receivedCalendarCosmeticJune2021;
    }

    public void setReceivedCalendarCosmeticJune2021(boolean receivedCalendarCosmeticJune2021) {
        this.receivedCalendarCosmeticJune2021 = receivedCalendarCosmeticJune2021;
    }

    public HashSet<GroundItem> getLocalGroundItems() {
        return localGroundItems;
    }

    public String getDisplayName() {
        return displayName;
    }
    public String getDisplayNameLower() {
        return displayName.toLowerCase();
    }

    public String getDisplayNameFormatted() {
        return getRights().buildCrownString() + " " + getDisplayName();
    }

    /**
     * Set {@link Player#displayName} and {@link Player#displayNameLong}.
     */
    public void setDisplayName(String displayName) {
        this.displayName = displayName;
        this.displayNameLong = Misc.playerNameToInt64(displayName.toLowerCase());
    }

    /**
     * Get the player's lowercased display name as a long value.
     */
    public long getDisplayNameLong() {
        return displayNameLong;
    }

    public boolean isCompletedTutorial() {
        return completedTutorial;
    }

    public void setCompletedTutorial(boolean completedTutorial) {
        this.completedTutorial = completedTutorial;
    }

    public String getLoginName() {
        return loginName;
    }

    public void setLoginName(String loginName) {
        this.loginName = loginName;
    }

    /**
     * @return {@link Player#getLoginName()} lowercase.
     */
    public String getLoginNameLower() {
        return getLoginName().toLowerCase();
    }

    public FriendsList getFriendsList() {
        return friendsList;
    }

    public void sendDestroyItem(int itemId) {
        ItemDef def = ItemDef.forId(itemId);

        if (def == null) {
            return;
        }

        if (!this.getItems().playerHasItem(itemId))
            return;

        final String itemName = def.getName();

        this.start(new DialogueBuilder(this).option(
                "Destroy " + itemName + "?",
                new DialogueOption("Yes", player -> {
                    this.getPA().closeAllWindows();
                    this.getItems().deleteItem(itemId, 1);
                }),
                new DialogueOption("No", player -> {
                    this.getPA().closeAllWindows();
                })
        ).send());
        return;
    }

    public boolean isRequiresPinUnlock() {
        return requiresPinUnlock;
    }

    public void setRequiresPinUnlock(boolean requiresPinUnlock) {
        this.requiresPinUnlock = requiresPinUnlock;
        if (requiresPinUnlock)
            logger.debug("Requires account pin unlock.");
    }

    public List<SkillExperience> getOutlastSkillBackup() {
        return outlastSkillBackup;
    }

    public PerduLostPropertyShop getPerduLostPropertyShop() {
        return perduLostPropertyShop;
    }
    
    public LeaderboardPeriodicity getCurrentLeaderboardPeriod() {
        if (currentLeaderboardPeriod == null)
            return LeaderboardPeriodicity.TODAY;
        return currentLeaderboardPeriod;
    }

    public void setCurrentLeaderboardPeriod(LeaderboardPeriodicity currentLeaderboardPeriod) {
        this.currentLeaderboardPeriod = currentLeaderboardPeriod;
    }

    private LeaderboardPeriodicity currentLeaderboardPeriod;

    public CollectionBox getCollectionBox() {
        return collectionBox;
    }

    public PvpWeapons getPvpWeapons() {
        return pvpWeapons;
    }

    @Override
    public int getBonus(Bonus bonus) {
        return this.getItems().getBonus(bonus);
    }

    public TomeOfFire getTomeOfFire() {
        return this.tomeOfFire;
    }

    public boolean isReceivedVoteStreakRefund() {
        return receivedVoteStreakRefund;
    }

    public void setReceivedVoteStreakRefund(boolean receivedVoteStreakRefund) {
        this.receivedVoteStreakRefund = receivedVoteStreakRefund;
    }

    public int getMigrationVersion() {
        return migrationVersion;
    }

    public void setMigrationVersion(int migrationVersion) {
        this.migrationVersion = migrationVersion;
    }
}
