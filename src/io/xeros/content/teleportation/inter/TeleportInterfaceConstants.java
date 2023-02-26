package io.xeros.content.teleportation.inter;

import java.util.Collections;
import java.util.List;
import java.util.stream.IntStream;

import com.google.common.collect.Lists;
import io.xeros.Configuration;
import io.xeros.content.achievement_diary.impl.*;
import io.xeros.model.entity.player.Position;
import io.xeros.model.entity.player.Right;

public class TeleportInterfaceConstants {

    public static final List<Integer> TELEPORT_BUTTON_ID_LIST;
    public static final List<Integer> TELEPORT_MESSAGE_ID_LIST;

    private static final int TELEPORT_LIST_LENGTH = 40;
    private static final int TELEPORT_LIST_START_ID = 44_745;
    private static final int TELEPORT_LIST_ID_INTERVAL = 4;

    /**
     * This requires editing of the client (Interfaces#teleportInterface#teleportAmount) if changed.
     */
    public static final int TELEPORT_LIST_COUNT = 40;

    static {
        List<Integer> buttonList = Lists.newArrayList();
        for (int buttonId = 44_742; buttonId <= (44_742 + (TELEPORT_LIST_COUNT * 4)); buttonId += 4)
            buttonList.add(buttonId);
        TELEPORT_BUTTON_ID_LIST = Collections.unmodifiableList(Lists.newArrayList(buttonList));

        List<Integer> messageIdList = Lists.newArrayList();
        for (int index = 0; index < TELEPORT_LIST_LENGTH; index++) {
            messageIdList.add(TELEPORT_LIST_START_ID + (index * TELEPORT_LIST_ID_INTERVAL));
        }
        TELEPORT_MESSAGE_ID_LIST = Collections.unmodifiableList(Lists.newArrayList(messageIdList));
    }

    public static final int[] TAB_SELECTION_BUTTON_IDS = {39_706, 39_711, 39_716, 39_721, 39_726, 39_731, 39_736};

    public static final int INTERFACE_ID = 39_700;
    public static final int INTERFACE_SCROLLABLE_ID = 44_741;

    private static final TeleportContainer MONSTERS = new TeleportContainer(Lists.newArrayList(
            new TeleportButton("Catacombs", plr -> {
                if (plr.getPA().canTeleport()) {
                    plr.getPA().movePlayer(1664, 10050);
                    plr.getPA().showInterface(33900);
                    plr.sendMessage("@cr10@ Use @pur@::level2@bla@ to be sent to @pur@next catacombs level.");
                }
            }),
            new TeleportButtonStandard("Rock Crabs", new Position(2673, 3710, 0)),
            new TeleportButtonStandard("Cows", new Position(3260, 3272, 0)),
            new TeleportButtonStandard("Bob's Island", new Position(2524, 4775, 0)),
            new TeleportButtonStandard("Jormungand's Prison", new Position(2471, 10420, 0)),
            new TeleportButtonStandard("Forthos Dungeon", new Position(1800, 9948, 0)),
            new TeleportButtonStandard("Elf Warriors", new Position(2897, 2725, 0)),
            new TeleportButtonStandard("Dagannoths", new Position(2442, 10147, 0)),
            new TeleportButtonStandard("Mithril Dragons", new Position(1740, 5342, 0)),
            new TeleportButtonStandard("Slayer Tower", new Position(3428, 3538, 0)),
            new TeleportButtonStandard("Fremennik Slayer Dungeon", new Position(2807, 10002, 0)),
            new TeleportButtonStandard("Taverly Dungeon", new Position(2883, 9800, 0)),
            new TeleportButtonStandard("Stronghold Cave", new Position(2452, 9820, 0)),
            new TeleportButtonStandard("Mount Karuulm", new Position(1311, 3795, 0)),
            new TeleportButtonStandard("Smoke Devils", new Position(2404, 9415, 0)),
            new TeleportButtonStandard("Asgarnian Ice Dungeon", new Position(3053, 9578, 0)),
            new TeleportButtonStandard("Brimhaven Dungeon", new Position(2709, 9476, 0)),
            new TeleportButtonStandard("Lithkren Vault", new Position(1567, 5074, 0)),
            new TeleportButtonStandard("Crystal Cavern", new Position(3272, 6052, 0)),
            new TeleportButtonStandard("Death Plateau", new Position(2867, 3594, 0)),
            new TeleportButtonStandard("Brine Rat Cavern", new Position(2722, 10133, 0)),
            new TeleportButtonStandard("Desert Bandits", new Position(3176, 2987, 0))
            ));

