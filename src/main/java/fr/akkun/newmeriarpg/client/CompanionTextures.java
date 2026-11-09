package fr.akkun.newmeriarpg.client;

import fr.akkun.newmeriarpg.companion.CompanionAttachments;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;

import java.util.Map;
import java.util.Optional;
import java.util.WeakHashMap;

/**
 * Shared by the per-form companion rendering mixins (one pair of {@code extractRenderState}/{@code
 * getTextureLocation} injects per vanilla renderer). Rather than adding a field to each of the 5
 * different render-state classes, the resolved texture is remembered here, keyed by the render
 * state instance itself (weakly, so entries are naturally cleaned up once a render state is no
 * longer referenced elsewhere).
 */
public final class CompanionTextures {
    private static final Map<Object, Identifier> BY_RENDER_STATE = new WeakHashMap<>();

    private CompanionTextures() {
    }

    /** Called from each form's {@code extractRenderState} mixin, which has entity access. */
    public static void capture(Entity entity, Object renderState) {
        Optional<Identifier> texture = entity.getExistingData(CompanionAttachments.COMPANION_DATA)
                .map(data -> data.form().texture());
        if (texture.isPresent()) {
            BY_RENDER_STATE.put(renderState, texture.get());
        } else {
            BY_RENDER_STATE.remove(renderState);
        }
    }

    /** Called from each form's {@code getTextureLocation} mixin, which only has the render state. */
    public static Identifier resolve(Object renderState, Identifier vanillaTexture) {
        return BY_RENDER_STATE.getOrDefault(renderState, vanillaTexture);
    }
}
