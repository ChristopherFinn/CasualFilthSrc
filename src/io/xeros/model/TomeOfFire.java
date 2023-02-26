package io.xeros.model;

import io.xeros.model.entity.player.Player;

/**
 * @author Arthur Behesnilian 6:47 PM
 */
public class TomeOfFire {

    /**
     * The Player who this Tome of Fire is bound to
     */
    private Player player;

    /**
     * The amount of pages in this Tome of Fire
     */
    private int pages;

    /**
     * The amount of charges in this Tome of fire
     */
    private int charges;

    public TomeOfFire(Player player) {
        this.player = player;
        this.pages = 0;
        this.charges = 0;
    }

    public boolean hasPages() {
        return pages > 0;
    }

    public void decrPage() {
        this.pages -= 1;
        if (this.pages == 0)
            player.sendMessage("You have charged your Tome of Fire with the last page available.");
    }

    public void incrPage() {
        this.pages += 1;
    }

    public void addPages(int amount) {
        this.pages += amount;
        if (this.pages == 1)
            this.pages = 0;

        this.charges = 20;
    }

    public void removePages(int amount) {
        this.pages -= amount;
    }

    public int getPages() {
        return pages;
    }

    public void setPages(int pages) {
        this.pages = pages;
    }

    public boolean hasCharges() {
        return charges > 0;
    }

    public void decrCharge() {
        if (this.charges == 0)
            return;

        if (--this.charges <= 0) {
            if (hasPages()) {
                this.charges = 20;
                this.decrPage();
            } else {
                this.charges = 0;
                player.sendMessage("Your Tome of Fire has run out of pages and charges.");
                replaceTomeOfFire();
            }
        }
    }

    public void replaceTomeOfFire() {
        int shield = player.playerEquipment[Player.playerShield];
        if (shield == Items.TOME_OF_FIRE)
            player.getItems().setEquipment(Items.TOME_OF_FIRE_EMPTY, 1, Player.playerShield, true);
    }

    public int getCharges() {
        return charges;
    }

    public void setCharges(int charges) {
        this.charges = charges;
    }

    public void reset() {
        this.pages = 0;
        this.charges = 0;
    }


}
