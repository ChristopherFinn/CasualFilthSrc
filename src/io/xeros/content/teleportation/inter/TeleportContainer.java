package io.xeros.content.teleportation.inter;

import java.util.Collections;
import java.util.List;

public class TeleportContainer {

    private final List<TeleportButton> buttonList;

    public TeleportContainer(List<TeleportButton> buttonList) {
        this.buttonList = Collections.unmodifiableList(buttonList);
    }

    public List<TeleportButton> getButtonList() {
        return buttonList;
    }
}
