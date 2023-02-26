package io.xeros.content.commands;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;
import com.google.common.collect.Lists;
import io.xeros.Server;
import io.xeros.content.tournaments.TourneyManager;
import io.xeros.model.entity.player.Player;
import io.xeros.model.entity.player.Right;
import io.xeros.util.Misc;
import org.reflections.Reflections;

public class CommandManager {

    private static final java.util.logging.Logger log = java.util.logging.Logger.getLogger(CommandManager.class.getName());
    public static final Map<String, Command> COMMAND_MAP = new TreeMap<>();
    public static final List<CommandPackage> COMMAND_PACKAGES = Lists.newArrayList(new CommandPackage("admin", Right.ADMINISTRATOR), new CommandPackage("owner", Right.OWNER), new CommandPackage("moderator", Right.MODERATOR), new CommandPackage("helper", Right.HELPER), new CommandPackage("donator", Right.REGULAR_DONATOR), new CommandPackage("all", Right.PLAYER));

    private static boolean hasRightsRequirement(Player c, Right rightsRequired) {
        if (rightsRequired == Right.REGULAR_DONATOR && c.getRights().hasStaffPosition()) {
            return true;
        }
        return c.getRights().isOrInherits(rightsRequired);
    }

    public static void execute(Player c, String playerCommand) {
        for (CommandPackage commandPackage : COMMAND_PACKAGES) {
            if (hasRightsRequirement(c, commandPackage.getRight()) && executeCommand(c, playerCommand, commandPackage.getPackagePath())) {
                return;
            }
        }
    }

    public static CommandPackage getPackage(Command command) {
        for (CommandPackage commandPackage : COMMAND_PACKAGES) {
            if (command.getClass().getPackageName().contains(commandPackage.getPackagePath())) {
                return commandPackage;
            }
        }
        return null;
    }

    private static String getPackageName(String packagePath) {
        String[] split = packagePath.split("\\.");
        return split[split.length - 2];
    }

    public static List<Command> getCommands(Player player, String... skips) {
        return COMMAND_MAP.entrySet().stream().filter(entry -> {
            for (CommandPackage commandPackage : COMMAND_PACKAGES) {
                if (getPackageName(entry.getKey().toLowerCase()).contains(commandPackage.getPackagePath())) {
                    if (Arrays.stream(skips).anyMatch(skip -> commandPackage.getPackagePath().toLowerCase().contains(skip))) {
                        continue;
                    }
                    if (hasRightsRequirement(player, commandPackage.getRight())) {
                        return true;
                    }
                }
            }
            return false;
        }).map(Map.Entry::getValue).collect(Collectors.toList());
    }

    public static boolean executeCommand(Player c, String playerCommand, String commandPackage) {
        if (playerCommand == null) {
            return true;
        }
        String commandName = Misc.findCommand(playerCommand);
        String commandInput = Misc.findInput(playerCommand);
        String className;
        if (commandName.length() <= 0) {
            return true;
        } else if (commandName.length() == 1) {
            className = commandName.toUpperCase();
        } else {
            className = Character.toUpperCase(commandName.charAt(0)) + commandName.substring(1).toLowerCase();
        }

        boolean outlast = TourneyManager.getSingleton().isInArenaBounds(c) || TourneyManager.getSingleton().isInLobbyBounds(c);

        if (outlast && c.getRights().isNot(Right.ADMINISTRATOR)) {
            c.sendMessage("You cannot use commands when in the tournament arena");
            return true;
        }
        try {
            String path = "io.xeros.content.commands." + commandPackage + "." + className;
            if (COMMAND_MAP.get(path.toLowerCase()) != null) {
                COMMAND_MAP.get(path.toLowerCase()).execute(c, commandName, commandInput);
                return true;
            }
            return false;
        } catch (Exception e) {
            c.sendMessage("Error while executing the following command: " + playerCommand);
            e.printStackTrace();
            return true;
        }
    }

    private static void initialize(String path) throws InstantiationException, IllegalAccessException, ClassNotFoundException {
        Class<?> commandClass = Class.forName(path);
        Object instance = commandClass.newInstance();
        if (instance instanceof Command) {
            Command command = (Command) instance;
            COMMAND_MAP.putIfAbsent(path.toLowerCase(), command);
            log.fine(String.format("Added command [path=%s] [command=%s]", path, command.toString()));
        }
    }

    public static void initializeCommands() throws IllegalAccessException, ClassNotFoundException, InstantiationException {
        if (Server.isDebug() || Server.isTest()) { // Important that this doesn't get removed
            COMMAND_PACKAGES.add(new CommandPackage("test", Right.PLAYER));
        }
        Reflections reflections = new Reflections("io.xeros.content.commands");
        for (Class<? extends Command> clazz : reflections.getSubTypesOf(Command.class)) {
            initialize(clazz.getName());
        }
        log.info("Loaded " + COMMAND_MAP.size() + " commands.");
    }
}
