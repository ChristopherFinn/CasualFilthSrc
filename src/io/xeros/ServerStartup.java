package io.xeros;

import io.xeros.annotate.Init;
import io.xeros.annotate.PostInit;
import io.xeros.content.boosts.Boosts;
import io.xeros.content.bosses.godwars.GodwarsEquipment;
import io.xeros.content.bosses.godwars.GodwarsNPCs;
import io.xeros.content.bosses.nightmare.NightmareStatusNPC;
import io.xeros.content.bosses.sarachnis.SarachnisNpc;
import io.xeros.content.collection_log.CollectionLog;
import io.xeros.content.combat.stats.TrackedMonster;
import io.xeros.content.commands.CommandManager;
import io.xeros.content.dailyrewards.DailyRewardContainer;
import io.xeros.content.dailyrewards.DailyRewardsRecords;
import io.xeros.content.donationrewards.DonationReward;
import io.xeros.content.event.eventcalendar.EventCalendar;
import io.xeros.content.event.eventcalendar.EventCalendarWinnerSelect;
import io.xeros.content.events.monsterhunt.MonsterHunt;
import io.xeros.content.fireofexchange.FireOfExchangeBurnPrice;
import io.xeros.content.polls.PollTab;
import io.xeros.content.preset.PresetManager;
import io.xeros.content.referral.ReferralCode;
import io.xeros.content.skills.runecrafting.ouriana.ZamorakGuardian;
import io.xeros.content.tournaments.TourneyManager;
import io.xeros.content.tradingpost.Listing;
import io.xeros.content.trails.TreasureTrailsRewards;
import io.xeros.content.vote_panel.VotePanelManager;
import io.xeros.content.wogw.Wogw;
import io.xeros.content.worldevent.WorldEventContainer;
import io.xeros.model.Npcs;
import io.xeros.model.collisionmap.ObjectDef;
import io.xeros.model.collisionmap.Region;
import io.xeros.model.collisionmap.doors.DoorDefinition;
import io.xeros.model.cycleevent.impl.BonusApplianceEvent;
import io.xeros.model.cycleevent.impl.DidYouKnowEvent;
import io.xeros.model.cycleevent.impl.LeaderboardUpdateEvent;
import io.xeros.model.cycleevent.impl.UpdateQuestTab;
import io.xeros.model.definitions.AnimationLength;
import io.xeros.model.definitions.ItemDef;
import io.xeros.model.definitions.ItemStats;
import io.xeros.model.definitions.NpcDef;
import io.xeros.model.definitions.NpcStats;
import io.xeros.model.definitions.ShopDef;
import io.xeros.model.entity.npc.NPCRelationship;
import io.xeros.model.entity.npc.NpcSpawnLoader;
import io.xeros.model.entity.npc.stats.NpcCombatDefinition;
import io.xeros.model.entity.player.PlayerFactory;
import io.xeros.model.entity.player.save.PlayerSave;
import io.xeros.model.lobby.LobbyManager;
import io.xeros.model.world.ShopHandler;
import io.xeros.punishments.PunishmentCycleEvent;
import io.xeros.model.entity.player.save.backup.PlayerSaveBackup;
import io.xeros.util.Reflection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Stuff to do on startup.
 * @author Michael Sasse (https://github.com/mikeysasse/)
 */
public class ServerStartup {

    private static final Logger logger = LoggerFactory.getLogger(ServerStartup.class);

    static void load() throws Exception {
        Reflection.getMethodsAnnotatedWith(Init.class).forEach(method -> {
            try {
                method.invoke(null);
            } catch (Exception e) {
                logger.error("Error loading @Init annotated method[{}] inside class[{}]", method, method.getClass(), e);
                e.printStackTrace();
                System.exit(1);
            }
        });

        DonationReward.load();
        PlayerSave.loadPlayerSaveEntries();
        EventCalendarWinnerSelect.getInstance().init();
        TrackedMonster.init();
        Boosts.init();
        ItemDef.load();
        ShopDef.load();
        ShopHandler.load();
        NpcStats.load();
        ItemStats.load();
        NpcDef.load();
        // Npc Combat Definition must be above npc load
        NpcCombatDefinition.load();
        Server.npcHandler.init();
        NPCRelationship.setup();
        EventCalendar.verifyCalendar();
        Server.getPunishments().initialize();
        Server.getEventHandler().submit(new DidYouKnowEvent());
        Server.getEventHandler().submit(new BonusApplianceEvent());
        Server.getEventHandler().submit(new PunishmentCycleEvent(Server.getPunishments(), 50));
        Server.getEventHandler().submit(new UpdateQuestTab());
        Server.getEventHandler().submit(new LeaderboardUpdateEvent());
        Listing.init();
        Wogw.init();
        PollTab.init();
        DoorDefinition.load();
        GodwarsEquipment.load();
        GodwarsNPCs.load();
        LobbyManager.initializeLobbies();
        VotePanelManager.init();
        TourneyManager.initialiseSingleton();
        TourneyManager.getSingleton().init();
        Server.getDropManager().read();
        TreasureTrailsRewards.load();
        AnimationLength.startup();
        PresetManager.getSingleton().init();
        ObjectDef.loadConfig();
        CollectionLog.init();
        Region.load();
        Server.getGlobalObjects().loadGlobalObjectFile();

        // Keep this below region load and object loading
        NpcSpawnLoader.load();
        MonsterHunt.spawnNPC();
        Runtime.getRuntime().addShutdownHook(new ShutdownHook());
        CommandManager.initializeCommands();
        NightmareStatusNPC.init();
        if (Server.isDebug()) {
            PlayerFactory.createTestPlayers();
        }
        ReferralCode.load();
        DailyRewardContainer.load();
        DailyRewardsRecords.load();
        WorldEventContainer.getInstance().initialise();
        FireOfExchangeBurnPrice.init();
        Server.getLogging().schedule();

        ZamorakGuardian.spawn();
        new SarachnisNpc(Npcs.SARACHNIS, SarachnisNpc.SPAWN_POSITION);

        if (Server.isPublic()) {
            PlayerSaveBackup.start(Configuration.PLAYER_SAVE_TIMER_MILLIS, Configuration.PLAYER_SAVE_BACKUP_EVERY_X_SAVE_TICKS);
        }

        Reflection.getMethodsAnnotatedWith(PostInit.class).forEach(method -> {
            try {
                method.invoke(null);
            } catch (Exception e) {
                logger.error("Error loading @PostInit annotated method[{}] inside class[{}]", method, method.getClass(), e);
                e.printStackTrace();
                System.exit(1);
            }
        });
    }

}
