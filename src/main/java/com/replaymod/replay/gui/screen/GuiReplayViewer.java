package com.replaymod.replay.gui.screen;

import com.google.common.util.concurrent.SettableFuture;
import com.replaymod.core.ReplayMod;
import com.replaymod.core.SettingsRegistry;
import com.replaymod.core.gui.GuiReplaySettings;
import com.replaymod.core.utils.Utils;
import com.replaymod.core.versions.MCVer;
import com.replaymod.core.versions.MCVer.Keyboard;
import com.replaymod.gui.GuiRenderer;
import com.replaymod.gui.RenderInfo;
import com.replaymod.gui.container.AbstractGuiContainer;
import com.replaymod.gui.container.GuiContainer;
import com.replaymod.gui.container.GuiPanel;
import com.replaymod.gui.container.GuiScreen;
import com.replaymod.gui.element.GuiTextField;
import com.replaymod.gui.element.advanced.AbstractGuiResourceLoadingList;
import com.replaymod.gui.function.Typeable;
import com.replaymod.gui.layout.CustomLayout;
import com.replaymod.gui.layout.HorizontalLayout;
import com.replaymod.gui.layout.VerticalLayout;
import com.replaymod.gui.popup.AbstractGuiPopup;
import com.replaymod.gui.popup.GuiYesNoPopup;
import com.replaymod.gui.utils.Colors;
import com.replaymod.gui.utils.Consumer;
import com.replaymod.gui.versions.Image;
import com.replaymod.render.gui.GuiRenderQueue;
import com.replaymod.render.rendering.VideoRenderer;
import com.replaymod.render.utils.RenderJob;
import com.replaymod.replay.ReplayModReplay;
import com.replaymod.replay.Setting;
import com.replaymod.replaystudio.replay.ReplayFile;
import com.replaymod.replaystudio.replay.ReplayMetaData;
import com.replaymod.replaystudio.us.myles.ViaVersion.api.Pair;
import de.johni0702.minecraft.gui.utils.lwjgl.Dimension;
import de.johni0702.minecraft.gui.utils.lwjgl.ReadableDimension;
import de.johni0702.minecraft.gui.utils.lwjgl.ReadablePoint;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.screen.AlertScreen;
import net.minecraft.crash.CrashReport;
import net.minecraft.crash.ReportedException;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOCase;
import org.apache.commons.io.filefilter.SuffixFileFilter;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static com.replaymod.gui.versions.MCVer.getFontRenderer;
import static com.replaymod.replay.ReplayModReplay.LOGGER;

public class GuiReplayViewer extends GuiScreen {
    private final ReplayModReplay mod;

    public final GuiReplayList list = new GuiReplayList(this).onSelectionChanged(this::updateButtons).onSelectionDoubleClicked(() -> {
        if (this.loadButton.isEnabled()) {
            this.loadButton.onClick();
        }
    });

