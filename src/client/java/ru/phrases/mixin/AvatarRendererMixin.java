package ru.phrases.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.state.CameraRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.phrases.PhrasesClient;
import ru.phrases.SfProPhraseRenderer;

@Mixin(AvatarRenderer.class)
public abstract class AvatarRendererMixin {
    @Inject(method = "submitNameTag", at = @At("HEAD"))
    private void phrases$renderPhraseNameTag(
            AvatarRenderState state,
            PoseStack poseStack,
            SubmitNodeCollector queue,
            CameraRenderState cameraState,
            CallbackInfo callbackInfo
    ) {
        if (state.nameTag == null) {
            return;
        }
        String playerName = state.nameTag.getString();
        String phrase = PhrasesClient.getActivePhrase(state.id);
        if (phrase == null) {
            phrase = PhrasesClient.getActivePhrase(playerName);
        }
        if (phrase == null) {
            return;
        }
        SfProPhraseRenderer.submit(phrase, state, poseStack, queue, cameraState);
    }
}
