package fr.akkun.newmeriarpg;

import com.mojang.logging.LogUtils;
import fr.akkun.newmeriacore.NewmeriaCore;
import fr.akkun.newmeriarpg.companion.CompanionAttachments;
import fr.akkun.newmeriarpg.network.RpgNetworking;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

/**
 * The RPG of Newmeria: RPG points and levels, stats, spells, the Ink Friend companions. Installed on
 * the server and on the players' computers.
 *
 * <p>The items it gives a use to (wands, totems) belong to NewmeriaCore; this mod only adds what they
 * do, through the events NewmeriaCore fires ({@code WandCastEvent}, {@code HammerExtraBlockMinedEvent}).
 */
// The value here should match an entry in the META-INF/neoforge.mods.toml file
@Mod(NewmeriaRPG.MOD_ID)
public class NewmeriaRPG {
    public static final String MOD_ID = "newmeriarpg";
    public static final Logger LOGGER = LogUtils.getLogger();

    /**
     * Namespace of the ids saved in worlds that this mod took over from NewmeriaCore: data attachments
     * (RPG progress, companions, placed blocks) and attribute modifiers. Renaming them would lose the
     * players' progress (unknown attachments are dropped on load) or stack the stat bonuses twice (old
     * modifiers would stay on the players next to the new ones).
     */
    public static final String LEGACY_NAMESPACE = NewmeriaCore.MOD_ID;

    public NewmeriaRPG(IEventBus modEventBus, ModContainer modContainer) {
        RpgAttachments.register(modEventBus);
        CompanionAttachments.register(modEventBus);
        RpgNetworking.register(modEventBus);
    }
}
