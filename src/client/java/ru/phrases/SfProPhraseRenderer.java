package ru.phrases;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.GsonHelper;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/** Custom MSDF renderer. The TTF is only used offline to generate the atlas. */
public final class SfProPhraseRenderer {
    private static final Identifier ATLAS_ID = Identifier.fromNamespaceAndPath(PhrasesClient.MOD_ID, "textures/font/sf_pro.png");
    private static final Identifier SHADER_ID = Identifier.fromNamespaceAndPath(PhrasesClient.MOD_ID, "core/msdf_font");
    private static final RenderPipeline PIPELINE = RenderPipeline.builder(RenderPipelines.MATRICES_PROJECTION_SNIPPET)
            .withLocation(Identifier.fromNamespaceAndPath(PhrasesClient.MOD_ID, "pipeline/msdf_font"))
            .withVertexShader(SHADER_ID)
            .withFragmentShader(SHADER_ID)
            .withSampler("Sampler0")
            .withVertexFormat(DefaultVertexFormat.POSITION_TEX_COLOR, VertexFormat.Mode.QUADS)
            .withBlend(BlendFunction.TRANSLUCENT)
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            .withCull(false)
            .build();

    private static final Map<Character, Glyph> GLYPHS = new HashMap<>();
    private static RenderType renderType;
    private static int atlasWidth = 512;
    private static int atlasHeight = 512;
    private static int baseline = 61;
    private static boolean initialized;

    private SfProPhraseRenderer() {}

    public static void submit(String phrase, AvatarRenderState state, PoseStack poseStack,
                              SubmitNodeCollector queue, CameraRenderState cameraState) {
        if (phrase == null || phrase.isBlank() || state.nameTagAttachment == null) return;
        ensureLoaded();
        if (renderType == null) return;

        String text = phrase.toUpperCase(Locale.ROOT);
        float scale = 0.0032F;
        float width = textWidth(text);
        poseStack.pushPose();
        poseStack.translate(state.nameTagAttachment.x, state.nameTagAttachment.y + 0.30F, state.nameTagAttachment.z);
        poseStack.mulPose(cameraState.orientation);
        poseStack.scale(-scale, -scale, scale);
        queue.submitCustomGeometry(poseStack, renderType, (pose, vertices) -> drawWorld(text, width, pose, vertices));
        poseStack.popPose();
    }

    public static void drawGuiText(GuiGraphics graphics, String text, int centerX, int topY,
                                   int maxWidth, int color) {
        ensureLoaded();
        if (renderType == null || text == null || text.isBlank()) return;
        String value = text.toUpperCase(Locale.ROOT);
        float scale = 0.34F;
        float width = textWidth(value) * scale;
        if (width > maxWidth) scale *= maxWidth / width;
        float x = centerX - textWidth(value) * scale / 2.0F;
        for (int i = 0; i < value.length(); i++) {
            Glyph glyph = glyph(value.charAt(i));
            int drawX = Math.round(x + glyph.xOffset * scale);
            int drawY = Math.round(topY + glyph.yOffset * scale);
            int drawWidth = Math.max(1, Math.round(glyph.width * scale));
            int drawHeight = Math.max(1, Math.round(glyph.height * scale));
            graphics.blit(PIPELINE, ATLAS_ID, drawX, drawY, glyph.x, glyph.y,
                    drawWidth, drawHeight, atlasWidth, atlasHeight);
            x += glyph.xAdvance * scale;
        }
    }

    private static void ensureLoaded() {
        if (initialized) return;
        initialized = true;
        try (InputStream metadataStream = SfProPhraseRenderer.class.getResourceAsStream("/assets/phrases/msdf/sf_pro.json")) {
            if (metadataStream == null) return;
            JsonObject root = GsonHelper.parse(new InputStreamReader(metadataStream, StandardCharsets.UTF_8));
            JsonObject common = root.getAsJsonObject("common");
            atlasWidth = common.get("scaleW").getAsInt();
            atlasHeight = common.get("scaleH").getAsInt();
            baseline = common.get("base").getAsInt();
            JsonArray chars = root.getAsJsonArray("chars");
            for (JsonElement element : chars) {
                JsonObject json = element.getAsJsonObject();
                String value = json.get("char").getAsString();
                if (!value.isEmpty()) {
                    GLYPHS.put(value.charAt(0), new Glyph(json.get("x").getAsInt(), json.get("y").getAsInt(),
                            json.get("width").getAsInt(), json.get("height").getAsInt(),
                            json.get("xoffset").getAsInt(), json.get("yoffset").getAsInt(),
                            json.get("xadvance").getAsInt()));
                }
            }
        } catch (Exception error) {
            System.err.println("[Phrases] Could not load MSDF metadata: " + error.getMessage());
            return;
        }

        try (InputStream imageStream = SfProPhraseRenderer.class.getResourceAsStream("/assets/phrases/textures/font/sf_pro.png")) {
            if (imageStream == null) return;
            NativeImage image = NativeImage.read(imageStream);
            Minecraft.getInstance().getTextureManager().register(ATLAS_ID, new DynamicTexture(() -> "Phrases SF Pro MSDF", image));
            RenderSetup setup = RenderSetup.builder(PIPELINE).withTexture("Sampler0", ATLAS_ID)
                    .bufferSize(RenderType.SMALL_BUFFER_SIZE).createRenderSetup();
            renderType = RenderType.create("phrases_msdf_font", setup);
        } catch (Exception error) {
            System.err.println("[Phrases] Could not load MSDF atlas: " + error.getMessage());
        }
    }

    private static Glyph glyph(char character) {
        return GLYPHS.getOrDefault(character, GLYPHS.get(' '));
    }

    private static float textWidth(String text) {
        float width = 0.0F;
        for (int i = 0; i < text.length(); i++) width += glyph(text.charAt(i)).xAdvance;
        return width;
    }

    private static void drawWorld(String text, float width, PoseStack.Pose pose, VertexConsumer vertices) {
        float x = -width / 2.0F;
        for (int i = 0; i < text.length(); i++) {
            Glyph glyph = glyph(text.charAt(i));
            float y = baseline - glyph.yOffset - glyph.height;
            quad(vertices, pose, x + glyph.xOffset, y, glyph.width, glyph.height, glyph);
            x += glyph.xAdvance;
        }
    }

    private static void quad(VertexConsumer v, PoseStack.Pose pose, float x, float y,
                             float width, float height, Glyph glyph) {
        float u0 = glyph.x / (float) atlasWidth;
        float v0 = glyph.y / (float) atlasHeight;
        float u1 = (glyph.x + glyph.width) / (float) atlasWidth;
        float v1 = (glyph.y + glyph.height) / (float) atlasHeight;
        v.addVertex(pose, x, y, 0).setColor(0xFFFFFFFF).setUv(u0, v0);
        v.addVertex(pose, x + width, y, 0).setColor(0xFFFFFFFF).setUv(u1, v0);
        v.addVertex(pose, x + width, y + height, 0).setColor(0xFFFFFFFF).setUv(u1, v1);
        v.addVertex(pose, x, y + height, 0).setColor(0xFFFFFFFF).setUv(u0, v1);
    }

    private record Glyph(int x, int y, int width, int height, int xOffset, int yOffset, int xAdvance) {}
}
