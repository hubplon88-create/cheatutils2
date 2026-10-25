package com.cheatutils.esp;

import com.cheatutils.module.ModuleManager;
import com.cheatutils.module.modules.Esp;
import com.cheatutils.module.modules.Xray;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MappableRingBuffer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.system.MemoryUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.OptionalDouble;
import java.util.OptionalInt;

/**
 * Rysuje ESP (wypelnione boxy + tracery) i Xray (podswietlone rudy) PRZEZ SCIANY,
 * wlasnym RenderPipeline bez testu glebi (wg poradnika Fabric dla 1.21.11).
 */
public class EspRenderer {

    private static final RenderPipeline FILLED_THROUGH_WALLS = RenderPipelines.register(
            RenderPipeline.builder(RenderPipelines.DEBUG_FILLED_SNIPPET)
                    .withLocation(Identifier.fromNamespaceAndPath("cheatutils", "pipeline/esp_filled_through_walls"))
                    .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
                    .build()
    );

    private static final ByteBufferBuilder ALLOCATOR = new ByteBufferBuilder(RenderType.SMALL_BUFFER_SIZE);
    private static final Vector4f COLOR_MODULATOR = new Vector4f(1f, 1f, 1f, 1f);
    private static final Vector3f MODEL_OFFSET = new Vector3f();
    private static final Matrix4f TEXTURE_MATRIX = new Matrix4f();
    private static MappableRingBuffer vertexBuffer;
    private static BufferBuilder buffer;

    private static final List<BlockPos> TRACKED_BLOCKS = new ArrayList<>();
    private static final List<BlockPos> TRACKED_STORAGE = new ArrayList<>();
    private static int scanCooldown = 0;

    // ------------------------------------------------------------------ skan (co tick)

    public static void tickScan() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;

        Xray xray = ModuleManager.xray;
        Esp esp = ModuleManager.esp;
        boolean xrayOn = xray.isEnabled();
        boolean storageOn = esp.isEnabled() && esp.storage.asBool();
        if (!xrayOn && !storageOn) {
            TRACKED_BLOCKS.clear();
            TRACKED_STORAGE.clear();
            return;
        }

        if (scanCooldown-- > 0) return;
        scanCooldown = 20; // raz na sekunde

        TRACKED_BLOCKS.clear();
        TRACKED_STORAGE.clear();
        BlockPos center = mc.player.blockPosition();

        if (xrayOn) {
            int radius = (int) xray.scanRadius.asDouble();
            BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
            for (int x = -radius; x <= radius; x++) {
                for (int y = -radius; y <= radius; y++) {
                    for (int z = -radius; z <= radius; z++) {
                        m.set(center.getX() + x, center.getY() + y, center.getZ() + z);
                        if (xray.isTracked(mc.level.getBlockState(m).getBlock())) {
                            TRACKED_BLOCKS.add(m.immutable());
                        }
                    }
                }
            }
        }

