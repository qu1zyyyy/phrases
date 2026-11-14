package ru.phrases;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.resources.Identifier;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/** Custom textured renderer backed by the supplied SF Pro TrueType font. */
public final class SfProPhraseRenderer {
    private static final Identifier TEXTURE_ID = Identifier.fromNamespaceAndPath(
            PhrasesClient.MOD_ID, "sf_pro_phrase_atlas"
    );
    private static final int ATLAS_WIDTH = 512;
    private static final int ATLAS_HEIGHT = 384;
    private static final int CELL_WIDTH = 64;
    private static final int CELL_HEIGHT = 96;
    private static final int FONT_SIZE = 72;
    private static final int COLUMNS = ATLAS_WIDTH / CELL_WIDTH;
    private static final String GLYPHS = "АБВГДЕЖЗИЙКЛМНОПРСТУФХЦЧШЩЬЫЭЮЯ ";
    private static final Map<Character, Glyph> FONT = new HashMap<>();
    private static RenderType renderType;
    private static boolean initialized;

    private SfProPhraseRenderer() {}

    public static void submit(String phrase, AvatarRenderState state, PoseStack poseStack,
                              SubmitNodeCollector queue, CameraRenderState cameraState) {
        if (phrase == null || phrase.isBlank() || state.nameTagAttachment == null) return;
        ensureFont();
        if (renderType == null) return;

        String text = phrase.toUpperCase(Locale.ROOT);
        float width = textWidth(text);
        poseStack.pushPose();
        poseStack.translate(
                state.nameTagAttachment.x,
                state.nameTagAttachment.y + 0.34F,
                state.nameTagAttachment.z
        );
        poseStack.mulPose(cameraState.orientation);
        poseStack.scale(-0.025F, -0.025F, 0.025F);
        queue.submitCustomGeometry(poseStack, renderType,
                (pose, vertices) -> draw(text, width, pose, vertices));
        poseStack.popPose();
    }

    public static void drawGuiText(GuiGraphics graphics, String text, int centerX, int topY,
                                   int maxWidth, int color) {
        ensureFont();
        if (renderType == null || text == null || text.isBlank()) return;
        String value = text.toUpperCase(Locale.ROOT);
        float scale = 0.34F;
        float width = textWidth(value) * scale;
        if (width > maxWidth) scale *= maxWidth / width;
        float x = centerX - textWidth(value) * scale / 2.0F;
        for (int i = 0; i < value.length(); i++) {
            Glyph glyph = FONT.getOrDefault(value.charAt(i), FONT.get(' '));
            int drawX = Math.round(x);
            int drawY = Math.round(topY);
            int drawWidth = Math.max(1, Math.round(CELL_WIDTH * scale));
            int drawHeight = Math.max(1, Math.round(CELL_HEIGHT * scale));
            graphics.blit(TEXTURE_ID, drawX, drawY, drawWidth, drawHeight,
                    glyph.u0, glyph.v0, glyph.u1 - glyph.u0, glyph.v1 - glyph.v0);
            x += glyph.advance * scale;
        }
    }

    private static void ensureFont() {
        if (initialized) return;
        initialized = true;
        try (InputStream stream = SfProPhraseRenderer.class.getResourceAsStream(
                "/assets/phrases/fonts/SF-Pro.ttf")) {
            if (stream == null) return;
            java.awt.Font font = java.awt.Font.createFont(java.awt.Font.TRUETYPE_FONT, stream)
                    .deriveFont(java.awt.Font.PLAIN, (float) FONT_SIZE);
            BufferedImage atlas = new BufferedImage(ATLAS_WIDTH, ATLAS_HEIGHT, BufferedImage.TYPE_INT_ARGB);
            Graphics2D graphics = atlas.createGraphics();
            graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            graphics.setFont(font);
            graphics.setColor(Color.WHITE);
            for (int i = 0; i < GLYPHS.length(); i++) {
                char character = GLYPHS.charAt(i);
                int cellX = (i % COLUMNS) * CELL_WIDTH;
                int cellY = (i / COLUMNS) * CELL_HEIGHT;
                int advance = graphics.getFontMetrics().charWidth(character);
                graphics.drawString(String.valueOf(character), cellX + 4, cellY + FONT_SIZE - 10);
                FONT.put(character, new Glyph(
                        cellX / (float) ATLAS_WIDTH,
                        cellY / (float) ATLAS_HEIGHT,
                        (cellX + CELL_WIDTH) / (float) ATLAS_WIDTH,
                        (cellY + CELL_HEIGHT) / (float) ATLAS_HEIGHT,
                        Math.max(advance, 12),
                        cellX,
                        cellY
                ));
            }
            graphics.dispose();

            NativeImage image = new NativeImage(ATLAS_WIDTH, ATLAS_HEIGHT, false);
            for (int y = 0; y < ATLAS_HEIGHT; y++) {
                for (int x = 0; x < ATLAS_WIDTH; x++) {
                    int argb = atlas.getRGB(x, y);
                    int abgr = ((argb & 0xFF) << 24)
                            | ((argb & 0xFF00) << 8)
                            | ((argb >> 8) & 0xFF00)
                            | ((argb >> 24) & 0xFF);
                    image.setPixelABGR(x, y, abgr);
                }
            }
            Minecraft.getInstance().getTextureManager().register(TEXTURE_ID,
                    new DynamicTexture(() -> "Phrases SF Pro", image));
            renderType = RenderTypes.text(TEXTURE_ID);
        } catch (Exception error) {
            System.err.println("[Phrases] Could not load SF Pro font: " + error.getMessage());
        }
    }

    private static float textWidth(String text) {
        float width = 0.0F;
        for (int i = 0; i < text.length(); i++) {
            Glyph glyph = FONT.getOrDefault(text.charAt(i), FONT.get(' '));
            width += glyph.advance;
        }
        return width;
    }

    private static void draw(String text, float width, PoseStack.Pose pose, VertexConsumer vertices) {
        float x = -width / 2.0F;
        for (int i = 0; i < text.length(); i++) {
            Glyph glyph = FONT.getOrDefault(text.charAt(i), FONT.get(' '));
            float glyphWidth = glyph.advance;
            texturedQuad(vertices, pose, x + 0.8F, 0.8F, glyphWidth, CELL_HEIGHT,
                    glyph, 0xFF202020);
            texturedQuad(vertices, pose, x, 0.0F, glyphWidth, CELL_HEIGHT,
                    glyph, 0xFFFFFFFF);
            x += glyph.advance;
        }
    }

    private static void texturedQuad(VertexConsumer vertices, PoseStack.Pose pose,
                                     float x, float y, float width, float height,
                                     Glyph glyph, int color) {
        vertices.addVertex(pose, x, y, 0).setColor(color).setUv(glyph.u0, glyph.v0).setUv2(15728880, 15728880);
        vertices.addVertex(pose, x + width, y, 0).setColor(color).setUv(glyph.u1, glyph.v0).setUv2(15728880, 15728880);
        vertices.addVertex(pose, x + width, y + height, 0).setColor(color).setUv(glyph.u1, glyph.v1).setUv2(15728880, 15728880);
        vertices.addVertex(pose, x, y + height, 0).setColor(color).setUv(glyph.u0, glyph.v1).setUv2(15728880, 15728880);
    }

    private record Glyph(float u0, float v0, float u1, float v1, float advance, int cellX, int cellY) {}
}
