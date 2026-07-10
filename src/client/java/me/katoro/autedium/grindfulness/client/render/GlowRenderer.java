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
		long expiry = client.level.getGameTime() + durationTicks;
		for (BlockPos pos : positions) {
			PINGS.add(new Ping(pos.immutable(), argb, expiry));
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
				emitBox(pose, buffer, ping.pos(), ping.argb());
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

	private static void quad(PoseStack.Pose pose, VertexConsumer buffer, int color,
			float ax, float ay, float az, float bx, float by, float bz,
			float cx, float cy, float cz, float dx, float dy, float dz) {
		buffer.addVertex(pose, ax, ay, az).setColor(color);
		buffer.addVertex(pose, bx, by, bz).setColor(color);
		buffer.addVertex(pose, cx, cy, cz).setColor(color);
		buffer.addVertex(pose, dx, dy, dz).setColor(color);
	}
}
