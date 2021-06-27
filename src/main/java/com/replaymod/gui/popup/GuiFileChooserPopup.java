/*
 * This file is part of jGui API, licensed under the MIT License (MIT).
 *
 * Copyright (c) 2016 johni0702 <https://github.com/johni0702>
 * Copyright (c) contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.replaymod.gui.popup;

import com.replaymod.gui.GuiRenderer;
import com.replaymod.gui.RenderInfo;
import com.replaymod.gui.container.GuiContainer;
import com.replaymod.gui.container.GuiPanel;
import com.replaymod.gui.container.GuiScrollable;
import com.replaymod.gui.container.GuiVerticalList;
import com.replaymod.gui.element.GuiButton;
import com.replaymod.gui.element.GuiElement;
import com.replaymod.gui.element.GuiTextField;
import com.replaymod.gui.element.advanced.GuiDropdownMenu;
import com.replaymod.gui.function.Typeable;
import com.replaymod.gui.layout.CustomLayout;
import com.replaymod.gui.layout.HorizontalLayout;
import com.replaymod.gui.layout.VerticalLayout;
import com.replaymod.gui.utils.Colors;
import com.replaymod.gui.utils.Consumer;
import de.johni0702.minecraft.gui.utils.lwjgl.Dimension;
import de.johni0702.minecraft.gui.utils.lwjgl.ReadableDimension;
import de.johni0702.minecraft.gui.utils.lwjgl.ReadablePoint;

import java.io.File;
import java.io.IOException;
import java.nio.file.InvalidPathException;
import java.util.*;


public class GuiFileChooserPopup extends AbstractGuiPopup<GuiFileChooserPopup> implements Typeable {
    public static GuiFileChooserPopup openSaveGui(com.replaymod.gui.container.GuiContainer container, String buttonLabel, String... fileExtensions) {
        GuiFileChooserPopup popup = new GuiFileChooserPopup(container, fileExtensions, false).setBackgroundColor(com.replaymod.gui.utils.Colors.DARK_TRANSPARENT);
        popup.acceptButton.setI18nLabel(buttonLabel);
        popup.open();
        return popup;
    }

    public static GuiFileChooserPopup openLoadGui(com.replaymod.gui.container.GuiContainer container, String buttonLabel, String... fileExtensions) {
        GuiFileChooserPopup popup = new GuiFileChooserPopup(container, fileExtensions, true).setBackgroundColor(Colors.DARK_TRANSPARENT);
        popup.acceptButton.setI18nLabel(buttonLabel).setDisabled();
        popup.open();
        return popup;
    }

    private com.replaymod.gui.utils.Consumer<File> onAccept = (file) -> {
    };
    private Runnable onCancel = () -> {
    };

    private final com.replaymod.gui.container.GuiScrollable pathScrollable = new GuiScrollable(popup) {
        @Override
        public void draw(com.replaymod.gui.GuiRenderer renderer, ReadableDimension size, com.replaymod.gui.RenderInfo renderInfo) {
            scrollX(0);
            super.draw(renderer, size, renderInfo);
        }
    };
    private final com.replaymod.gui.container.GuiPanel pathPanel = new com.replaymod.gui.container.GuiPanel(pathScrollable).setLayout(new HorizontalLayout());
    private final com.replaymod.gui.container.GuiVerticalList fileList = new GuiVerticalList(popup);
    private final com.replaymod.gui.element.GuiTextField nameField = new GuiTextField(popup).onEnter(new Runnable() {
        @Override
        public void run() {
            if (acceptButton.isEnabled()) {
                acceptButton.onClick();
            }
        }
    }).onTextChanged(new com.replaymod.gui.utils.Consumer<String>() {
        @Override
        public void consume(String oldName) {
            updateButton();
        }
    }).setMaxLength(Integer.MAX_VALUE);

    private final com.replaymod.gui.element.GuiButton acceptButton = new com.replaymod.gui.element.GuiButton(popup).onClick(new Runnable() {
        @Override
        public void run() {
            String fileName = nameField.getText();
            if (!load && fileExtensions.length > 0) {
                if (!hasValidExtension(fileName)) {
                    fileName = fileName + "." + fileExtensions[0];
                }
            }
            onAccept.consume(new File(folder, fileName));
            close();
        }
    }).setSize(50, 20);

    private final com.replaymod.gui.element.GuiButton cancelButton = new com.replaymod.gui.element.GuiButton(popup).onClick(new Runnable() {
        @Override
        public void run() {
            onCancel.run();
            close();
        }
    }).setI18nLabel("gui.cancel").setSize(50, 20);

    {
        fileList.setLayout(new com.replaymod.gui.layout.VerticalLayout().setSpacing(1));
        popup.setLayout(new CustomLayout<com.replaymod.gui.container.GuiPanel>() {
            @Override
            protected void layout(GuiPanel container, int width, int height) {
                pos(pathScrollable, 0, 0);
                size(pathScrollable, width, 20);
                pos(cancelButton, width - width(cancelButton), height - height(cancelButton));
                pos(acceptButton, x(cancelButton) - 5 - width(acceptButton), y(cancelButton));
                size(nameField, x(acceptButton) - 5, 20);
                pos(nameField, 0, height - height(nameField));
                pos(fileList, 0, y(pathScrollable) + height(pathScrollable) + 5);
                size(fileList, width, y(nameField) - y(fileList) - 5);
            }

            @Override
            public ReadableDimension calcMinSize(com.replaymod.gui.container.GuiContainer container) {
                return new Dimension(300, 200);
            }
        });
    }

    private final String[] fileExtensions;
    private final boolean load;

    private File folder;

    public GuiFileChooserPopup(GuiContainer container, String[] fileExtensions, boolean load) {
        super(container);
        this.fileExtensions = fileExtensions;
        this.load = load;

        setFolder(new File("."));
    }

    protected void updateButton() {
        String name = nameField.getText();
        File file = new File(folder, name);

        // File name must not contain
        boolean valid = !name.contains(File.separator);

        // Name must not contain any illegal characters (depends on OS)
        try {
            //noinspection ResultOfMethodCallIgnored
            file.toPath();
        } catch (InvalidPathException ignored) {
            valid = false;
        }

        // If we're loading, the file must exist
        if (load) {
            valid &= file.exists();
        }

        acceptButton.setEnabled(valid);
    }

    public void setFolder(File folder) {
        if (!folder.isDirectory()) {
            throw new IllegalArgumentException("Folder has to be a directory.");
        }
        try {
            this.folder = folder = folder.getCanonicalFile();
        } catch (IOException e) {
            close();
            throw new RuntimeException(e);
        }

        updateButton();

        for (com.replaymod.gui.element.GuiElement element : new ArrayList<>(pathPanel.getElements().keySet())) {
            pathPanel.removeElement(element);
        }
        for (GuiElement element : new ArrayList<>(fileList.getListPanel().getElements().keySet())) {
            fileList.getListPanel().removeElement(element);
        }

        File[] files = folder.listFiles();
        if (files != null) {
            Arrays.sort(files, new Comparator<File>() {
                @Override
                public int compare(File f1, File f2) {
                    if (f1.isDirectory() && !f2.isDirectory()) {
                        return -1;
                    } else if (!f1.isDirectory() && f2.isDirectory()) {
                        return 1;
                    }
                    return f1.getName().compareToIgnoreCase(f2.getName());
                }
            });
            for (final File file : files) {
                if (file.isDirectory()) {
                    fileList.getListPanel().addElements(new com.replaymod.gui.layout.VerticalLayout.Data(0), new com.replaymod.gui.element.GuiButton().onClick(new Runnable() {
                        @Override
                        public void run() {
                            setFolder(file);
                        }
                    }).setLabel(file.getName() + File.separator));
                } else {
                    if (hasValidExtension(file.getName())) {
                        fileList.getListPanel().addElements(new VerticalLayout.Data(0), new com.replaymod.gui.element.GuiButton().onClick(new Runnable() {
                            @Override
                            public void run() {
                                setFileName(file.getName());
                            }
                        }).setLabel(file.getName()));
                    }
                }
            }
        }
        fileList.setOffsetY(0);

        File[] roots = File.listRoots();
        if (roots != null && roots.length > 1) {
            // Windows can have multiple file system roots
            // So we place a dropdown menu (skinned like a button) at the front of the path
            final com.replaymod.gui.element.advanced.GuiDropdownMenu<File> dropdown = new GuiDropdownMenu<File>(pathPanel) {
                private final com.replaymod.gui.element.GuiButton skin = new com.replaymod.gui.element.GuiButton();

                @Override
                protected ReadableDimension calcMinSize() {
                    ReadableDimension dim = super.calcMinSize();
                    return new Dimension(dim.getWidth() - 5 - com.replaymod.gui.versions.MCVer.getFontRenderer().FONT_HEIGHT,
                            dim.getHeight());
                }

                @Override
                public void layout(ReadableDimension size, com.replaymod.gui.RenderInfo renderInfo) {
                    super.layout(size, renderInfo);
                    if (renderInfo.layer == 0) {
                        skin.layout(size, renderInfo);
                    }
                }

                @Override
                public void draw(GuiRenderer renderer, ReadableDimension size, RenderInfo renderInfo) {
                    super.draw(renderer, size, renderInfo);
                    if (renderInfo.layer == 0) {
                        skin.setLabel(getSelectedValue().toString());
                        skin.draw(renderer, size, renderInfo);
                    }
                }
            };
            List<File> actualRoots = new ArrayList<>();
            File selected = null;
            for (File root : roots) {
                // Windows apparently also has file system roots that aren't directories, so we'll have to filter those
                if (root.isDirectory()) {
                    actualRoots.add(root);
                    if (folder.getAbsolutePath().startsWith(root.getAbsolutePath())) {
                        selected = root;
                    }
                }
            }
            assert selected != null;
            // First set values and current selection
            dropdown.setValues(actualRoots.toArray(new File[actualRoots.size()])).setSelected(selected);
            // then add selection handler afterwards
            dropdown.onSelection(new com.replaymod.gui.utils.Consumer<Integer>() {
                @Override
                public void consume(Integer old) {
                    setFolder(dropdown.getSelectedValue());
                }
            });
        }
        LinkedList<File> parents = new LinkedList<>();
        while (folder != null) {
            parents.addFirst(folder);
            folder = folder.getParentFile();
        }
        for (final File parent : parents) {
            pathPanel.addElements(null, new com.replaymod.gui.element.GuiButton().onClick(new Runnable() {
                @Override
                public void run() {
                    setFolder(parent);
                }
            }).setLabel(parent.getName() + File.separator));
        }
        pathScrollable.setOffsetX(Integer.MAX_VALUE);
    }

    public void setFileName(String fileName) {
        this.nameField.setText(fileName);
        this.nameField.setCursorPosition(fileName.length());
        updateButton();
    }

    private boolean hasValidExtension(String name) {
        for (String fileExtension : fileExtensions) {
            if (name.endsWith("." + fileExtension)) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected GuiFileChooserPopup getThis() {
        return this;
    }

    public GuiFileChooserPopup onAccept(Consumer<File> onAccept) {
        this.onAccept = onAccept;
        return this;
    }

    public GuiFileChooserPopup onCancel(Runnable onCancel) {
        this.onCancel = onCancel;
        return this;
    }

    @Override
    public boolean typeKey(ReadablePoint mousePosition, int keyCode, char keyChar, boolean ctrlDown, boolean shiftDown) {
        if (keyCode == com.replaymod.gui.versions.MCVer.Keyboard.KEY_ESCAPE) {
            cancelButton.onClick();
            return true;
        }
        return false;
    }

    public com.replaymod.gui.element.GuiButton getAcceptButton() {
        return this.acceptButton;
    }

    public GuiButton getCancelButton() {
        return this.cancelButton;
    }
}
