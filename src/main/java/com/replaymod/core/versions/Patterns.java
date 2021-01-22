package com.replaymod.core.versions;

import com.replaymod.gradle.remap.Pattern;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.EntityRendererManager;
import net.minecraft.client.audio.SimpleSound;
import net.minecraft.crash.CrashReportCategory;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;

//#if MC>=11400
import net.minecraft.client.gui.widget.Widget;
import net.minecraft.client.MainWindow;
//#else
//$$ import net.minecraft.client.gui.GuiButton;
//#endif

//#if MC>=10904
import net.minecraft.util.SoundEvent;
import net.minecraft.crash.ICrashReportDetail;
//#else
//$$ import java.util.concurrent.Callable;
//#endif

//#if MC>=10809
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
//#else
//#endif

//#if MC>=10800
import net.minecraft.client.renderer.BufferBuilder;
//#else
//$$ import net.minecraft.entity.EntityLivingBase;
//#endif

import java.util.Collection;
import java.util.List;

class Patterns {
    //#if MC>=10904
    @Pattern
    private static void addCrashCallable(CrashReportCategory category, String name, ICrashReportDetail<String> callable) {
        //#if MC>=11200
        category.addDetail(name, callable);
        //#else
        //$$ category.setDetail(name, callable);
        //#endif
    }
    //#else
    //$$ @Pattern
    //$$ private static void addCrashCallable(CrashReportCategory category, String name, Callable<String> callable) {
    //$$     category.addCrashSectionCallable(name, callable);
    //$$ }
    //#endif

    @Pattern
    private static double Entity_getX(Entity entity) {
        //#if MC>=11500
        return entity.getPosX();
        //#else
        //$$ return entity.x;
        //#endif
    }

    @Pattern
    private static double Entity_getY(Entity entity) {
        //#if MC>=11500
        return entity.getPosY();
        //#else
        //$$ return entity.y;
        //#endif
    }

    @Pattern
    private static double Entity_getZ(Entity entity) {
        //#if MC>=11500
        return entity.getPosZ();
        //#else
        //$$ return entity.z;
        //#endif
    }

    @Pattern
    private static void Entity_setPos(Entity entity, double x, double y, double z) {
        //#if MC>=11500
        entity.setRawPosition(x, y, z);
        //#else
        //$$ { net.minecraft.entity.Entity self = entity; self.x = x; self.y = y; self.z = z; }
        //#endif
    }

