package io.xeros.content.commands.owner;

import io.xeros.content.commands.Command;
import io.xeros.model.definitions.ItemDef;
import io.xeros.model.entity.player.Player;

/**
 * Send the item IDs of all matching items to the player.
 * 
 * @author Emiel
 *
 */
public class Getid extends Command {

	@Override
	public void execute(Player c, String commandName, String input) {
		if (input.length() < 3) {
			c.sendMessage("You must give at least 3 letters of input to narrow down the item.");
			return;
		}
		int results = 0;
		c.sendMessage("Searching: " + input);
		for (ItemDef def : ItemDef.getDefinitions().values()) {
			if (results == 100) {
				c.sendMessage("100 results have been found, the maximum number of allowed results. If you cannot");
				c.sendMessage("find the item, try and enter more characters to refine the results.");
				return;
			}
			if (def.getName().toLowerCase().contains(input.toLowerCase())) {
				c.sendMessage("@red@" + def.getName().replace("_", " ") + " - " + def.getId());
				results++;
			}
		}
		c.sendMessage(results + " results found...");
	}
}