    public final com.replaymod.gui.element.GuiButton loadButton = new com.replaymod.gui.element.GuiButton().onClick(new Runnable() {
        private boolean loading = false;

        @Override
        public void run() {
            // Prevent the player from opening the replay twice at the same time
            if (loading) {
                return;
            }
            loading = true;
            loadButton.setDisabled(); // visual hint

            List<GuiReplayEntry> selected = list.getSelected();
            if (selected.size() == 1) {
                try {
                    mod.startReplay(selected.get(0).file);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                Iterator<Pair<File, List<RenderJob>>> replays = selected.stream()
                        .filter(it -> !it.renderQueue.isEmpty())
                        .map(it -> new Pair<>(it.file, it.renderQueue))
                        .iterator();
                GuiRenderQueue.processMultipleReplays(GuiReplayViewer.this, mod, replays, () -> {
                    loading = false;
                    updateButtons();
                    display();
                });
            }
        }
    }).setSize(150, 20);

    public final com.replaymod.gui.element.GuiButton folderButton = new com.replaymod.gui.element.GuiButton().onClick(new Runnable() {
        @Override
        public void run() {
            try {
                File folder = mod.getCore().getReplayFolder().toFile();

                MCVer.openFile(folder);
            } catch (IOException e) {
                mod.getLogger().error("Cannot open file", e);
            }
        }
    }).setSize(150, 20).setI18nLabel("replaymod.gui.viewer.replayfolder");

    public final com.replaymod.gui.element.GuiButton renameButton = new com.replaymod.gui.element.GuiButton().onClick(new Runnable() {
        @Override
        public void run() {
            final File file = list.getSelected().get(0).file;
            String name = Utils.fileNameToReplayName(file.getName());
            final com.replaymod.gui.element.GuiTextField nameField = new com.replaymod.gui.element.GuiTextField().setSize(200, 20).setFocused(true).setText(name);
            final GuiYesNoPopup popup = GuiYesNoPopup.open(GuiReplayViewer.this,
                    new com.replaymod.gui.element.GuiLabel().setI18nText("replaymod.gui.viewer.rename.name").setColor(Colors.BLACK),
                    nameField
            ).setYesI18nLabel("replaymod.gui.rename").setNoI18nLabel("replaymod.gui.cancel");
            ((VerticalLayout) popup.getInfo().getLayout()).setSpacing(7);
            nameField.onEnter(new Runnable() {
                @Override
                public void run() {
                    if (popup.getYesButton().isEnabled()) {
                        popup.getYesButton().onClick();
                    }
                }
            }).onTextChanged(obj -> {
                popup.getYesButton().setEnabled(!nameField.getText().isEmpty()
                        && !new File(file.getParentFile(), Utils.replayNameToFileName(nameField.getText())).exists());
            });
            popup.onAccept(() -> {
                // Sanitize their input
                String newName = nameField.getText().trim();
                // This file is what they want
                File targetFile = new File(file.getParentFile(), Utils.replayNameToFileName(newName));
                try {
                    // Finally, try to move it
                    FileUtils.moveFile(file, targetFile);
                } catch (IOException e) {
                    // We failed (might also be their OS)
                    e.printStackTrace();
                    getMinecraft().displayGuiScreen(new AlertScreen(
                            GuiReplayViewer.this::display,
                            new TranslationTextComponent("replaymod.gui.viewer.delete.failed1"),
                            new TranslationTextComponent("replaymod.gui.viewer.delete.failed2")
                    ));
                    return;
                }
                list.load();
            });
        }
    }).setSize(73, 20).setI18nLabel("replaymod.gui.rename").setDisabled();
    public final com.replaymod.gui.element.GuiButton deleteButton = new com.replaymod.gui.element.GuiButton().onClick(() -> {
        for (GuiReplayEntry entry : list.getSelected()) {
            String name = entry.name.getText();
            GuiYesNoPopup.open(GuiReplayViewer.this,
                    new com.replaymod.gui.element.GuiLabel().setI18nText("replaymod.gui.viewer.delete.linea").setColor(Colors.BLACK),
                    new com.replaymod.gui.element.GuiLabel().setI18nText("replaymod.gui.viewer.delete.lineb", name + TextFormatting.RESET).setColor(Colors.BLACK)
            ).setYesI18nLabel("replaymod.gui.delete").setNoI18nLabel("replaymod.gui.cancel").onAccept(() -> {
                try {
                    FileUtils.forceDelete(entry.file);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                list.load();
            });
        }
    }).setSize(73, 20).setI18nLabel("replaymod.gui.delete").setDisabled();

    public final com.replaymod.gui.element.GuiButton settingsButton = new com.replaymod.gui.element.GuiButton(this)
            .setSize(20, 20)
            .setTexture(ReplayMod.TEXTURE, ReplayMod.TEXTURE_SIZE).setSpriteUV(20, 0)
            .setTooltip(new com.replaymod.gui.element.GuiTooltip().setI18nText("replaymod.gui.settings"))
            .onClick(() -> new GuiReplaySettings(toMinecraft(), getMod().getCore().getSettingsRegistry()).display());

    public final com.replaymod.gui.element.GuiButton cancelButton = new com.replaymod.gui.element.GuiButton().onClick(new Runnable() {
        @Override
        public void run() {
            getMinecraft().displayGuiScreen(null);
        }
    }).setSize(73, 20).setI18nLabel("replaymod.gui.cancel");

    public final List<com.replaymod.gui.element.GuiButton> replaySpecificButtons = new ArrayList<>();

    {
        replaySpecificButtons.addAll(Arrays.asList(renameButton));
    }

    public final GuiPanel editorButton = new GuiPanel();

    public final GuiPanel upperButtonPanel = new GuiPanel().setLayout(new HorizontalLayout().setSpacing(5))
            .addElements(null, loadButton);
    public final GuiPanel lowerButtonPanel = new GuiPanel().setLayout(new HorizontalLayout().setSpacing(5))
            .addElements(null, renameButton, deleteButton, editorButton, cancelButton);
    public final GuiPanel buttonPanel = new GuiPanel(this).setLayout(new VerticalLayout().setSpacing(5))
            .addElements(null, upperButtonPanel, lowerButtonPanel);

    public GuiReplayViewer(ReplayModReplay mod) {
        this.mod = mod;

        try {
            list.setFolder(mod.getCore().getReplayFolder().toFile());
        } catch (IOException e) {
            throw new ReportedException(CrashReport.makeCrashReport(e, "Getting replay folder"));
        }

        setTitle(new com.replaymod.gui.element.GuiLabel().setI18nText("replaymod.gui.replayviewer"));

        setLayout(new CustomLayout<GuiScreen>() {
            @Override
            protected void layout(GuiScreen container, int width, int height) {
                pos(buttonPanel, width / 2 - width(buttonPanel) / 2, height - 10 - height(buttonPanel));

                pos(list, 0, 30);
                size(list, width, y(buttonPanel) - 10 - y(list));

                pos(settingsButton, width - width(settingsButton) - 5, 5);
            }
        });

        updateButtons();
    }

    public ReplayModReplay getMod() {
        return mod;
    }

    private void updateButtons() {
        List<GuiReplayEntry> selected = list.getSelected();
        int count = selected.size();

        replaySpecificButtons.forEach(b -> b.setEnabled(count == 1));
        deleteButton.setEnabled(count > 0);
        if (count > 1) {
            Set<RenderJob> jobs = selected.stream().flatMap(entry -> entry.renderQueue.stream()).collect(Collectors.toSet());
            String[] tooltipLines = jobs.stream().map(RenderJob::getName).toArray(String[]::new);
            loadButton.setI18nLabel("replaymod.gui.viewer.bulkrender", jobs.size());
            loadButton.setTooltip(new com.replaymod.gui.element.GuiTooltip().setText(tooltipLines));
            loadButton.setEnabled(!jobs.isEmpty());

            String[] compatError = VideoRenderer.checkCompat();
            if (compatError != null) {
                loadButton.setDisabled().setTooltip(new com.replaymod.gui.element.GuiTooltip().setText(compatError));
            }
        } else {
            loadButton.setI18nLabel("replaymod.gui.load");
            loadButton.setTooltip(null);
            loadButton.setEnabled(count == 1 && !selected.get(0).incompatible);
        }
    }

    private static final com.replaymod.gui.element.GuiImage DEFAULT_THUMBNAIL = new com.replaymod.gui.element.GuiImage().setTexture(Utils.DEFAULT_THUMBNAIL);

    public static class GuiSelectReplayPopup extends AbstractGuiPopup<GuiSelectReplayPopup> {
        public static GuiSelectReplayPopup openGui(GuiContainer container, File folder) {
            GuiSelectReplayPopup popup = new GuiSelectReplayPopup(container, folder);
            popup.list.load();
            popup.open();
            return popup;
        }

        private final SettableFuture<File> future = SettableFuture.create();

        private final GuiReplayList list = new GuiReplayList(popup);

        private final com.replaymod.gui.element.GuiButton acceptButton = new com.replaymod.gui.element.GuiButton(popup).setI18nLabel("gui.done").setSize(50, 20).setDisabled();

        private final com.replaymod.gui.element.GuiButton cancelButton = new com.replaymod.gui.element.GuiButton(popup).setI18nLabel("gui.cancel").setSize(50, 20);


        public GuiSelectReplayPopup(GuiContainer container, File folder) {
            super(container);

            list.setFolder(folder);

            list.onSelectionChanged(() -> {
                acceptButton.setEnabled(list.getSelected() != null);
            }).onSelectionDoubleClicked(() -> {
                close();
                future.set(list.getSelected().get(0).file);
            });
            acceptButton.onClick(() -> {
                future.set(list.getSelected().get(0).file);
                close();
            });
            cancelButton.onClick(() -> {
                future.set(null);
                close();
            });

            popup.setLayout(new CustomLayout<GuiPanel>() {
                @Override
                protected void layout(GuiPanel container, int width, int height) {
                    pos(cancelButton, width - width(cancelButton), height - height(cancelButton));
                    pos(acceptButton, x(cancelButton) - 5 - width(acceptButton), y(cancelButton));
                    pos(list, 0, 5);
                    size(list, width, height - height(cancelButton) - 10);
                }

                @Override
                public ReadableDimension calcMinSize(GuiContainer container) {
                    return new Dimension(330, 200);
                }
            });
        }

        public SettableFuture<File> getFuture() {
            return future;
        }

        public GuiReplayList getList() {
            return list;
        }

        public com.replaymod.gui.element.GuiButton getAcceptButton() {
            return acceptButton;
        }

        public com.replaymod.gui.element.GuiButton getCancelButton() {
            return cancelButton;
        }

        @Override
        protected GuiSelectReplayPopup getThis() {
            return this;
        }
    }

    public static class GuiReplayList extends AbstractGuiResourceLoadingList<GuiReplayList, GuiReplayEntry> implements Typeable {
        private File folder = null;

        // Not actually a child of this element, we just use it for text manipulation
        private final com.replaymod.gui.element.GuiTextField filterTextField = new GuiTextField()
                .setFocused(true);

        public GuiReplayList(GuiContainer container) {
            super(container);
        }

        {
            onLoad((Consumer<Supplier<GuiReplayEntry>> results) -> {
                File[] files = folder.listFiles((FileFilter) new SuffixFileFilter(".mcpr", IOCase.INSENSITIVE));
                if (files == null) {
                    LOGGER.warn("Failed to list files in {}", folder);
                    return;
                }
                Map<File, Long> lastModified = new HashMap<>();
                Arrays.sort(files, Comparator.<File>comparingLong(f -> lastModified.computeIfAbsent(f, File::lastModified)).reversed());
                for (final File file : files) {
                    if (Thread.interrupted()) break;
                    try (ReplayFile replayFile = ReplayMod.instance.openReplay(file.toPath())) {
                        final Image thumb = Optional.ofNullable(replayFile.getThumbBytes().orNull()).flatMap(stream -> {
                            try (InputStream in = stream) {
                                return Optional.of(Image.read(in));
                            } catch (IOException e) {
                                e.printStackTrace();
                                return Optional.empty();
                            }
                        }).orElse(null);
                        final ReplayMetaData metaData = replayFile.getMetaData();
                        List<RenderJob> renderQueue = RenderJob.readQueue(replayFile);

                        if (metaData != null) {
                            results.consume(() -> new GuiReplayEntry(file, metaData, thumb, renderQueue) {
                                @Override
                                public ReadableDimension calcMinSize() {
                                    if (isFiltered(this)) {
                                        return new Dimension(-4, -4);
                                    }
                                    return super.calcMinSize();
                                }
                            });
                        }
                    } catch (Exception e) {
                        LOGGER.error("Could not load Replay File {}", file.getName(), e);
                    }
                }
            }).setDrawShadow(true).setDrawSlider(true);
        }

        public void setFolder(File folder) {
            this.folder = folder;
        }

        private boolean isFiltered(GuiReplayEntry entry) {
            String filter = filterTextField.getText().toLowerCase();
            if (filter.isEmpty()) {
                return false;
            }
            return !entry.name.getText().toLowerCase().contains(filter);
        }

        @Override
        public boolean typeKey(ReadablePoint mousePosition, int keyCode, char keyChar, boolean ctrlDown, boolean shiftDown) {
            if (keyCode == Keyboard.KEY_F1) {
                SettingsRegistry reg = ReplayMod.instance.getSettingsRegistry();
                reg.set(Setting.SHOW_SERVER_IPS, !reg.get(Setting.SHOW_SERVER_IPS));
                reg.save();
                load();
            }

            boolean filterHasPriority = !filterTextField.getText().isEmpty();
            if (filterHasPriority && filterTextField.typeKey(mousePosition, keyCode, keyChar, ctrlDown, shiftDown)) {
                scrollY(0); // ensure we scroll to top if most entries are filtered
                return true;
            }

            if (super.typeKey(mousePosition, keyCode, keyChar, ctrlDown, shiftDown)) {
                return true;
            }

            if (!filterHasPriority && filterTextField.typeKey(mousePosition, keyCode, keyChar, ctrlDown, shiftDown)) {
                scrollY(0); // ensure we scroll to top if most entries are filtered
                return true;
            }

            return false;
        }

        @Override
        public void draw(GuiRenderer renderer, ReadableDimension size, RenderInfo renderInfo) {
            super.draw(renderer, size, renderInfo);

            String filter = filterTextField.getText();
            if (!filter.isEmpty()) {
                boolean anyMatches = getListPanel().calcMinSize().getHeight() > 0;

                FontRenderer fontRenderer = getFontRenderer();
                int filterTextWidth = fontRenderer.getStringWidth(filter);
                int filterTextHeight = fontRenderer.FONT_HEIGHT;
                renderer.drawRect(
                        size.getWidth() - 3 - 2 - filterTextWidth - 2,
                        size.getHeight() - 3 - 2 - filterTextHeight - 2,
                        2 + filterTextWidth + 2,
                        2 + filterTextHeight + 2,
                        Colors.WHITE
                );
                renderer.drawString(
                        size.getWidth() - 3 - 2 - filterTextWidth,
                        size.getHeight() - 3 - 2 - filterTextHeight,
                        anyMatches ? Colors.BLACK : Colors.DARK_RED,
                        filter
                );
            }
        }

        @Override
        protected GuiReplayList getThis() {
            return this;
        }
    }

    public static class GuiReplayEntry extends AbstractGuiContainer<GuiReplayEntry> implements Comparable<GuiReplayEntry> {
        public final File file;
        public final com.replaymod.gui.element.GuiLabel name = new com.replaymod.gui.element.GuiLabel();
        public final com.replaymod.gui.element.GuiLabel server = new com.replaymod.gui.element.GuiLabel().setColor(Colors.LIGHT_GRAY);
        public final com.replaymod.gui.element.GuiLabel date = new com.replaymod.gui.element.GuiLabel().setColor(Colors.LIGHT_GRAY);
        public final GuiPanel infoPanel = new GuiPanel(this).setLayout(new VerticalLayout().setSpacing(2))
                .addElements(null, name, server, date);
        public final com.replaymod.gui.element.GuiLabel version = new com.replaymod.gui.element.GuiLabel(this).setColor(Colors.RED);
        public final com.replaymod.gui.element.GuiImage thumbnail;
        public final com.replaymod.gui.element.GuiLabel duration = new com.replaymod.gui.element.GuiLabel();
        public final GuiPanel durationPanel = new GuiPanel().setBackgroundColor(Colors.HALF_TRANSPARENT)
                .addElements(null, duration).setLayout(new CustomLayout<GuiPanel>() {
                    @Override
                    protected void layout(GuiPanel container, int width, int height) {
                        pos(duration, 2, 2);
                    }

                    @Override
                    public ReadableDimension calcMinSize(GuiContainer<?> container) {
                        ReadableDimension dimension = duration.calcMinSize();
                        return new Dimension(dimension.getWidth() + 2, dimension.getHeight() + 2);
                    }
                });
        public final com.replaymod.gui.element.GuiImage renderQueueIcon = new com.replaymod.gui.element.GuiImage()
                .setSize(10, 10)
                .setTexture(ReplayMod.TEXTURE, 40, 0, 20, 20);

        private final long dateMillis;
        private final boolean incompatible;
        private final List<RenderJob> renderQueue;

        public GuiReplayEntry(File file, ReplayMetaData metaData, Image thumbImage, List<RenderJob> renderQueue) {
            this.file = file;
            this.renderQueue = renderQueue;

            name.setText(TextFormatting.UNDERLINE + Utils.fileNameToReplayName(file.getName()));
            if (!StringUtils.isEmpty(metaData.getCustomServerName())) {
                server.setText(metaData.getCustomServerName());
            } else if (StringUtils.isEmpty(metaData.getServerName())
                    || !ReplayMod.instance.getSettingsRegistry().get(Setting.SHOW_SERVER_IPS)) {
                server.setI18nText("replaymod.gui.iphidden").setColor(Colors.DARK_RED);
            } else {
                server.setText(metaData.getServerName());
            }
            incompatible = !ReplayMod.isCompatible(metaData.getFileFormatVersion(), metaData.getRawProtocolVersionOr0());
            if (incompatible) {
                version.setText("Minecraft " + metaData.getMcVersion());
            }
            dateMillis = metaData.getDate();
            date.setText(new SimpleDateFormat().format(new Date(dateMillis)));
            if (thumbImage == null) {
                thumbnail = new com.replaymod.gui.element.GuiImage(DEFAULT_THUMBNAIL).setSize(30 * 16 / 9, 30);
                addElements(null, thumbnail);
            } else {
                thumbnail = new com.replaymod.gui.element.GuiImage(this).setTexture(thumbImage).setSize(30 * 16 / 9, 30);
            }
            duration.setText(Utils.convertSecondsToShortString(metaData.getDuration() / 1000));
            addElements(null, durationPanel);

            if (!renderQueue.isEmpty()) {
                renderQueueIcon.setTooltip(new com.replaymod.gui.element.GuiTooltip()
                        .setText(renderQueue.stream().map(RenderJob::getName).toArray(String[]::new)));
                addElements(null, renderQueueIcon);
            }

            setLayout(new CustomLayout<GuiReplayEntry>() {
                @Override
                protected void layout(GuiReplayEntry container, int width, int height) {
                    pos(thumbnail, 0, 0);
                    x(durationPanel, width(thumbnail) - width(durationPanel));
                    y(durationPanel, height(thumbnail) - height(durationPanel));

                    pos(infoPanel, width(thumbnail) + 5, 0);
                    pos(version, width - width(version), 0);

                    if (renderQueueIcon.getContainer() != null) {
                        pos(renderQueueIcon, width(thumbnail) - width(renderQueueIcon), 0);
                    }
                }

                @Override
                public ReadableDimension calcMinSize(GuiContainer<?> container) {
                    return new Dimension(300, thumbnail.getMinSize().getHeight());
                }
            });
        }

        @Override
        protected GuiReplayEntry getThis() {
            return this;
        }

        @Override
        public int compareTo(GuiReplayEntry o) {
            return Long.compare(o.dateMillis, dateMillis);
        }
    }
}