    private static final TeleportContainer SKILLING = new TeleportContainer(Lists.newArrayList(
            new TeleportButtonStandard("Slayer Masters", new Position(3083, 3499, 0)),
            new TeleportButtonStandard("Mount Karuulm (Slayer)", new Position(1311, 3795, 0)),
            new TeleportButtonStandard("Skilling Island", new Position(3803, 3538, 0)),
            new TeleportButtonStandard("Agility", new Position(3091, 3474, 0)),
            new TeleportButtonStandard("Hunter", new Position(3560, 4010, 0)),
            new TeleportButtonStandard("Farming", new Position(3053, 3301, 0)),
            new TeleportButtonStandard("Puro-Puro", new Position(2594, 4320, 0)),
            new TeleportButtonStandard("ZMI Altar", new Position(2453, 3231, 0))
            ));

    private static final TeleportContainer CITIES = new TeleportContainer(Lists.newArrayList(
            new TeleportButtonStandard("Varrock", new Position(3210, 3424, 0)),
            new TeleportButtonStandard("Yanille", new Position(2606, 3093, 0)),
            new TeleportButtonStandard("Edgeville", new Position(3093, 3493, 0)),
            new TeleportButton("Lumbridge", plr -> {
                plr.getDiaryManager().getLumbridgeDraynorDiary().progress(LumbridgeDraynorDiaryEntry.LUMBRIDGE_TELEPORT);
                plr.getPA().startTeleport(3222, 3218, 0, "modern", false);
            }),
            new TeleportButton("Ardougne", plr -> {
                plr.getPA().startTeleport(2662, 3305, 0, "modern", false);
                plr.getDiaryManager().getArdougneDiary().progress(ArdougneDiaryEntry.TELEPORT_ARDOUGNE);
            }),

            new TeleportButtonStandard("Neitiznot", new Position(2321, 3804, 0)),
            new TeleportButtonStandard("Karamja", new Position(2948, 3147, 0)),
            new TeleportButton("Falador", plr -> {
                plr.getPA().startTeleport(2964, 3378, 0, "modern", false);
                plr.getDiaryManager().getFaladorDiary().progress(FaladorDiaryEntry.TELEPORT_TO_FALADOR);
            }),
            new TeleportButtonStandard("Taverley", new Position(2928, 3451, 0)),
            new TeleportButton("Camelot", plr -> {
                plr.getPA().startTeleport(2757, 3478, 0, "modern", false);
                plr.getDiaryManager().getKandarinDiary().progress(KandarinDiaryEntry.CAMELOT_TELEPORT);
            }),
            new TeleportButtonStandard("Catherby", new Position(2804, 3432, 0)),
            new TeleportButtonStandard("Al Kharid", new Position(3293, 3179, 0)),
            new TeleportButtonStandard("Draynor", new Position(3105, 3249, 0)),
            new TeleportButtonStandard("Kebos Lowlands", new Position(1310, 3618, 0))
    ));

    private static final TeleportContainer WILDERNESS = new TeleportContainer(Lists.newArrayList(

            new TeleportButtonStandard("Mage Bank @yel@(Safe)", new Position(2539, 4716, 0)),
            new TeleportButtonStandard("West Dragons @red@(10)", new Position(2976, 3591, 0), true),
            new TeleportButtonStandard("Dark Castle @red@(15)", new Position(3020, 3632, 0), true),
            new TeleportButtonStandard("Elder Chaos Druids @red@(16)", new Position(3232, 3642, 0), true),
            new TeleportButtonStandard("Hill Giants (Multi) @red@(18)", new Position(3304, 3657, 0), true),
            new TeleportButtonStandard("Black Chinchompa @red@(32)", new Position(3137, 3767, 0), true),
            new TeleportButtonStandard("Revenant Cave @red@(40)", new Position(3127, 3835, 0), true),
            new TeleportButtonStandard("Lava Maze @red@(43)", new Position(3025, 3836, 0), true),
            new TeleportButtonStandard("Wildy God Wars Dungeon @red@(28)", new Position(3021, 3738, 0), true),
             new TeleportButtonStandard("Wilderness Agility Course @red@(52)", new Position(3003, 3934, 0), true)

    ));