    //#if MC>=11400
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
        //#if MC>=11600
        return button.getHeight();
        //#else
        //$$ return ((com.replaymod.core.mixin.AbstractButtonWidgetAccessor) button).getHeight();
        //#endif
    }
    //#else
    //$$ @Pattern
    //$$ private static void setWidth(GuiButton button, int value) {
    //$$     button.width = value;
    //$$ }
    //$$
    //$$ @Pattern
    //$$ private static int getWidth(GuiButton button) {
    //$$     return button.width;
    //$$ }
    //$$
    //$$ @Pattern
    //$$ private static int getHeight(GuiButton button) {
    //$$     return button.height;
    //$$ }
    //#endif

    @Pattern
    private static String readString(PacketBuffer buffer, int max) {
        //#if MC>=10800
        return buffer.readString(max);
        //#else
        //$$ return com.replaymod.core.versions.MCVer.tryReadString(buffer, max);
        //#endif
    }

    @Pattern
    //#if MC>=10800
    private static Entity getRenderViewEntity(Minecraft mc) {
        return mc.getRenderViewEntity();
    }
    //#else
    //$$ private static EntityLivingBase getRenderViewEntity(Minecraft mc) {
    //$$     return mc.renderViewEntity;
    //$$ }
    //#endif

    @Pattern
    //#if MC>=10800
    private static void setRenderViewEntity(Minecraft mc, Entity entity) {
        mc.setRenderViewEntity(entity);
    }
    //#else
    //$$ private static void setRenderViewEntity(Minecraft mc, EntityLivingBase entity) {
    //$$     mc.renderViewEntity = entity;
    //$$ }
    //#endif

    @Pattern
    private static Entity getVehicle(Entity passenger) {
        //#if MC>=10904
        return passenger.getRidingEntity();
        //#else
        //$$ return passenger.ridingEntity;
        //#endif
    }

    @Pattern
    private static Iterable<Entity> loadedEntityList(ClientWorld world) {
        //#if MC>=11400
        return world.getAllEntities();
        //#else
        //#if MC>=10809
        //$$ return world.loadedEntityList;
        //#else
        //$$ return ((java.util.List<net.minecraft.entity.Entity>) world.loadedEntityList);
        //#endif
        //#endif
    }

    @Pattern
    private static Collection<Entity>[] getEntitySectionArray(Chunk chunk) {
        //#if MC>=10800
        return chunk.getEntityLists();
        //#else
        //$$ return chunk.entityLists;
        //#endif
    }

    @Pattern
    private static List<? extends PlayerEntity> playerEntities(World world) {
        //#if MC>=11400
        return world.getPlayers();
        //#elseif MC>=10809
        //$$ return world.playerEntities;
        //#else
        //$$ return ((List<? extends net.minecraft.entity.player.EntityPlayer>) world.playerEntities);
        //#endif
    }

    @Pattern
    private static boolean isOnMainThread(Minecraft mc) {
        //#if MC>=11400
        return mc.isOnExecutionThread();
        //#else
        //$$ return mc.isCallingFromMinecraftThread();
        //#endif
    }

    @Pattern
    private static void scheduleOnMainThread(Minecraft mc, Runnable runnable) {
        //#if MC>=11400
        mc.enqueue(runnable);
        //#else
        //$$ mc.addScheduledTask(runnable);
        //#endif
    }

    @Pattern
    private static MainWindow getWindow(Minecraft mc) {
        //#if MC>=11500
        return mc.getMainWindow();
        //#elseif MC>=11400
        //$$ return mc.window;
        //#else
        //$$ return new com.replaymod.core.versions.Window(mc);
        //#endif
    }

    @Pattern
    private static BufferBuilder Tessellator_getBuffer(Tessellator tessellator) {
        //#if MC>=10800
        return tessellator.getBuffer();
        //#else
        //$$ return new BufferBuilder(tessellator);
        //#endif
    }

    @Pattern
    private static void BufferBuilder_beginPosCol(BufferBuilder buffer, int mode) {
        //#if MC>=10809
        buffer.begin(mode, DefaultVertexFormats.POSITION_COLOR);
        //#else
        //$$ buffer.startDrawing(mode /* POSITION_COLOR */);
        //#endif
    }

    @Pattern
    private static void BufferBuilder_addPosCol(BufferBuilder buffer, double x, double y, double z, int r, int g, int b, int a) {
        //#if MC>=10809
        buffer.pos(x, y, z).color(r, g, b, a).endVertex();
        //#else
        //$$ { WorldRenderer $buffer = buffer; double $x = x; double $y = y; double $z = z; $buffer.setColorRGBA(r, g, b, a); $buffer.addVertex($x, $y, $z); }
        //#endif
    }

    @Pattern
    private static void BufferBuilder_beginPosTex(BufferBuilder buffer, int mode) {
        //#if MC>=10809
        buffer.begin(mode, DefaultVertexFormats.POSITION_TEX);
        //#else
        //$$ buffer.startDrawing(mode /* POSITION_TEXTURE */);
        //#endif
    }

    @Pattern
    private static void BufferBuilder_addPosTex(BufferBuilder buffer, double x, double y, double z, float u, float v) {
        //#if MC>=10809
        buffer.pos(x, y, z).tex(u, v).endVertex();
        //#else
        //$$ buffer.addVertexWithUV(x, y, z, u, v);
        //#endif
    }

    @Pattern
    private static void BufferBuilder_beginPosTexCol(BufferBuilder buffer, int mode) {
        //#if MC>=10809
        buffer.begin(mode, DefaultVertexFormats.POSITION_TEX_COLOR);
        //#else
        //$$ buffer.startDrawing(mode /* POSITION_TEXTURE_COLOR */);
        //#endif
    }

    @Pattern
    private static void BufferBuilder_addPosTexCol(BufferBuilder buffer, double x, double y, double z, float u, float v, int r, int g, int b, int a) {
        //#if MC>=10809
        buffer.pos(x, y, z).tex(u, v).color(r, g, b, a).endVertex();
        //#else
        //$$ { WorldRenderer $buffer = buffer; double $x = x; double $y = y; double $z = z; float $u = u; float $v = v; $buffer.setColorRGBA(r, g, b, a); $buffer.addVertexWithUV($x, $y, $z, $u, $v); }
        //#endif
    }

    @Pattern
    private static Tessellator Tessellator_getInstance() {
        //#if MC>=10800
        return Tessellator.getInstance();
        //#else
        //$$ return Tessellator.instance;
        //#endif
    }

    @Pattern
    private static EntityRendererManager getEntityRenderDispatcher(Minecraft mc) {
        //#if MC>=10800
        return mc.getRenderManager();
        //#else
        //$$ return com.replaymod.core.versions.MCVer.getRenderManager(mc);
        //#endif
    }

    @Pattern
    private static float getCameraYaw(EntityRendererManager dispatcher) {
        //#if MC>=11500
        return dispatcher.info.getYaw();
        //#else
        //$$ return dispatcher.cameraYaw;
        //#endif
    }

    @Pattern
    private static float getCameraPitch(EntityRendererManager dispatcher) {
        //#if MC>=11500
        return dispatcher.info.getPitch();
        //#else
        //$$ return dispatcher.cameraPitch;
        //#endif
    }

    @Pattern
    private static float getRenderPartialTicks(Minecraft mc) {
        //#if MC>=10900
        return mc.getRenderPartialTicks();
        //#else
        //$$ return ((com.replaymod.core.mixin.MinecraftAccessor) mc).getTimer().renderPartialTicks;
        //#endif
    }

    @Pattern
    private static TextureManager getTextureManager(Minecraft mc) {
        //#if MC>=11400
        return mc.getTextureManager();
        //#else
        //$$ return mc.renderEngine;
        //#endif
    }

    @Pattern
    private static String getBoundKeyName(KeyBinding keyBinding) {
        //#if MC>=11600
        return keyBinding.func_238171_j_().getString();
        //#elseif MC>=11400
        //$$ return keyBinding.getLocalizedName();
        //#else
        //$$ return org.lwjgl.input.Keyboard.getKeyName(keyBinding.getKeyCode());
        //#endif
    }

    @Pattern
    private static SimpleSound master(ResourceLocation sound, float pitch) {
        //#if MC>=10900
        return SimpleSound.master(new SoundEvent(sound), pitch);
        //#elseif MC>=10800
        //$$ return PositionedSoundRecord.create(sound, pitch);
        //#else
        //$$ return PositionedSoundRecord.createPositionedSoundRecord(sound, pitch);
        //#endif
    }

    @Pattern
    private static boolean isKeyBindingConflicting(KeyBinding a, KeyBinding b) {
        //#if MC>=10900
        return a.conflicts(b);
        //#else
        //$$ return (a.getKeyCode() == b.getKeyCode());
        //#endif
    }
}
