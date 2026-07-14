package me.katoro.autedium.grindfulness.client.render;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import me.katoro.autedium.grindfulness.GrindfulnessMod;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class GlowRenderer {
	private record Ping(BlockPos pos, int argb, long expiryGameTime) {}

	private static final List<Ping> PINGS = new ArrayList<>();

	private static final RenderPipeline GLOW_PIPELINE = RenderPipelines.register(
		RenderPipeline.builder(RenderPipelines.DEBUG_FILLED_SNIPPET)
			.withLocation(GrindfulnessMod.id("pipeline/glow_through_walls"))
			.withDepthStencilState(Optional.empty()) // no depth test = visible thru walls, the whole point
			.build()
	);

	private static final RenderType GLOW_TYPE = RenderType.create(
		"autedium_glow", net.minecraft.client.renderer.rendertype.RenderSetup.builder(GLOW_PIPELINE).createRenderSetup()
	);

	private GlowRenderer() {}

	public static void init() {
		LevelRenderEvents.AFTER_TRANSLUCENT_TERRAIN.register(GlowRenderer::render);
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			if (client.level != null && !PINGS.isEmpty()) {
				long now = client.level.getGameTime();
				PINGS.removeIf(p -> p.expiryGameTime() <= now);
			}
			if (client.level == null) PINGS.clear();
		});
	}

	// payload receiver calls this, already on the client thread
	public static void add(List<BlockPos> positions, int argb, int durationTicks) {
		var client = net.minecraft.client.Minecraft.getInstance();
		if (client.level == null) return;
		int clamped = Math.min(durationTicks, 1200);
		long expiry = client.level.getGameTime() + clamped;
		for (BlockPos pos : positions) {
			PINGS.add(new Ping(pos.immutable(), argb, expiry));
		}
		if (PINGS.size() > 512) {
			PINGS.subList(0, PINGS.size() - 512).clear();
		}
	}

	private static void render(LevelRenderContext context) {
		if (PINGS.isEmpty()) return;

		Vec3 camera = context.levelState().cameraRenderState.pos;
		PoseStack poses = context.poseStack();
		poses.pushPose();
		poses.translate(-camera.x, -camera.y, -camera.z);

		context.submitNodeCollector().submitCustomGeometry(poses, GLOW_TYPE, (pose, buffer) -> {
			for (Ping ping : PINGS) {
				// glowing-effect vibe: faint fill + bright edges so it reads as an outline
				emitBox(pose, buffer, ping.pos(), (ping.argb() & 0x00FFFFFF) | 0x28000000);
				emitEdges(pose, buffer, ping.pos(), (ping.argb() & 0x00FFFFFF) | 0xE6000000);
			}
		});

		poses.popPose();
	}

	private static void emitBox(PoseStack.Pose pose, VertexConsumer buffer, BlockPos p, int color) {
		float e = 0.03f;
		float x0 = p.getX() - e, y0 = p.getY() - e, z0 = p.getZ() - e;
		float x1 = p.getX() + 1 + e, y1 = p.getY() + 1 + e, z1 = p.getZ() + 1 + e;
		// 6 faces. culling is off on this pipeline so winding doesnt even matter
		quad(pose, buffer, color, x0, y0, z0, x1, y0, z0, x1, y1, z0, x0, y1, z0); // -Z
		quad(pose, buffer, color, x1, y0, z1, x0, y0, z1, x0, y1, z1, x1, y1, z1); // +Z
		quad(pose, buffer, color, x0, y0, z1, x0, y0, z0, x0, y1, z0, x0, y1, z1); // -X
		quad(pose, buffer, color, x1, y0, z0, x1, y0, z1, x1, y1, z1, x1, y1, z0); // +X
		quad(pose, buffer, color, x0, y0, z1, x1, y0, z1, x1, y0, z0, x0, y0, z0); // -Y
		quad(pose, buffer, color, x0, y1, z0, x1, y1, z0, x1, y1, z1, x0, y1, z1); // +Y
	}

	// the 12 edges as skinny boxes. real line pipelines exist but this reuses the one
	// pipeline we KNOW works thru walls, and 288 verts a block is nothing
	private static void emitEdges(PoseStack.Pose pose, VertexConsumer buffer, BlockPos p, int color) {
		float t = 0.045f; // edge thickness
		float x0 = p.getX(), y0 = p.getY(), z0 = p.getZ();
		float x1 = x0 + 1, y1 = y0 + 1, z1 = z0 + 1;
		// 4 edges along X
		bar(pose, buffer, color, x0, y0 - t, z0 - t, x1, y0 + t, z0 + t);
		bar(pose, buffer, color, x0, y0 - t, z1 - t, x1, y0 + t, z1 + t);
		bar(pose, buffer, color, x0, y1 - t, z0 - t, x1, y1 + t, z0 + t);
		bar(pose, buffer, color, x0, y1 - t, z1 - t, x1, y1 + t, z1 + t);
		// 4 along Y
		bar(pose, buffer, color, x0 - t, y0, z0 - t, x0 + t, y1, z0 + t);
		bar(pose, buffer, color, x1 - t, y0, z0 - t, x1 + t, y1, z0 + t);
		bar(pose, buffer, color, x0 - t, y0, z1 - t, x0 + t, y1, z1 + t);
		bar(pose, buffer, color, x1 - t, y0, z1 - t, x1 + t, y1, z1 + t);
		// 4 along Z
		bar(pose, buffer, color, x0 - t, y0 - t, z0, x0 + t, y0 + t, z1);
		bar(pose, buffer, color, x1 - t, y0 - t, z0, x1 + t, y0 + t, z1);
		bar(pose, buffer, color, x0 - t, y1 - t, z0, x0 + t, y1 + t, z1);
		bar(pose, buffer, color, x1 - t, y1 - t, z0, x1 + t, y1 + t, z1);
	}

	// axis-aligned solid box between two corners, 6 quads
	private static void bar(PoseStack.Pose pose, VertexConsumer buffer, int color,
			float x0, float y0, float z0, float x1, float y1, float z1) {
		quad(pose, buffer, color, x0, y0, z0, x1, y0, z0, x1, y1, z0, x0, y1, z0);
		quad(pose, buffer, color, x1, y0, z1, x0, y0, z1, x0, y1, z1, x1, y1, z1);
		quad(pose, buffer, color, x0, y0, z1, x0, y0, z0, x0, y1, z0, x0, y1, z1);
		quad(pose, buffer, color, x1, y0, z0, x1, y0, z1, x1, y1, z1, x1, y1, z0);
		quad(pose, buffer, color, x0, y0, z1, x1, y0, z1, x1, y0, z0, x0, y0, z0);
		quad(pose, buffer, color, x0, y1, z0, x1, y1, z0, x1, y1, z1, x0, y1, z1);
	}

	private static void quad(PoseStack.Pose pose, VertexConsumer buffer, int color,
			float ax, float ay, float az, float bx, float by, float bz,
			float cx, float cy, float cz, float dx, float dy, float dz) {
		buffer.addVertex(pose, ax, ay, az).setColor(color);
		buffer.addVertex(pose, bx, by, bz).setColor(color);
		buffer.addVertex(pose, cx, cy, cz).setColor(color);
		buffer.addVertex(pose, dx, dy, dz).setColor(color);
	}
}