    private static final TeleportContainer BOSSES = new TeleportContainer(Lists.newArrayList(
            new TeleportButtonStandard("Barrelchest", new Position(2903, 3612, 0)),
            new TeleportButtonStandard("Bryophyta", new Position(3174, 9898, 0)),
            new TeleportButtonStandard("Obor", new Position(3097, 9833, 0)),
            new TeleportButtonStandard("Dagannoth Kings", new Position(1913, 4367, 0)),
            new TeleportButtonStandard("Giant Mole", new Position(2993, 3376, 0)),
            new TeleportButtonStandard("Kalphite Queen", new Position(3510, 9496, 2)),
            new TeleportButtonStandard("Lizardman Shaman", new Position(1558, 3696, 0)),
            new TeleportButtonStandard("Sarachnis", new Position(1842, 9926, 0)),
            new TeleportButtonStandard("Grotesque Guardians", new Position(3428, 3541, 2)),
            new TeleportButtonStandard("Thermonuclear Smoke Devil", new Position(2404, 9415, 0)),
            new TeleportButtonStandard("Kraken", new Position(2280, 10016, 0)),
            new TeleportButtonStandard("Demonic Gorillas", new Position(2124, 5660, 0)),
            new TeleportButton("Godwars", plr -> plr.getDH().sendDialogues(4487, -1)),
            new TeleportButtonStandard("Corporeal Beast", new Position(2964, 4382, 2)),
            new TeleportButtonStandard("Zulrah", new Position(2203, 3056, 0)),
            new TeleportButtonStandard("Cerberus", new Position(1310, 1248, 0)),
            new TeleportButtonStandard("Abyssal Sire", new Position(3038, 4767, 0)),
            new TeleportButtonStandard("Vorkath", new Position(2272, 4050, 0)),
            new TeleportButtonStandard("Alchemical hydra", new Position(1354, 10259, 0)),
            new TeleportButtonStandard("The Nightmare", new Position(3808, 9755, 1)),
            new TeleportButtonStandard("King Black Dragon @red@(42 Wild)", new Position(3005, 3849, 0), true),
            new TeleportButtonStandard("Vet'ion @red@(40 Wild)", new Position(3200, 3794, 0), true),
            new TeleportButtonStandard("Callisto @red@(43 Wild)", new Position(3325, 3845, 0), true),
            new TeleportButtonStandard("Scorpia @red@(54 Wild)", new Position(3233, 3945, 0), true),
            new TeleportButtonStandard("Venenatis @red@(28 Wild)", new Position(3345, 3754, 0), true),
            new TeleportButtonStandard("Chaos Elemental @red@(50 Wild)", new Position(3285, 3925, 0), true),
            new TeleportButtonStandard("Chaos Fanatic @red@(41 Wild)", new Position(2978, 3833, 0), true),
            new TeleportButtonStandard("Crazy Archaeologist @red@(23 Wild)", new Position(2984, 3713, 0), true)
    ));

    private static final TeleportContainer MINIGAMES = new TeleportContainer(Lists.newArrayList(
            new TeleportButtonStandard("Theatre of Blood", new Position(3671, 3219, 0)),
            new TeleportButtonStandard("Chambers of Xeric", new Position(3033, 6067, 0)),
            new TeleportButtonStandard("Fight Caves", new Position(2444, 5179, 0)),
            new TeleportButtonStandard("The Inferno", new Position(2437, 5126, 0)),
            new TeleportButtonStandard("Barrows", new Position(3565, 3316, 0)),
            new TeleportButtonStandard("Duel Arena", new Position(3366, 3266, 0)),
            new TeleportButtonStandard("Warriors Guild", new Position(2874, 3546, 0)),
            new TeleportButton("Pest Control", plr -> {
                plr.getPA().startTeleport(2660, 2648, 0, "modern", false);
                plr.getDiaryManager().getWesternDiary().progress(WesternDiaryEntry.PEST_CONTROL_TELEPORT);
            }),
            new TeleportButtonStandard("Clan Wars", new Position(3387, 3158, 0)),
            new TeleportButtonStandard("Outlast", new Position(3080, 3510, 0)),
            new TeleportButtonStandard("Mage Arena", new Position(2541, 4716, 0)),
            new TeleportButtonStandard("Horror From The Deep [Quest]", new Position(2508, 3641, 0)),
            new TeleportButtonStandard("Monkey Madness [Quest]", new Position(3100, 3471, 0))
    ));
    private static final TeleportContainer DONATOR = new TeleportContainer(Lists.newArrayList(

            new TeleportButton("@cr4@Donator Zone (::dz)", plr -> {
                if (plr.getRights().isOrInherits(Right.REGULAR_DONATOR) || plr.getRights().hasStaffPosition()) {
                        plr.getPA().startTeleport(3809, 2844, 0, "modern", false);
                } else {
                    plr.sendMessage("You need to be a regular donator to teleport here.");
                }
            }),
            new TeleportButton("@cr8@Legendary Zone (::lz)", plr -> {
                if (plr.getRights().isOrInherits(Right.LEGENDARY_DONATOR) || plr.getRights().hasStaffPosition()) {
                        plr.getPA().startTeleport(2846, 5089, 0, "modern", false);
                } else {
                    plr.sendMessage("You need a donator status of Legendary to tele here.");
                }
            }),
            new TeleportButton("@cr17@Onyx Zone(::oz)", plr -> {
                if (plr.getRights().isOrInherits(Right.ONYX_CLUB)  || plr.getRights().hasStaffPosition()) {
                        plr.getPA().startTeleport(Configuration.ONYX_ZONE_TELEPORT, "modern", false);
                } else {
                    plr.sendMessage("You need a donator status of Onyx to tele here.");
                }
            })
    ));

    public static final List<TeleportContainer> TELEPORT_CONTAINER_LIST = Collections.unmodifiableList(Lists.newArrayList(
        MONSTERS, BOSSES, WILDERNESS, SKILLING, MINIGAMES, CITIES, DONATOR
    ));

}
