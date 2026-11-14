package ru.phrases;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.CameraRenderState;

/** A tiny self-contained bitmap font used for phrase labels. */
public final class CustomPhraseRenderer {
    private static final RenderType GLYPH_LAYER = RenderTypes.debugFilledBox();
    private static final float PIXEL = 1.0F;
    private static final float GAP = 1.0F;

    private CustomPhraseRenderer() {}

    public static void submit(String phrase, AvatarRenderState state, PoseStack poseStack,
                              SubmitNodeCollector queue, CameraRenderState cameraState) {
        if (phrase == null || phrase.isBlank() || state.nameTagAttachment == null) return;

        poseStack.pushPose();
        poseStack.translate(
                state.nameTagAttachment.x,
                state.nameTagAttachment.y + 0.34F,
                state.nameTagAttachment.z
        );
        poseStack.mulPose(cameraState.orientation);
        poseStack.scale(-0.025F, -0.025F, 0.025F);

        queue.submitCustomGeometry(poseStack, GLYPH_LAYER,
                (pose, vertices) -> draw(phrase, pose, vertices));
        poseStack.popPose();
    }

    private static void draw(String phrase, PoseStack.Pose pose, VertexConsumer vertices) {
        String text = phrase.toUpperCase(java.util.Locale.ROOT);
        float width = textWidth(text);
        float x = -width / 2.0F;

        // Black offset gives the custom glyphs a readable outline in the world.
        drawText(text, x + 0.8F, 0.8F, 0xFF202020, pose, vertices);
        drawText(text, x, 0.0F, 0xFFFFFFFF, pose, vertices);
    }

    private static void drawText(String text, float x, float y, int color,
                                 PoseStack.Pose pose, VertexConsumer vertices) {
        for (int i = 0; i < text.length(); i++) {
            char character = text.charAt(i);
            int[] rows = glyph(character);
            for (int row = 0; row < 7; row++) {
                int bits = rows[row];
                for (int column = 0; column < 5; column++) {
                    if ((bits & (1 << (4 - column))) != 0) {
                        quad(vertices, pose, x + column * PIXEL, y + row * PIXEL, color);
                    }
                }
            }
            x += 5.0F * PIXEL + GAP;
        }
    }

    private static float textWidth(String text) {
        return text.isEmpty() ? 0.0F : text.length() * 6.0F - GAP;
    }

    private static void quad(VertexConsumer v, PoseStack.Pose pose, float x, float y, int color) {
        v.addVertex(pose, x, y, 0).setColor(color);
        v.addVertex(pose, x + PIXEL, y, 0).setColor(color);
        v.addVertex(pose, x + PIXEL, y + PIXEL, 0).setColor(color);
        v.addVertex(pose, x, y + PIXEL, 0).setColor(color);
    }

    private static int[] glyph(char c) {
        return switch (c) {
            case 'А' -> new int[]{14, 17, 17, 31, 17, 17, 17};
            case 'Б' -> new int[]{31, 16, 16, 30, 17, 17, 30};
            case 'В' -> new int[]{30, 17, 17, 30, 17, 17, 30};
            case 'Г' -> new int[]{31, 16, 16, 16, 16, 16, 16};
            case 'Д' -> new int[]{14, 17, 17, 17, 17, 17, 31};
            case 'Е' -> new int[]{31, 16, 16, 30, 16, 16, 31};
            case 'Ж' -> new int[]{21, 21, 14, 4, 14, 21, 21};
            case 'З' -> new int[]{30, 1, 1, 14, 1, 1, 30};
            case 'И' -> new int[]{17, 19, 21, 25, 17, 17, 17};
            case 'Й' -> new int[]{10, 0, 17, 19, 21, 25, 17};
            case 'К' -> new int[]{17, 18, 20, 24, 20, 18, 17};
            case 'Л' -> new int[]{7, 9, 9, 9, 9, 17, 17};
            case 'М' -> new int[]{17, 27, 21, 21, 17, 17, 17};
            case 'Н' -> new int[]{17, 17, 17, 31, 17, 17, 17};
            case 'О' -> new int[]{14, 17, 17, 17, 17, 17, 14};
            case 'П' -> new int[]{31, 17, 17, 17, 17, 17, 17};
            case 'Р' -> new int[]{30, 17, 17, 30, 16, 16, 16};
            case 'С' -> new int[]{15, 16, 16, 16, 16, 16, 15};
            case 'Т' -> new int[]{31, 4, 4, 4, 4, 4, 4};
            case 'У' -> new int[]{17, 17, 17, 15, 1, 1, 30};
            case 'Ф' -> new int[]{4, 14, 21, 21, 14, 4, 4};
            case 'Х' -> new int[]{17, 17, 10, 4, 10, 17, 17};
            case 'Ц' -> new int[]{18, 18, 18, 18, 18, 18, 31};
            case 'Ч' -> new int[]{17, 17, 17, 15, 1, 1, 1};
            case 'Ш' -> new int[]{21, 21, 21, 21, 21, 21, 31};
            case 'Щ' -> new int[]{21, 21, 21, 21, 21, 21, 31};
            case 'Ь' -> new int[]{16, 16, 16, 30, 17, 17, 30};
            case 'Ы' -> new int[]{17, 17, 17, 29, 21, 21, 29};
            case 'Э' -> new int[]{30, 1, 1, 15, 1, 1, 30};
            case 'Ю' -> new int[]{26, 21, 21, 29, 21, 21, 26};
            case 'Я' -> new int[]{15, 17, 17, 15, 21, 18, 17};
            default -> new int[]{0, 0, 0, 0, 0, 0, 0};
        };
    }
}
