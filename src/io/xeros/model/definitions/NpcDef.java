package io.xeros.model.definitions;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import io.xeros.Server;

public class NpcDef {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(NpcDef.class);
    private static final Map<Integer, NpcDef> definitions = new HashMap<>();
    private static final NpcDef DEFAULT = builder().build();

    public static void load() throws IOException {
        try (FileReader fr = new FileReader(new File(Server.getDataDirectory() + "/cfg/npc/npc_definitions.json"))) {
            Map<Integer, NpcDef> map = new Gson().fromJson(fr, new TypeToken<Map<Integer, NpcDef>>() {
            }.getType());
            map.entrySet().forEach(entry -> {
                definitions.put(entry.getKey(), entry.getValue());
            });
        }
        log.info("Loaded " + definitions.size() + " npc definitions.");
    }

    public static NpcDef forId(int npcId) {
        return definitions.getOrDefault(npcId, DEFAULT);
    }

    public static Map<Integer, NpcDef> getDefinitions() {
        return Collections.unmodifiableMap(definitions);
    }

    private final String name;
    private final int combatLevel;
    private final int size;
    private final boolean runnable;

    public String getName() {
        if (name != null) {
            if (name.contains("@red@")) {
                return name.replace("@red@", "");
            }
            return name;
        }
        return "unknown";
    }

    NpcDef(final String name, final int combatLevel, final int size, final boolean runnable) {
        this.name = name;
        this.combatLevel = combatLevel;
        this.size = size;
        this.runnable = runnable;
    }

    public static class NpcDefBuilder {

        private String name;

        private int combatLevel;

        private int size;

        private boolean runnable;

        NpcDefBuilder() {
        }

        public NpcDef.NpcDefBuilder name(final String name) {
            this.name = name;
            return this;
        }

        public NpcDef.NpcDefBuilder combatLevel(final int combatLevel) {
            this.combatLevel = combatLevel;
            return this;
        }

        public NpcDef.NpcDefBuilder size(final int size) {
            this.size = size;
            return this;
        }

        public NpcDef.NpcDefBuilder runnable(final boolean runnable) {
            this.runnable = runnable;
            return this;
        }

        public NpcDef build() {
            return new NpcDef(this.name, this.combatLevel, this.size, this.runnable);
        }

        @Override
    public String toString() {
            return "NpcDef.NpcDefBuilder(name=" + this.name + ", combatLevel=" + this.combatLevel + ", size=" + this.size + ", runnable=" + this.runnable + ")";
        }
    }

    public static NpcDef.NpcDefBuilder builder() {
        return new NpcDef.NpcDefBuilder();
    }

    public int getCombatLevel() {
        return this.combatLevel;
    }

    public int getSize() {
        return this.size;
    }

    public boolean isRunnable() {
        return this.runnable;
    }

    @Override
    public boolean equals(final Object o) {
        if (o == this) return true;
        if (!(o instanceof NpcDef)) return false;
        final NpcDef other = (NpcDef) o;
        if (!other.canEqual((Object) this)) return false;
        if (this.getCombatLevel() != other.getCombatLevel()) return false;
        if (this.getSize() != other.getSize()) return false;
        if (this.isRunnable() != other.isRunnable()) return false;
        final Object this$name = this.getName();
        final Object other$name = other.getName();
        if (this$name == null ? other$name != null : !this$name.equals(other$name)) return false;
        return true;
    }

    protected boolean canEqual(final Object other) {
        return other instanceof NpcDef;
    }

    @Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        result = result * PRIME + this.getCombatLevel();
        result = result * PRIME + this.getSize();
        result = result * PRIME + (this.isRunnable() ? 79 : 97);
        final Object $name = this.getName();
        result = result * PRIME + ($name == null ? 43 : $name.hashCode());
        return result;
    }

    @Override
    public String toString() {
        return "NpcDef(name=" + this.getName() + ", combatLevel=" + this.getCombatLevel() + ", size=" + this.getSize() + ", runnable=" + this.isRunnable() + ")";
    }
}
