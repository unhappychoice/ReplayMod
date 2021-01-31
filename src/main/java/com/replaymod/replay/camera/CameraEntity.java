package com.replaymod.replay.camera;

import com.replaymod.core.KeyBindingRegistry;
import com.replaymod.core.ReplayMod;
import com.replaymod.core.SettingsRegistry;
import com.replaymod.core.events.KeyBindingEventCallback;
import com.replaymod.core.events.PreRenderCallback;
import com.replaymod.core.events.PreRenderHandCallback;
import com.replaymod.core.events.SettingsChangedCallback;
import com.replaymod.core.utils.Utils;
import com.replaymod.gui.utils.EventRegistrations;
import com.replaymod.gui.versions.callbacks.PreTickCallback;
import com.replaymod.mixin.FirstPersonRendererAccessor;
import com.replaymod.replay.ReplayHandler;
import com.replaymod.replay.ReplayModReplay;
import com.replaymod.replay.Setting;
import com.replaymod.replay.events.RenderHotbarCallback;
import com.replaymod.replay.events.RenderSpectatorCrosshairCallback;
import com.replaymod.replay.events.ReplayChatMessageEvent;
import com.replaymod.replaystudio.util.Location;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.AbstractClientPlayerEntity;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.client.network.play.ClientPlayNetHandler;
import net.minecraft.client.util.ClientRecipeBook;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerModelPart;
import net.minecraft.fluid.Fluid;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.item.ItemStack;
import net.minecraft.stats.StatisticsManager;
import net.minecraft.tags.ITag;
import net.minecraft.util.Hand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

import static com.replaymod.core.versions.MCVer.getMinecraft;

/**
 * The camera entity used as the main player entity during replay viewing.
 * During a replay the player should be an instance of this class.
 * Camera movement is controlled by a separate {@link CameraController}.
 */
