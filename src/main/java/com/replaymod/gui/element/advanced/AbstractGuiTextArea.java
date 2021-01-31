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
package com.replaymod.gui.element.advanced;

import com.replaymod.gui.GuiRenderer;
import com.replaymod.gui.RenderInfo;
import com.replaymod.gui.container.GuiContainer;
import com.replaymod.gui.element.AbstractGuiElement;
import com.replaymod.gui.function.Clickable;
import com.replaymod.gui.function.Focusable;
import com.replaymod.gui.function.Tickable;
import com.replaymod.gui.function.Typeable;
import com.replaymod.gui.utils.Consumer;
import de.johni0702.minecraft.gui.utils.lwjgl.*;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.SharedConstants;

import java.util.Arrays;

import static com.replaymod.gui.utils.Utils.clamp;

public abstract class AbstractGuiTextArea<T extends AbstractGuiTextArea<T>>
        extends AbstractGuiElement<T> implements Clickable, Typeable, Tickable, IGuiTextArea<T> {
    private static final ReadableColor BACKGROUND_COLOR = new Color(160, 160, 160);
    private static final ReadableColor CURSOR_COLOR = new Color(240, 240, 240);
    private static final int BORDER = 4;
    private static final int LINE_SPACING = 2;

    private boolean focused;
    private com.replaymod.gui.function.Focusable next, previous;

    private Consumer<Boolean> focusChanged;

    // Content
    private int maxTextWidth = -1;
    private int maxTextHeight = -1;
    private int maxCharCount = -1;

    private String[] text = {""};
    private String[] hint;
    private int cursorX;
    private int cursorY;
    private int selectionX;
    private int selectionY;

    // Rendering
    private int currentXOffset;
    private int currentYOffset;
    private int blinkCursorTick;
    public ReadableColor textColorEnabled = new Color(224, 224, 224);
    public ReadableColor textColorDisabled = new Color(112, 112, 112);
    private ReadableDimension size = new Dimension(0, 0); // Size of last render

    public AbstractGuiTextArea() {
    }

    public AbstractGuiTextArea(GuiContainer container) {
        super(container);
    }

    @Override
    public T setText(String[] lines) {
        if (lines.length > maxTextHeight) {
            lines = Arrays.copyOf(lines, maxTextHeight);
        }
        this.text = lines;
        for (int i = 0; i < lines.length; i++) {
            if (lines[i].length() > maxTextWidth) {
                lines[i] = lines[i].substring(0, maxTextWidth);
            }
        }
        return getThis();
    }

    @Override
    public String[] getText() {
        return this.text;
    }

    @Override
    public String getText(int fromX, int fromY, int toX, int toY) {
        StringBuilder sb = new StringBuilder();
        if (fromY == toY) {
            sb.append(text[fromY].substring(fromX, toX));
        } else {
            sb.append(text[fromY].substring(fromX)).append('\n');
            for (int y = fromY + 1; y < toY; y++) {
                sb.append(text[y]).append('\n');
            }
            sb.append(text[toY].substring(0, toX));
        }
        return sb.toString();
    }

    private void deleteText(int fromX, int fromY, int toX, int toY) {
        String[] newText = new String[text.length - (toY - fromY)];
        if (fromY > 0) {
            System.arraycopy(text, 0, newText, 0, fromY);
        }

        newText[fromY] = text[fromY].substring(0, fromX) + text[toY].substring(toX);

        if (toY + 1 < text.length) {
            System.arraycopy(text, toY + 1, newText, fromY + 1, text.length - toY - 1);
        }
        text = newText;
    }

    @Override
    public int getSelectionFromX() {
        if (cursorY == selectionY) {
            return cursorX > selectionX ? selectionX : cursorX;
        }
        return cursorY > selectionY ? selectionX : cursorX;
    }

    @Override
    public int getSelectionToX() {
        if (cursorY == selectionY) {
            return cursorX > selectionX ? cursorX : selectionX;
        }
        return cursorY > selectionY ? cursorX : selectionX;
    }

    @Override
    public int getSelectionFromY() {
        return cursorY > selectionY ? selectionY : cursorY;
    }

    @Override
    public int getSelectionToY() {
        return cursorY > selectionY ? cursorY : selectionY;
    }

    @Override
    public String getSelectedText() {
        if (cursorX == selectionX && cursorY == selectionY) {
            return "";
        }
        int fromX = getSelectionFromX();
        int fromY = getSelectionFromY();
        int toX = getSelectionToX();
        int toY = getSelectionToY();
        return getText(fromX, fromY, toX, toY);
    }

    @Override
    public void deleteSelectedText() {
        if (cursorX == selectionX && cursorY == selectionY) {
            return;
        }
        int fromX = getSelectionFromX();
        int fromY = getSelectionFromY();
        int toX = getSelectionToX();
        int toY = getSelectionToY();
        deleteText(fromX, fromY, toX, toY);
        cursorX = selectionX = fromX;
        cursorY = selectionY = fromY;
    }

    private void updateCurrentOffset() {
        currentXOffset = Math.min(currentXOffset, cursorX);
        String line = text[cursorY].substring(currentXOffset, cursorX);
        FontRenderer fontRenderer = com.replaymod.gui.versions.MCVer.getFontRenderer();
        int currentWidth = fontRenderer.getStringWidth(line);
        if (currentWidth > size.getWidth() - BORDER * 2) {
            currentXOffset = cursorX - fontRenderer.getLineScrollOffset(line, size.getWidth() - BORDER * 2, true).length();
        }

        currentYOffset = Math.min(currentYOffset, cursorY);
        int lineHeight = com.replaymod.gui.versions.MCVer.getFontRenderer().FONT_HEIGHT + LINE_SPACING;
        int contentHeight = size.getHeight() - BORDER * 2;
        int maxLines = contentHeight / lineHeight;
        if (cursorY - currentYOffset >= maxLines) {
            currentYOffset = cursorY - maxLines + 1;
        }
    }

    @Override
    public String cutSelectedText() {
        String selection = getSelectedText();
        deleteSelectedText();
        return selection;
    }

    @Override
    public void writeText(String append) {
        for (char c : append.toCharArray()) {
            writeChar(c);
        }
    }

    @Override
    public void writeChar(char c) {
        if (!SharedConstants.isAllowedCharacter(c)) {
            return;
        }

        int totalCharCount = 0;
        for (String line : text) {
            totalCharCount += line.length();
        }

        if (maxCharCount > 0 && totalCharCount - (getSelectedText().length()) >= maxCharCount) {
            return;
        }

        deleteSelectedText();

        if (c == '\n') {
            if (text.length >= maxTextHeight) {
                return;
            }
            String[] newText = new String[text.length + 1];
            if (cursorY > 0) {
                System.arraycopy(text, 0, newText, 0, cursorY);
            }

            newText[cursorY] = text[cursorY].substring(0, cursorX);
            newText[cursorY + 1] = text[cursorY].substring(cursorX);

            if (cursorY + 1 < text.length) {
                System.arraycopy(text, cursorY + 1, newText, cursorY + 2, text.length - cursorY - 1);
            }
            text = newText;
            selectionX = cursorX = 0;
            selectionY = ++cursorY;
        } else {
            String line = text[cursorY];
            if (line.length() >= maxTextWidth) {
                return;
            }

            line = line.substring(0, cursorX) + c + line.substring(cursorX);
            text[cursorY] = line;
            selectionX = ++cursorX;
        }
    }

    private void deleteNextChar() {
        String line = text[cursorY];
        if (cursorX < line.length()) {
            line = line.substring(0, cursorX) + line.substring(cursorX + 1);
            text[cursorY] = line;
        } else if (cursorY + 1 < text.length) {
            deleteText(cursorX, cursorY, 0, cursorY + 1);
        }
    }

    private int getNextWordLength() {
        int length = 0;
        String line = text[cursorY];
        boolean inWord = true;
        for (int i = cursorX; i < line.length(); i++) {
            if (inWord) {
                if (line.charAt(i) == ' ') {
                    inWord = false;
                }
            } else {
                if (line.charAt(i) != ' ') {
                    return length;
                }
            }
            length++;
        }
        return length;
    }

    private void deleteNextWord() {
        int worldLength = getNextWordLength();
        if (worldLength == 0) {
            deleteNextChar();
        } else {
            deleteText(cursorX, cursorY, cursorX + worldLength, cursorY);
        }
    }

    private void deletePreviousChar() {
        if (cursorX > 0) {
            String line = text[cursorY];
            line = line.substring(0, cursorX - 1) + line.substring(cursorX);
            selectionX = --cursorX;
            text[cursorY] = line;
        } else if (cursorY > 0) {
            int fromX = text[cursorY - 1].length();
            deleteText(fromX, cursorY - 1, cursorX, cursorY);
            selectionX = cursorX = fromX;
            selectionY = --cursorY;
        }
    }

    private int getPreviousWordLength() {
        int length = 0;
        String line = text[cursorY];
        boolean inWord = false;
        for (int i = cursorX - 1; i >= 0; i--) {
            if (inWord) {
                if (line.charAt(i) == ' ') {
                    return length;
                }
            } else {
                if (line.charAt(i) != ' ') {
                    inWord = true;
                }
            }
            length++;
        }
        return length;
    }

    private void deletePreviousWord() {
        int worldLength = getPreviousWordLength();
        if (worldLength == 0) {
            deletePreviousChar();
        } else {
            deleteText(cursorX, cursorY, cursorX - worldLength, cursorY);
            selectionX = cursorX -= worldLength;
        }
    }

    @Override
    public T setCursorPosition(int x, int y) {
        selectionY = cursorY = clamp(y, 0, text.length - 1);
        selectionX = cursorX = clamp(x, 0, text[cursorY].length());
        return getThis();
    }

    @Override
    protected ReadableDimension calcMinSize() {
        return new Dimension(0, 0);
    }

    @Override
    public boolean mouseClick(ReadablePoint position, int button) {
        if (getContainer() != null) {
            getContainer().convertFor(this, (Point) (position = new Point(position)));
        }
        boolean hovering = isMouseHovering(position);

        if (hovering && isFocused() && button == 0) {
            updateCurrentOffset();
            int mouseX = position.getX() - BORDER;
            int mouseY = position.getY() - BORDER;
            FontRenderer fontRenderer = com.replaymod.gui.versions.MCVer.getFontRenderer();
            int textY = clamp(mouseY / (fontRenderer.FONT_HEIGHT + LINE_SPACING) + currentYOffset, 0, text.length - 1);
            if (cursorY != textY) {
                currentXOffset = 0;
            }
            String line = text[textY].substring(currentXOffset);
            int textX = fontRenderer.trimStringToWidth(line, mouseX).length() + currentXOffset;
            setCursorPosition(textX, textY);
        }

        setFocused(hovering);
        return hovering;
    }

    protected boolean isMouseHovering(ReadablePoint pos) {
        return pos.getX() > 0 && pos.getY() > 0
                && pos.getX() < size.getWidth() && pos.getY() < size.getHeight();
    }

    @Override
    public T setFocused(boolean isFocused) {
        if (isFocused && !this.focused) {
            this.blinkCursorTick = 0; // Restart blinking to indicate successful focus
        }
        if (this.focused != isFocused) {
            this.focused = isFocused;
            onFocusChanged(this.focused);
        }
        return getThis();
    }

    @Override
    public T setNext(com.replaymod.gui.function.Focusable next) {
        this.next = next;
        return getThis();
    }

    @Override
    public T setPrevious(com.replaymod.gui.function.Focusable previous) {
        this.previous = previous;
        return getThis();
    }

    @Override
    public void draw(GuiRenderer renderer, ReadableDimension size, RenderInfo renderInfo) {
        this.size = size;
        updateCurrentOffset();
        super.draw(renderer, size, renderInfo);

        FontRenderer fontRenderer = com.replaymod.gui.versions.MCVer.getFontRenderer();
        int width = size.getWidth();
        int height = size.getHeight();

        // Draw black rect once pixel smaller than gray rect
        renderer.drawRect(0, 0, width, height, BACKGROUND_COLOR);
        renderer.drawRect(1, 1, width - 2, height - 2, ReadableColor.BLACK);

        ReadableColor textColor = isEnabled() ? textColorEnabled : textColorDisabled;

        int lineHeight = fontRenderer.FONT_HEIGHT + LINE_SPACING;
        int contentHeight = height - BORDER * 2;
        int maxLines = contentHeight / lineHeight;
        int contentWidth = width - BORDER * 2;

        // Draw hint if applicable
        if (hint != null && !isFocused() && Arrays.stream(text).allMatch(String::isEmpty)) {
            for (int i = 0; i < maxLines && i < hint.length; i++) {
                String line = fontRenderer.trimStringToWidth(hint[i], contentWidth);

                int posY = BORDER + i * lineHeight;
                renderer.drawString(BORDER, posY, textColorDisabled, line, true);
            }
            return;
        }

        // Draw lines
        for (int i = 0; i < maxLines && i + currentYOffset < text.length; i++) {
            int lineY = i + currentYOffset;
            String line = text[lineY];
            int leftTrimmed = 0;
            if (lineY == cursorY) {
                line = line.substring(currentXOffset);
                leftTrimmed = currentXOffset;
            }
            line = fontRenderer.trimStringToWidth(line, contentWidth);

            // Draw line
            int posY = BORDER + i * lineHeight;
            int lineEnd = renderer.drawString(BORDER, posY, textColor, line, true);

            // Draw selection
            int fromX = getSelectionFromX();
            int fromY = getSelectionFromY();
            int toX = getSelectionToX();
            int toY = getSelectionToY();
            if (lineY > fromY && lineY < toY) { // Whole line selected
                renderer.invertColors(lineEnd, posY - 1 + lineHeight, BORDER, posY - 1);
            } else if (lineY == fromY && lineY == toY) { // Part of line selected
                String leftStr = line.substring(0, clamp(fromX - leftTrimmed, 0, line.length()));
                String rightStr = line.substring(clamp(toX - leftTrimmed, 0, line.length()));
                int left = BORDER + fontRenderer.getStringWidth(leftStr);
                int right = lineEnd - fontRenderer.getStringWidth(rightStr) - 1;
                renderer.invertColors(right, posY - 1 + lineHeight, left, posY - 1);
            } else if (lineY == fromY) { // End of line selected
                String rightStr = line.substring(clamp(fromX - leftTrimmed, 0, line.length()));
                renderer.invertColors(lineEnd, posY - 1 + lineHeight, lineEnd - fontRenderer.getStringWidth(rightStr), posY - 1);
            } else if (lineY == toY) { // Beginning of line selected
                String leftStr = line.substring(0, clamp(toX - leftTrimmed, 0, line.length()));
                int right = BORDER + fontRenderer.getStringWidth(leftStr);
                renderer.invertColors(right, posY - 1 + lineHeight, BORDER, posY - 1);
            }

            // Draw cursor
            if (lineY == cursorY && blinkCursorTick / 6 % 2 == 0 && focused) {
                String beforeCursor = line.substring(0, cursorX - leftTrimmed);
                int posX = BORDER + fontRenderer.getStringWidth(beforeCursor);
                if (cursorX == text[lineY].length()) {
                    renderer.drawString(posX, posY, CURSOR_COLOR, "_", true);
                } else {
                    renderer.drawRect(posX, posY - 1, 1, 1 + fontRenderer.FONT_HEIGHT, CURSOR_COLOR);
                }
            }
        }
    }

    @Override
    public boolean typeKey(ReadablePoint mousePosition, int keyCode, char keyChar, boolean ctrlDown, boolean shiftDown) {
        if (keyCode == com.replaymod.gui.versions.MCVer.Keyboard.KEY_TAB) {
            com.replaymod.gui.function.Focusable other = shiftDown ? previous : next;
            if (other != null) {
                setFocused(false);
                other.setFocused(true);
            }
            return true;
        }

        if (!this.focused) {
            return false;
        }

        if (Screen.hasControlDown()) {
            switch (keyCode) {
                case com.replaymod.gui.versions.MCVer.Keyboard.KEY_A: // Select all
                    cursorX = cursorY = 0;
                    selectionY = text.length - 1;
                    selectionX = text[selectionY].length();
                    return true;
                case com.replaymod.gui.versions.MCVer.Keyboard.KEY_C: // Copy
                    com.replaymod.gui.versions.MCVer.setClipboardString(getSelectedText());
                    return true;
                case com.replaymod.gui.versions.MCVer.Keyboard.KEY_V: // Paste
                    if (isEnabled()) {
                        writeText(com.replaymod.gui.versions.MCVer.getClipboardString());
                    }
                    return true;
                case com.replaymod.gui.versions.MCVer.Keyboard.KEY_X: // Cut
                    if (isEnabled()) {
                        com.replaymod.gui.versions.MCVer.setClipboardString(cutSelectedText());
                    }
                    return true;
            }
        }

        boolean words = Screen.hasControlDown();
        boolean select = Screen.hasShiftDown();
        switch (keyCode) {
            case com.replaymod.gui.versions.MCVer.Keyboard.KEY_HOME:
                cursorX = 0;
                break;
            case com.replaymod.gui.versions.MCVer.Keyboard.KEY_END:
                cursorX = text[cursorY].length();
                break;
            case com.replaymod.gui.versions.MCVer.Keyboard.KEY_LEFT:
                if (cursorX == 0) {
                    if (cursorY > 0) {
                        cursorY--;
                        cursorX = text[cursorY].length();
                    }
                } else {
                    if (words) {
                        cursorX -= getPreviousWordLength();
                    } else {
                        cursorX--;
                    }
                }
                break;
            case com.replaymod.gui.versions.MCVer.Keyboard.KEY_RIGHT:
                if (cursorX == text[cursorY].length()) {
                    if (cursorY < text.length - 1) {
                        cursorY++;
                        cursorX = 0;
                    }
                } else {
                    if (words) {
                        cursorX += getNextWordLength();
                    } else {
                        cursorX++;
                    }
                }
                break;
            case com.replaymod.gui.versions.MCVer.Keyboard.KEY_UP:
                if (cursorY > 0) {
                    cursorY--;
                    cursorX = Math.min(cursorX, text[cursorY].length());
                }
                break;
            case com.replaymod.gui.versions.MCVer.Keyboard.KEY_DOWN:
                if (cursorY + 1 < text.length) {
                    cursorY++;
                    cursorX = Math.min(cursorX, text[cursorY].length());
                }
                break;
            case com.replaymod.gui.versions.MCVer.Keyboard.KEY_BACK:
                if (isEnabled()) {
                    if (getSelectedText().length() > 0) {
                        deleteSelectedText();
                    } else if (words) {
                        deletePreviousWord();
                    } else {
                        deletePreviousChar();
                    }
                }
                return true;
            case com.replaymod.gui.versions.MCVer.Keyboard.KEY_DELETE:
                if (isEnabled()) {
                    if (getSelectedText().length() > 0) {
                        deleteSelectedText();
                    } else if (words) {
                        deleteNextWord();
                    } else {
                        deleteNextChar();
                    }
                }
                return true;
            default:
                if (isEnabled()) {
                    if (keyChar == '\r') {
                        keyChar = '\n';
                    }
                    writeChar(keyChar);
                }
                return true;
        }

        if (!select) {
            selectionX = cursorX;
            selectionY = cursorY;
        }
        return true;
    }

    @Override
    public void tick() {
        blinkCursorTick++;
    }

    @Override
    public T setMaxTextWidth(int maxTextWidth) {
        this.maxTextWidth = maxTextWidth;
        return getThis();
    }

    @Override
    public T setMaxTextHeight(int maxTextHeight) {
        this.maxTextHeight = maxTextHeight;
        return getThis();
    }

    @Override
    public T setMaxCharCount(int maxCharCount) {
        this.maxCharCount = maxCharCount;
        return getThis();
    }

    @Override
    public T setTextColor(ReadableColor textColor) {
        this.textColorEnabled = textColor;
        return getThis();
    }

    @Override
    public T setTextColorDisabled(ReadableColor textColorDisabled) {
        this.textColorDisabled = textColorDisabled;
        return getThis();
    }

    @Override
    public T onFocusChange(Consumer<Boolean> focusChanged) {
        this.focusChanged = focusChanged;
        return getThis();
    }

    /**
     * Called when the element has been focused or unfocused
     */
    protected void onFocusChanged(boolean focused) {
        if (focusChanged != null) {
            focusChanged.consume(focused);
        }
    }

    @Override
    public String[] getHint() {
        return hint;
    }

    @Override
    public T setHint(String... hint) {
        this.hint = hint;
        return getThis();
    }

    @Override
    public T setI18nHint(String hint, Object... args) {
        setHint(I18n.format(hint, args).split("/n"));
        return getThis();
    }

    public boolean isFocused() {
        return this.focused;
    }

    public com.replaymod.gui.function.Focusable getNext() {
        return this.next;
    }

    public Focusable getPrevious() {
        return this.previous;
    }

    public int getMaxTextWidth() {
        return this.maxTextWidth;
    }

    public int getMaxTextHeight() {
        return this.maxTextHeight;
    }

    public int getMaxCharCount() {
        return this.maxCharCount;
    }
}