        if (storageOn) {
            int ccx = center.getX() >> 4;
            int ccz = center.getZ() >> 4;
            for (int cx = ccx - 4; cx <= ccx + 4; cx++) {
                for (int cz = ccz - 4; cz <= ccz + 4; cz++) {
                    if (!mc.level.hasChunk(cx, cz)) continue;
                    LevelChunk chunk = mc.level.getChunk(cx, cz);
                    for (BlockEntity be : chunk.getBlockEntities().values()) {
                        if (isStorage(be)) TRACKED_STORAGE.add(be.getBlockPos());
                    }
                }
            }
        }
    }

    private static boolean isStorage(BlockEntity be) {
        BlockEntityType<?> t = be.getType();
        return t == BlockEntityType.CHEST || t == BlockEntityType.TRAPPED_CHEST
                || t == BlockEntityType.BARREL || t == BlockEntityType.SHULKER_BOX
                || t == BlockEntityType.FURNACE || t == BlockEntityType.BLAST_FURNACE
                || t == BlockEntityType.SMOKER || t == BlockEntityType.HOPPER
                || t == BlockEntityType.DISPENSER || t == BlockEntityType.DROPPER
                || t == BlockEntityType.ENDER_CHEST;
    }

    // ------------------------------------------------------------------ render (co klatke)

    public static void render(WorldRenderContext context) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        Esp esp = ModuleManager.esp;
        Xray xray = ModuleManager.xray;
        boolean espOn = esp.isEnabled();
        boolean xrayOn = xray.isEnabled();
        if (!espOn && !xrayOn) return;

        PoseStack matrices = context.matrices();
        Vec3 cam = context.worldState().cameraRenderState.pos;

        matrices.pushPose();
        matrices.translate(-cam.x, -cam.y, -cam.z);

        if (buffer == null) {
            buffer = new BufferBuilder(ALLOCATOR, FILLED_THROUGH_WALLS.getVertexFormatMode(), FILLED_THROUGH_WALLS.getVertexFormat());
        }
        Matrix4fc pose = matrices.last().pose();

        if (espOn) {
            double maxD = esp.maxDistance.asDouble();
            AABB area = mc.player.getBoundingBox().inflate(maxD);
            Vec3 tracerStart = mc.player.getEyePosition().add(mc.player.getLookAngle().scale(1.0)).add(0, -0.15, 0);

            for (Entity e : mc.level.getEntitiesOfClass(Entity.class, area, x -> x != mc.player)) {
                float[] c = colorFor(e, esp);
                if (c == null) continue;
                if (e.distanceToSqr(mc.player) > maxD * maxD) continue;

                if (esp.boxes.asBool()) {
                    addBox(pose, e.getBoundingBox(), c[0], c[1], c[2], 0.30f);
                }
                if (esp.tracers.asBool()) {
                    addTracer(pose, tracerStart, e.getBoundingBox().getCenter(), c[0], c[1], c[2], 0.9f);
                }
            }

            if (esp.storage.asBool()) {
                for (BlockPos p : TRACKED_STORAGE) {
                    addBox(pose, new AABB(p), 1f, 0.8f, 0.1f, 0.30f);
                }
            }
        }

        if (xrayOn) {
            for (BlockPos p : TRACKED_BLOCKS) {
                addBox(pose, new AABB(p), 0.1f, 0.9f, 1f, 0.45f);
            }
        }

        matrices.popPose();

        MeshData mesh = buffer.build(); // null gdy nic nie dodano
        buffer = null;
        if (mesh != null) {
            drawMesh(mc, mesh);
        }
    }

    private static float[] colorFor(Entity e, Esp esp) {
        if (e instanceof Player) return esp.players.asBool() ? new float[]{1f, 0.2f, 0.2f} : null;
        if (e instanceof ItemEntity) return esp.drops.asBool() ? new float[]{1f, 1f, 1f} : null;
        if (e instanceof LivingEntity) {
            MobCategory cat = e.getType().getCategory();
            if (cat == MobCategory.MONSTER) return esp.mobs.asBool() ? new float[]{1f, 0.55f, 0f} : null;
            if (cat == MobCategory.MISC) return null;
            return esp.animals.asBool() ? new float[]{0.3f, 1f, 0.3f} : null;
        }
        return null;
    }

    // ------------------------------------------------------------------ geometria

    private static void v(Matrix4fc pose, float x, float y, float z, float r, float g, float b, float a) {
        buffer.addVertex(pose, x, y, z).setColor(r, g, b, a);
    }

    private static void addBox(Matrix4fc p, AABB box, float r, float g, float b, float a) {
        float x0 = (float) box.minX, y0 = (float) box.minY, z0 = (float) box.minZ;
        float x1 = (float) box.maxX, y1 = (float) box.maxY, z1 = (float) box.maxZ;
        // przod
        v(p, x0, y0, z1, r, g, b, a); v(p, x1, y0, z1, r, g, b, a); v(p, x1, y1, z1, r, g, b, a); v(p, x0, y1, z1, r, g, b, a);
        // tyl
        v(p, x1, y0, z0, r, g, b, a); v(p, x0, y0, z0, r, g, b, a); v(p, x0, y1, z0, r, g, b, a); v(p, x1, y1, z0, r, g, b, a);
        // lewo
        v(p, x0, y0, z0, r, g, b, a); v(p, x0, y0, z1, r, g, b, a); v(p, x0, y1, z1, r, g, b, a); v(p, x0, y1, z0, r, g, b, a);
        // prawo
        v(p, x1, y0, z1, r, g, b, a); v(p, x1, y0, z0, r, g, b, a); v(p, x1, y1, z0, r, g, b, a); v(p, x1, y1, z1, r, g, b, a);
        // gora
        v(p, x0, y1, z1, r, g, b, a); v(p, x1, y1, z1, r, g, b, a); v(p, x1, y1, z0, r, g, b, a); v(p, x0, y1, z0, r, g, b, a);
        // dol
        v(p, x0, y0, z0, r, g, b, a); v(p, x1, y0, z0, r, g, b, a); v(p, x1, y0, z1, r, g, b, a); v(p, x0, y0, z1, r, g, b, a);
    }

    /** Tracer = dwa cienkie, prostopadle przeciete paski (zawsze widoczne z kazdego kata). */
    private static void addTracer(Matrix4fc p, Vec3 from, Vec3 to, float r, float g, float b, float a) {
        Vec3 dir = to.subtract(from);
        if (dir.lengthSqr() < 1.0e-4) return;
        Vec3 up = new Vec3(0, 1, 0);
        Vec3 side = dir.cross(up);
        if (side.lengthSqr() < 1.0e-6) side = new Vec3(1, 0, 0);
        side = side.normalize().scale(0.02);
        Vec3 side2 = dir.cross(side).normalize().scale(0.02);
        strip(p, from, to, side, r, g, b, a);
        strip(p, from, to, side2, r, g, b, a);
    }

    private static void strip(Matrix4fc p, Vec3 s, Vec3 e, Vec3 o, float r, float g, float b, float a) {
        // dwie strony (rewers), zeby nie zalezec od cullingu
        quad(p, s.add(o), s.subtract(o), e.subtract(o), e.add(o), r, g, b, a);
        quad(p, e.add(o), e.subtract(o), s.subtract(o), s.add(o), r, g, b, a);
    }

    private static void quad(Matrix4fc p, Vec3 a, Vec3 b, Vec3 c, Vec3 d, float r, float g, float bl, float al) {
        v(p, (float) a.x, (float) a.y, (float) a.z, r, g, bl, al);
        v(p, (float) b.x, (float) b.y, (float) b.z, r, g, bl, al);
        v(p, (float) c.x, (float) c.y, (float) c.z, r, g, bl, al);
        v(p, (float) d.x, (float) d.y, (float) d.z, r, g, bl, al);
    }

    // ------------------------------------------------------------------ rysowanie (wg poradnika Fabric)

    private static void drawMesh(Minecraft client, MeshData builtBuffer) {
        MeshData.DrawState drawParameters = builtBuffer.drawState();
        VertexFormat format = drawParameters.format();

        GpuBuffer vertices = upload(drawParameters, format, builtBuffer);
        draw(client, FILLED_THROUGH_WALLS, builtBuffer, drawParameters, vertices, format);

        vertexBuffer.rotate();
    }

    private static GpuBuffer upload(MeshData.DrawState drawParameters, VertexFormat format, MeshData builtBuffer) {
        int vertexBufferSize = drawParameters.vertexCount() * format.getVertexSize();

        if (vertexBuffer == null || vertexBuffer.size() < vertexBufferSize) {
            if (vertexBuffer != null) vertexBuffer.close();
            vertexBuffer = new MappableRingBuffer(() -> "cheatutils esp pipeline", GpuBuffer.USAGE_VERTEX | GpuBuffer.USAGE_MAP_WRITE, vertexBufferSize);
        }

        CommandEncoder commandEncoder = RenderSystem.getDevice().createCommandEncoder();
        try (GpuBuffer.MappedView mappedView = commandEncoder.mapBuffer(vertexBuffer.currentBuffer().slice(0, builtBuffer.vertexBuffer().remaining()), false, true)) {
            MemoryUtil.memCopy(builtBuffer.vertexBuffer(), mappedView.data());
        }
        return vertexBuffer.currentBuffer();
    }

    private static void draw(Minecraft client, RenderPipeline pipeline, MeshData builtBuffer, MeshData.DrawState drawParameters, GpuBuffer vertices, VertexFormat format) {
        GpuBuffer indices;
        VertexFormat.IndexType indexType;

        if (pipeline.getVertexFormatMode() == VertexFormat.Mode.QUADS) {
            builtBuffer.sortQuads(ALLOCATOR, RenderSystem.getProjectionType().vertexSorting());
            indices = pipeline.getVertexFormat().uploadImmediateIndexBuffer(builtBuffer.indexBuffer());
            indexType = builtBuffer.drawState().indexType();
        } else {
            RenderSystem.AutoStorageIndexBuffer shapeIndexBuffer = RenderSystem.getSequentialBuffer(pipeline.getVertexFormatMode());
            indices = shapeIndexBuffer.getBuffer(drawParameters.indexCount());
            indexType = shapeIndexBuffer.type();
        }

        GpuBufferSlice dynamicTransforms = RenderSystem.getDynamicUniforms()
                .writeTransform(RenderSystem.getModelViewMatrix(), COLOR_MODULATOR, MODEL_OFFSET, TEXTURE_MATRIX);
        try (RenderPass renderPass = RenderSystem.getDevice()
                .createCommandEncoder()
                .createRenderPass(() -> "cheatutils esp pipeline rendering", client.getMainRenderTarget().getColorTextureView(), OptionalInt.empty(), client.getMainRenderTarget().getDepthTextureView(), OptionalDouble.empty())) {
            renderPass.setPipeline(pipeline);
            RenderSystem.bindDefaultUniforms(renderPass);
            renderPass.setUniform("DynamicTransforms", dynamicTransforms);
            renderPass.setVertexBuffer(0, vertices);
            renderPass.setIndexBuffer(indices, indexType);
            renderPass.drawIndexed(0, 0, drawParameters.indexCount(), 1);
        }

        builtBuffer.close();
    }
}
