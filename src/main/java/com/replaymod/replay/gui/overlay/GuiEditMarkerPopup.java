package com.replaymod.replay.gui.overlay;

import com.google.common.base.Strings;
import com.replaymod.core.versions.MCVer.Keyboard;
import com.replaymod.gui.container.GuiContainer;
import com.replaymod.gui.container.GuiPanel;
import com.replaymod.gui.element.GuiTextField;
import com.replaymod.gui.function.Typeable;
import com.replaymod.gui.layout.GridLayout;
import com.replaymod.gui.layout.HorizontalLayout;
import com.replaymod.gui.layout.VerticalLayout;
import com.replaymod.gui.popup.AbstractGuiPopup;
import com.replaymod.gui.utils.Colors;
import com.replaymod.replaystudio.data.Marker;
import de.johni0702.minecraft.gui.utils.lwjgl.ReadablePoint;

import java.util.function.Consumer;

public class GuiEditMarkerPopup extends AbstractGuiPopup<GuiEditMarkerPopup> implements Typeable {
    private static com.replaymod.gui.element.GuiNumberField newGuiNumberField() {
        return new com.replaymod.gui.element.GuiNumberField().setSize(150, 20).setValidateOnFocusChange(true);
    }

    private final Consumer<Marker> onSave;

    public final com.replaymod.gui.element.GuiLabel title = new com.replaymod.gui.element.GuiLabel().setI18nText("replaymod.gui.editkeyframe.title.marker");

    public final com.replaymod.gui.element.GuiTextField nameField = new GuiTextField().setSize(150, 20);
    // TODO: Replace with a min/sec/msec field
    public final com.replaymod.gui.element.GuiNumberField timeField = newGuiNumberField().setPrecision(0);

    public final com.replaymod.gui.element.GuiNumberField xField = newGuiNumberField().setPrecision(10);
    public final com.replaymod.gui.element.GuiNumberField yField = newGuiNumberField().setPrecision(10);
    public final com.replaymod.gui.element.GuiNumberField zField = newGuiNumberField().setPrecision(10);

    public final com.replaymod.gui.element.GuiNumberField yawField = newGuiNumberField().setPrecision(5);
    public final com.replaymod.gui.element.GuiNumberField pitchField = newGuiNumberField().setPrecision(5);
    public final com.replaymod.gui.element.GuiNumberField rollField = newGuiNumberField().setPrecision(5);

    public final GuiPanel inputs = GuiPanel.builder()
            .layout(new GridLayout().setColumns(2).setSpacingX(7).setSpacingY(3))
            .with(new com.replaymod.gui.element.GuiLabel().setI18nText("replaymod.gui.editkeyframe.markername"), new GridLayout.Data(0, 0.5))
            .with(nameField, new GridLayout.Data(1, 0.5))
            .with(new com.replaymod.gui.element.GuiLabel().setI18nText("replaymod.gui.editkeyframe.timestamp"), new GridLayout.Data(0, 0.5))
            .with(timeField, new GridLayout.Data(1, 0.5))
            .with(new com.replaymod.gui.element.GuiLabel().setI18nText("replaymod.gui.editkeyframe.xpos"), new GridLayout.Data(0, 0.5))
            .with(xField, new GridLayout.Data(1, 0.5))
            .with(new com.replaymod.gui.element.GuiLabel().setI18nText("replaymod.gui.editkeyframe.ypos"), new GridLayout.Data(0, 0.5))
            .with(yField, new GridLayout.Data(1, 0.5))
            .with(new com.replaymod.gui.element.GuiLabel().setI18nText("replaymod.gui.editkeyframe.zpos"), new GridLayout.Data(0, 0.5))
            .with(zField, new GridLayout.Data(1, 0.5))
            .with(new com.replaymod.gui.element.GuiLabel().setI18nText("replaymod.gui.editkeyframe.camyaw"), new GridLayout.Data(0, 0.5))
            .with(yawField, new GridLayout.Data(1, 0.5))
            .with(new com.replaymod.gui.element.GuiLabel().setI18nText("replaymod.gui.editkeyframe.campitch"), new GridLayout.Data(0, 0.5))
            .with(pitchField, new GridLayout.Data(1, 0.5))
            .with(new com.replaymod.gui.element.GuiLabel().setI18nText("replaymod.gui.editkeyframe.camroll"), new GridLayout.Data(0, 0.5))
            .with(rollField, new GridLayout.Data(1, 0.5))
            .build();

    public final com.replaymod.gui.element.GuiButton saveButton = new com.replaymod.gui.element.GuiButton().onClick(new Runnable() {
        @Override
        public void run() {
            Marker marker = new Marker();
            marker.setName(Strings.emptyToNull(nameField.getText()));
            marker.setTime(timeField.getInteger());
            marker.setX(xField.getDouble());
            marker.setY(yField.getDouble());
            marker.setZ(zField.getDouble());
            marker.setYaw(yawField.getFloat());
            marker.setPitch(pitchField.getFloat());
            marker.setRoll(rollField.getFloat());
            onSave.accept(marker);
            close();
        }
    }).setSize(150, 20).setI18nLabel("replaymod.gui.save");

    public final com.replaymod.gui.element.GuiButton cancelButton = new com.replaymod.gui.element.GuiButton().onClick(new Runnable() {
        @Override
        public void run() {
            close();
        }
    }).setSize(150, 20).setI18nLabel("replaymod.gui.cancel");

    public final GuiPanel buttons = new GuiPanel()
            .setLayout(new HorizontalLayout(HorizontalLayout.Alignment.CENTER).setSpacing(7))
            .addElements(new HorizontalLayout.Data(0.5), saveButton, cancelButton);

    public GuiEditMarkerPopup(GuiContainer container, Marker marker, Consumer<Marker> onSave) {
        super(container);
        this.onSave = onSave;

        setBackgroundColor(Colors.DARK_TRANSPARENT);

        popup.setLayout(new VerticalLayout().setSpacing(5))
                .addElements(new VerticalLayout.Data(0.5), title, inputs, buttons);
        popup.invokeAll(com.replaymod.gui.element.IGuiLabel.class, e -> e.setColor(Colors.BLACK));

        nameField.setText(Strings.nullToEmpty(marker.getName()));
        timeField.setValue(marker.getTime());
        xField.setValue(marker.getX());
        yField.setValue(marker.getY());
        zField.setValue(marker.getZ());
        yawField.setValue(marker.getYaw());
        pitchField.setValue(marker.getPitch());
        rollField.setValue(marker.getRoll());
    }

    @Override
    public void open() {
        super.open();
    }

    @Override
    protected GuiEditMarkerPopup getThis() {
        return this;
    }

    @Override
    public boolean typeKey(ReadablePoint mousePosition, int keyCode, char keyChar, boolean ctrlDown, boolean shiftDown) {
        if (keyCode == Keyboard.KEY_ESCAPE) {
            cancelButton.onClick();
            return true;
        }
        return false;
    }
}
