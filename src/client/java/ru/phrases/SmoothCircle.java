package ru.phrases;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

final class SmoothCircle {
    private static final Identifier ATLAS = Identifier.fromNamespaceAndPath("phrases", "textures/font/sf_pro.png");
    private static final RenderPipeline PIPELINE = RenderPipeline.builder(RenderPipelines.MATRICES_PROJECTION_SNIPPET)
            .withLocation(Identifier.fromNamespaceAndPath("phrases", "pipeline/smooth_circle"))
            .withVertexShader(Identifier.fromNamespaceAndPath("phrases", "core/msdf_font"))
            .withFragmentShader(Identifier.fromNamespaceAndPath("phrases", "core/smooth_circle"))
            .withSampler("Sampler0")
            .withVertexFormat(DefaultVertexFormat.POSITION_TEX_COLOR, VertexFormat.Mode.QUADS)
            .withBlend(BlendFunction.TRANSLUCENT)
            .withCull(false)
            .build();

    static void draw(GuiGraphics graphics, int centerX, int centerY, int radius, int color) {
        int diameter = radius * 2;
        graphics.blit(PIPELINE, ATLAS, centerX - radius, centerY - radius,
                0.0F, 0.0F, diameter, diameter, 1024, 1024, 1024, 1024, color);
    }
}