@SuppressWarnings("EntityConstructor")
public class CameraEntity
        extends ClientPlayerEntity {
    private static final UUID CAMERA_UUID = UUID.nameUUIDFromBytes("ReplayModCamera".getBytes(StandardCharsets.UTF_8));

    /**
     * Roll of this camera in degrees.
     */
    public float roll;

    private CameraController cameraController;

    private long lastControllerUpdate = System.currentTimeMillis();

    /**
     * The entity whose hand was the last one rendered.
     */
    private Entity lastHandRendered = null;

    /**
     * The hashCode and equals methods of Entity are not stable.
     * Therefore we cannot register any event handlers directly in the CameraEntity class and
     * instead have this inner class.
     */
    private EventHandler eventHandler = new EventHandler();

    public CameraEntity(
            Minecraft mcIn,
            ClientWorld worldIn,
            ClientPlayNetHandler netHandlerPlayClient,
            StatisticsManager statisticsManager
            , ClientRecipeBook recipeBook
    ) {
        super(mcIn,
                worldIn,
                netHandlerPlayClient,
                statisticsManager
                , recipeBook
                , false
                , false
        );
        setUniqueId(CAMERA_UUID);
        eventHandler.register();
        if (ReplayModReplay.instance.getReplayHandler().getSpectatedUUID() == null) {
            cameraController = ReplayModReplay.instance.createCameraController(this);
        } else {
            cameraController = new SpectatorCameraController(this);
        }
    }

    public CameraController getCameraController() {
        return cameraController;
    }

    public void setCameraController(CameraController cameraController) {
        this.cameraController = cameraController;
    }

    /**
     * Moves the camera by the specified delta.
     *
     * @param x Delta in X direction
     * @param y Delta in Y direction
     * @param z Delta in Z direction
     */
    public void moveCamera(double x, double y, double z) {
        setCameraPosition(this.getPosX() + x, this.getPosY() + y, this.getPosZ() + z);
    }

    /**
     * Set the camera position.
     *
     * @param x X coordinate
     * @param y Y coordinate
     * @param z Z coordinate
     */
    public void setCameraPosition(double x, double y, double z) {
        this.lastTickPosX = this.prevPosX = x;
        this.lastTickPosY = this.prevPosY = y;
        this.lastTickPosZ = this.prevPosZ = z;
        this.setRawPosition(x, y, z);
        updateBoundingBox();
    }

    /**
     * Sets the camera rotation.
     *
     * @param yaw   Yaw in degrees
     * @param pitch Pitch in degrees
     * @param roll  Roll in degrees
     */
    public void setCameraRotation(float yaw, float pitch, float roll) {
        this.prevRotationYaw = this.rotationYaw = yaw;
        this.prevRotationPitch = this.rotationPitch = pitch;
        this.roll = roll;
    }

    /**
     * Sets the camera position and rotation to that of the specified AdvancedPosition
     *
     * @param pos The position and rotation to set
     */
    public void setCameraPosRot(Location pos) {
        setCameraRotation(pos.getYaw(), pos.getPitch(), roll);
        setCameraPosition(pos.getX(), pos.getY(), pos.getZ());
    }

    /**
     * Sets the camera position and rotation to that of the specified entity.
     *
     * @param to The entity whose position to copy
     */
    public void setCameraPosRot(Entity to) {
        if (to == this) return;
        float yOffset = 0;
        this.prevPosX = to.prevPosX;
        this.prevPosY = to.prevPosY + yOffset;
        this.prevPosZ = to.prevPosZ;
        this.prevRotationYaw = to.prevRotationYaw;
        this.prevRotationPitch = to.prevRotationPitch;
        this.setRawPosition(to.getPosX(), to.getPosY(), to.getPosZ());
        this.rotationYaw = to.rotationYaw;
        this.rotationPitch = to.rotationPitch;
        this.lastTickPosX = to.lastTickPosX;
        this.lastTickPosY = to.lastTickPosY + yOffset;
        this.lastTickPosZ = to.lastTickPosZ;
        updateBoundingBox();
    }

    private void updateBoundingBox() {
        float width = getWidth();
        float height = getHeight();
        setBoundingBox(new AxisAlignedBB(
                this.getPosX() - width / 2, this.getPosY(), this.getPosZ() - width / 2,
                this.getPosX() + width / 2, this.getPosY() + height, this.getPosZ() + width / 2));
    }

    @Override
    public void tick() {
        Entity view =
                this.mc.getRenderViewEntity();
        if (view != null) {
            // Make sure we're always spectating the right entity
            // This is important if the spectated player respawns as their
            // entity is recreated and we have to spectate a new entity
            UUID spectating = ReplayModReplay.instance.getReplayHandler().getSpectatedUUID();
            if (spectating != null && (view.getUniqueID() != spectating
                    || view.world != this.world)
                    || this.world.getEntityByID(view.getEntityId()) != view) {
                if (spectating == null) {
                    // Entity (non-player) died, stop spectating
                    ReplayModReplay.instance.getReplayHandler().spectateEntity(this);
                    return;
                }
                view = this.world.getPlayerByUuid(spectating);
                if (view != null) {
                    this.mc.setRenderViewEntity(view);
                } else {
                    this.mc.setRenderViewEntity(this);
                    return;
                }
            }
            // Move cmera to their position so when we exit the first person view
            // we don't jump back to where we entered it
            if (view != this) {
                setCameraPosRot(view);
            }
        }
    }

    @Override
    public void preparePlayerToSpawn() {
        // Make sure our world is up-to-date in case of world changes
        if (this.mc.world != null) {
            this.world = this.mc.world;
        }
        super.preparePlayerToSpawn();
    }

    @Override
    public void setRotation(float yaw, float pitch) {
        if (this.mc.getRenderViewEntity() == this) {
            // Only update camera rotation when the camera is the view
            super.setRotation(yaw, pitch);
        }
    }

    @Override
    public boolean isEntityInsideOpaqueBlock() {
        return falseUnlessSpectating(Entity::isEntityInsideOpaqueBlock); // Make sure no suffocation overlay is rendered
    }


    @Override
    public boolean areEyesInFluid(ITag<Fluid> fluid) {
        return falseUnlessSpectating(entity -> entity.areEyesInFluid(fluid));
    }

    @Override
    public boolean isBurning() {
        return falseUnlessSpectating(Entity::isBurning); // Make sure no fire overlay is rendered
    }

    private boolean falseUnlessSpectating(Function<Entity, Boolean> property) {
        Entity view = this.mc.getRenderViewEntity();
        if (view != null && view != this) {
            return property.apply(view);
        }
        return false;
    }

    @Override
    public boolean canBePushed() {
        return false; // We are in full control of ourselves
    }

    @Override
    protected void handleRunningEffect() {
        // We do not produce any particles, we are a camera
    }

    @Override
    public boolean canBeCollidedWith() {
        return false; // We are a camera, we cannot collide
    }

    @Override
    public boolean isSpectator() {
        ReplayHandler replayHandler = ReplayModReplay.instance.getReplayHandler();
        return replayHandler == null || replayHandler.isCameraView(); // Make sure we're treated as spectator
    }

    @Override
    public boolean isInRangeToRender3d(double double_1, double double_2, double double_3) {
        return false; // never render the camera otherwise it'd be visible e.g. in 3rd-person or with shaders
    }

    @Override
    public float getFovModifier() {
        Entity view = this.mc.getRenderViewEntity();
        if (view != this && view instanceof AbstractClientPlayerEntity) {
            return ((AbstractClientPlayerEntity) view).getFovModifier();
        }
        return 1;
    }

    @Override
    public boolean isInvisible() {
        Entity view = this.mc.getRenderViewEntity();
        if (view != this) {
            return view.isInvisible();
        }
        return super.isInvisible();
    }

    @Override
    public ResourceLocation getLocationSkin() {
        Entity view = this.mc.getRenderViewEntity();
        if (view != this && view instanceof PlayerEntity) {
            return Utils.getResourceLocationForPlayerUUID(view.getUniqueID());
        }
        return super.getLocationSkin();
    }

    @Override
    public String getSkinType() {
        Entity view = this.mc.getRenderViewEntity();
        if (view != this && view instanceof AbstractClientPlayerEntity) {
            return ((AbstractClientPlayerEntity) view).getSkinType();
        }
        return super.getSkinType();
    }

    @Override
    public boolean isWearing(PlayerModelPart modelPart) {
        Entity view = this.mc.getRenderViewEntity();
        if (view != this && view instanceof PlayerEntity) {
            return ((PlayerEntity) view).isWearing(modelPart);
        }
        return super.isWearing(modelPart);
    }

    @Override
    public float getSwingProgress(float renderPartialTicks) {
        Entity view = this.mc.getRenderViewEntity();
        if (view != this && view instanceof PlayerEntity) {
            return ((PlayerEntity) view).getSwingProgress(renderPartialTicks);
        }
        return 0;
    }

    @Override
    public float getCooldownPeriod() {
        Entity view = this.mc.getRenderViewEntity();
        if (view != this && view instanceof PlayerEntity) {
            return ((PlayerEntity) view).getCooldownPeriod();
        }
        return 1;
    }

    @Override
    public float getCooledAttackStrength(float adjustTicks) {
        Entity view = this.mc.getRenderViewEntity();
        if (view != this && view instanceof PlayerEntity) {
            return ((PlayerEntity) view).getCooledAttackStrength(adjustTicks);
        }
        // Default to 1 as to not render the cooldown indicator (renders for < 1)
        return 1;
    }

    @Override
    public Hand getActiveHand() {
        Entity view = this.mc.getRenderViewEntity();
        if (view != this && view instanceof PlayerEntity) {
            return ((PlayerEntity) view).getActiveHand();
        }
        return super.getActiveHand();
    }

    @Override
    public boolean isHandActive() {
        Entity view = this.mc.getRenderViewEntity();
        if (view != this && view instanceof PlayerEntity) {
            return ((PlayerEntity) view).isHandActive();
        }
        return super.isHandActive();
    }

    @Override
    protected void playEquipSound(ItemStack itemStack_1) {
        // Suppress equip sounds
    }

    @Override
    public RayTraceResult pick(double maxDistance, float tickDelta, boolean fluids) {
        RayTraceResult result = super.pick(maxDistance, tickDelta, fluids);

        // Make sure we can never look at blocks (-> no outline)
        if (result instanceof BlockRayTraceResult) {
            BlockRayTraceResult blockResult = (BlockRayTraceResult) result;
            result = BlockRayTraceResult.createMiss(result.getHitVec(), blockResult.getFace(), blockResult.getPos());
        }

        return result;
    }


    @Override
    public void remove() {
        super.remove();
        if (eventHandler != null) {
            eventHandler.unregister();
            eventHandler = null;
        }
    }

    private void update() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.world != this.world) {
            if (eventHandler != null) {
                eventHandler.unregister();
                eventHandler = null;
            }
            return;
        }

        long now = System.currentTimeMillis();
        long timePassed = now - lastControllerUpdate;
        cameraController.update(timePassed / 50f);
        lastControllerUpdate = now;

        handleInputEvents();

        Map<String, KeyBindingRegistry.Binding> keyBindings = ReplayMod.instance.getKeyBindingRegistry().getBindings();
        if (keyBindings.get("replaymod.input.rollclockwise").keyBinding.isKeyDown()) {
            roll += Utils.isCtrlDown() ? 0.2 : 1;
        }
        if (keyBindings.get("replaymod.input.rollcounterclockwise").keyBinding.isKeyDown()) {
            roll -= Utils.isCtrlDown() ? 0.2 : 1;
        }

        this.noClip = this.isSpectator();
    }

    private void handleInputEvents() {
        if (this.mc.gameSettings.keyBindAttack.isPressed() || this.mc.gameSettings.keyBindUseItem.isPressed()) {
            if (this.mc.currentScreen == null && canSpectate(this.mc.pointedEntity)) {
                ReplayModReplay.instance.getReplayHandler().spectateEntity(
                        this.mc.pointedEntity);
                // Make sure we don't exit right away
                //noinspection StatementWithEmptyBody
                while (this.mc.gameSettings.keyBindSneak.isPressed()) ;
            }
        }
    }

    private void updateArmYawAndPitch() {
        this.prevRenderArmYaw = this.renderArmYaw;
        this.prevRenderArmPitch = this.renderArmPitch;
        this.renderArmPitch = this.renderArmPitch + (this.rotationPitch - this.renderArmPitch) * 0.5f;
        this.renderArmYaw = this.renderArmYaw + (this.rotationYaw - this.renderArmYaw) * 0.5f;
    }

    public boolean canSpectate(Entity e) {
        return e != null
                && !e.isInvisible();
    }

    @Override
    public void sendMessage(ITextComponent component, UUID senderUUID) {
        if (MinecraftForge.EVENT_BUS.post(new ReplayChatMessageEvent(this))) return;
        super.sendMessage(component, senderUUID);
    }

    private
    class EventHandler extends EventRegistrations {
        private final Minecraft mc = getMinecraft();

        private EventHandler() {
        }

        {
            on(PreTickCallback.EVENT, this::onPreClientTick);
        }

        private void onPreClientTick() {
            updateArmYawAndPitch();
        }

        {
            on(PreRenderCallback.EVENT, this::onRenderUpdate);
        }

        private void onRenderUpdate() {
            update();
        }

        {
            on(KeyBindingEventCallback.EVENT, CameraEntity.this::handleInputEvents);
        }

        {
            on(RenderSpectatorCrosshairCallback.EVENT, this::shouldRenderSpectatorCrosshair);
        }

        private Boolean shouldRenderSpectatorCrosshair() {
            return canSpectate(mc.pointedEntity);
        }

        {
            on(RenderHotbarCallback.EVENT, this::shouldRenderHotbar);
        }

        private Boolean shouldRenderHotbar() {
            return false;
        }

        {
            on(SettingsChangedCallback.EVENT, this::onSettingsChanged);
        }

        private void onSettingsChanged(SettingsRegistry registry, SettingsRegistry.SettingKey<?> key) {
            if (key == Setting.CAMERA) {
                if (ReplayModReplay.instance.getReplayHandler().getSpectatedUUID() == null) {
                    cameraController = ReplayModReplay.instance.createCameraController(CameraEntity.this);
                } else {
                    cameraController = new SpectatorCameraController(CameraEntity.this);
                }
            }
        }

        {
            on(PreRenderHandCallback.EVENT, this::onRenderHand);
        }

        private boolean onRenderHand() {
            // Unless we are spectating another player, don't render our hand
            Entity view = mc.getRenderViewEntity();
            if (view == CameraEntity.this || !(view instanceof PlayerEntity)) {
                return true; // cancel hand rendering
            } else {
                PlayerEntity player = (PlayerEntity) view;
                // When the spectated player has changed, force equip their items to prevent the equip animation
                if (lastHandRendered != player) {
                    lastHandRendered = player;

                    FirstPersonRendererAccessor acc = (FirstPersonRendererAccessor) mc.gameRenderer.itemRenderer;
                    acc.setPrevEquippedProgressMainHand(1);
                    acc.setPrevEquippedProgressOffHand(1);
                    acc.setEquippedProgressMainHand(1);
                    acc.setEquippedProgressOffHand(1);
                    acc.setItemStackMainHand(player.getItemStackFromSlot(EquipmentSlotType.MAINHAND));
                    acc.setItemStackOffHand(player.getItemStackFromSlot(EquipmentSlotType.OFFHAND));


                    mc.player.renderArmYaw = mc.player.prevRenderArmYaw = player.rotationYaw;
                    mc.player.renderArmPitch = mc.player.prevRenderArmPitch = player.rotationPitch;
                }
                return false;
            }
        }

        // Moved to MixinCamera

        private boolean heldItemTooltipsWasTrue;

        @SubscribeEvent
        public void preRenderGameOverlay(RenderGameOverlayEvent.Pre event) {
            switch (event.getType()) {
                case ALL:
                    heldItemTooltipsWasTrue = mc.gameSettings.heldItemTooltips;
                    mc.gameSettings.heldItemTooltips = false;
                    break;
                case ARMOR:
                case HEALTH:
                case FOOD:
                case AIR:
                case HOTBAR:
                case EXPERIENCE:
                case HEALTHMOUNT:
                case JUMPBAR:
                case POTION_ICONS:
                    event.setCanceled(true);
                    break;
                case HELMET:
                case PORTAL:
                case CROSSHAIRS:
                case BOSSHEALTH:
                case BOSSINFO:
                case SUBTITLES:
                case TEXT:
                case CHAT:
                case PLAYER_LIST:
                case DEBUG:
                    break;
            }
        }

        @SubscribeEvent
        public void postRenderGameOverlay(RenderGameOverlayEvent.Post event) {
            if (event.getType() != RenderGameOverlayEvent.ElementType.ALL) return;
            mc.gameSettings.heldItemTooltips = heldItemTooltipsWasTrue;
        }
    }
}
