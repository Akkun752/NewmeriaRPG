package fr.akkun.newmeriarpg;

import net.minecraft.network.chat.Component;

public enum RpgStat {
    FORCE("rpg.newmeriarpg.stat.force") {
        @Override
        public int level(RpgData data) {
            return data.forceLevel();
        }

        @Override
        public RpgData withLevel(RpgData data, int level) {
            return data.withForceLevel(level);
        }
    },
    RESISTANCE("rpg.newmeriarpg.stat.resistance") {
        @Override
        public int level(RpgData data) {
            return data.resistanceLevel();
        }

        @Override
        public RpgData withLevel(RpgData data, int level) {
            return data.withResistanceLevel(level);
        }
    },
    SPEED("rpg.newmeriarpg.stat.speed") {
        @Override
        public int level(RpgData data) {
            return data.speedLevel();
        }

        @Override
        public RpgData withLevel(RpgData data, int level) {
            return data.withSpeedLevel(level);
        }
    },
    MAGIC("rpg.newmeriarpg.stat.magic") {
        @Override
        public int level(RpgData data) {
            return data.magicLevel();
        }

        @Override
        public RpgData withLevel(RpgData data, int level) {
            return data.withMagicLevel(level);
        }
    };

    private final String translationKey;

    RpgStat(String translationKey) {
        this.translationKey = translationKey;
    }

    public Component displayName() {
        return Component.translatable(translationKey);
    }

    public abstract int level(RpgData data);

    public abstract RpgData withLevel(RpgData data, int level);
}
