package io.xeros.content.commands.owner;

import io.xeros.content.commands.Command;
import io.xeros.model.entity.player.Player;

/**
 * Open a specific interface.
 * 
 * @author Emiel
 *
 */
public class Gfx extends Command {

	@Override
	public void execute(Player c, String commandName, String input) {

		String[] args = input.split(" ");
		
		if (Integer.parseInt(args[0]) > 3000) {
			c.sendMessage("Max graphic id is: 3000");
			return;
		}

		if (args.length == 1) {
			c.gfx0(Integer.parseInt(args[0]));
			c.sendMessage("Performing graphic: " + Integer.parseInt(args[0]));
			c.gfxCommandId = Integer.parseInt(args[0]);
			
		} else {

			switch (args[1]) {
			case "plus":
				//c.gfx0(c.gfx);
				c.getPA().stillGfx(c.gfxCommandId, c.absX, c.absY + 1, 0, 15);
				c.sendMessage("Performing graphic: " + c.gfxCommandId);
				c.gfxCommandId += Integer.parseInt(args[2]);
				break;

			case "minus":
				//c.gfx0(c.gfx);
				c.getPA().stillGfx(c.gfxCommandId, c.absX, c.absY + 1, 0, 15);
				c.sendMessage("Performing graphic: " + c.gfxCommandId);
				c.gfxCommandId -= Integer.parseInt(args[2]);
				break;
			}
		}
	}
}
