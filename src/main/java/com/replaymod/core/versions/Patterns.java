package com.replaymod.core.versions;

import com.replaymod.gradle.remap.Pattern;
import net.minecraft.client.MainWindow;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.SimpleSound;
import net.minecraft.client.gui.widget.Widget;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.EntityRendererManager;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.crash.CrashReportCategory;
import net.minecraft.crash.ICrashReportDetail;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;

import java.util.Collection;
import java.util.List;

class Patterns {
    @Pattern
    private static void addCrashCallable(CrashReportCategory category, String name, ICrashReportDetail<String> callable) {
        category.addDetail(name, callable);
    }

    @Pattern
    private static double Entity_getX(Entity entity) {
        return entity.getPosX();
    }

    @Pattern
    private static double Entity_getY(Entity entity) {
        return entity.getPosY();
    }

    @Pattern
    private static double Entity_getZ(Entity entity) {
        return entity.getPosZ();
    }

    @Pattern
    private static void Entity_setPos(Entity entity, double x, double y, double z) {
        entity.setRawPosition(x, y, z);
    }

    @Pattern
    private static void setWidth(Widget button, int value) {
        button.setWidth(value);
    }

    @Pattern
    private static int getWidth(Widget button) {
        return button.getWidth();
    }

    @Pattern
    private static int getHeight(Widget button) {
        return button.getHeight();
    }

    @Pattern
    private static String readString(PacketBuffer buffer, int max) {
        return buffer.readString(max);
    }

    @Pattern
    private static Entity getRenderViewEntity(Minecraft mc) {
        return mc.getRenderViewEntity();
    }

    @Pattern
    private static void setRenderViewEntity(Minecraft mc, Entity entity) {
        mc.setRenderViewEntity(entity);
    }

    @Pattern
    private static Entity getVehicle(Entity passenger) {
        return passenger.getRidingEntity();
    }

    @Pattern
    private static Iterable<Entity> loadedEntityList(ClientWorld world) {
        return world.getAllEntities();
    }

    @Pattern
    private static Collection<Entity>[] getEntitySectionArray(Chunk chunk) {
        return chunk.getEntityLists();
    }

    @Pattern
    private static List<? extends PlayerEntity> playerEntities(World world) {
        return world.getPlayers();
    }

    @Pattern
    private static boolean isOnMainThread(Minecraft mc) {
        return mc.isOnExecutionThread();
    }

    @Pattern
    private static void scheduleOnMainThread(Minecraft mc, Runnable runnable) {
        mc.enqueue(runnable);
    }

    @Pattern
    private static MainWindow getWindow(Minecraft mc) {
        return mc.getMainWindow();
    }

    @Pattern
    private static BufferBuilder Tessellator_getBuffer(Tessellator tessellator) {
        return tessellator.getBuffer();
    }

    @Pattern
    private static void BufferBuilder_beginPosCol(BufferBuilder buffer, int mode) {
        buffer.begin(mode, DefaultVertexFormats.POSITION_COLOR);
    }

    @Pattern
    private static void BufferBuilder_addPosCol(BufferBuilder buffer, double x, double y, double z, int r, int g, int b, int a) {
        buffer.pos(x, y, z).color(r, g, b, a).endVertex();
    }

    @Pattern
    private static void BufferBuilder_beginPosTex(BufferBuilder buffer, int mode) {
        buffer.begin(mode, DefaultVertexFormats.POSITION_TEX);
    }

    @Pattern
    private static void BufferBuilder_addPosTex(BufferBuilder buffer, double x, double y, double z, float u, float v) {
        buffer.pos(x, y, z).tex(u, v).endVertex();
    }

    @Pattern
    private static void BufferBuilder_beginPosTexCol(BufferBuilder buffer, int mode) {
        buffer.begin(mode, DefaultVertexFormats.POSITION_TEX_COLOR);
    }

    @Pattern
    private static void BufferBuilder_addPosTexCol(BufferBuilder buffer, double x, double y, double z, float u, float v, int r, int g, int b, int a) {
        buffer.pos(x, y, z).tex(u, v).color(r, g, b, a).endVertex();
    }

    @Pattern
    private static Tessellator Tessellator_getInstance() {
        return Tessellator.getInstance();
    }

    @Pattern
    private static EntityRendererManager getEntityRenderDispatcher(Minecraft mc) {
        return mc.getRenderManager();
    }

    @Pattern
    private static float getCameraYaw(EntityRendererManager dispatcher) {
        return dispatcher.info.getYaw();
    }

    @Pattern
    private static float getCameraPitch(EntityRendererManager dispatcher) {
        return dispatcher.info.getPitch();
    }

    @Pattern
    private static float getRenderPartialTicks(Minecraft mc) {
        return mc.getRenderPartialTicks();
    }

    @Pattern
    private static TextureManager getTextureManager(Minecraft mc) {
        return mc.getTextureManager();
    }

    @Pattern
    private static String getBoundKeyName(KeyBinding keyBinding) {
        return keyBinding.func_238171_j_().getString();
    }

    @Pattern
    private static SimpleSound master(ResourceLocation sound, float pitch) {
        return SimpleSound.master(new SoundEvent(sound), pitch);
    }

    @Pattern
    private static boolean isKeyBindingConflicting(KeyBinding a, KeyBinding b) {
        return a.conflicts(b);
    }
}
