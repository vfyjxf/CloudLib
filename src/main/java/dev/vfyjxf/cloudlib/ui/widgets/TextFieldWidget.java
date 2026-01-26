package dev.vfyjxf.cloudlib.ui.widgets;

import dev.vfyjxf.cloudlib.api.event.EventDispatch;
import dev.vfyjxf.cloudlib.api.ui.base.Widget;
import dev.vfyjxf.cloudlib.api.ui.texture.ColorTexture;
import dev.vfyjxf.cloudlib.api.ui.texture.VisualTexture;
import net.minecraft.client.gui.GuiGraphics;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

/**
 * A text input field widget with cursor and selection support.
 * <p>
 * Features:
 * <ul>
 *   <li>Single-line text input</li>
 *   <li>Cursor positioning and blinking</li>
 *   <li>Placeholder text support</li>
 *   <li>Max length validation</li>
 * </ul>
 */
public class TextFieldWidget extends Widget {

    private String text = "";
    private String placeholder = "";
    private int maxLength = 256;
    private int cursorPos = 0;
    private int selectionStart = -1;
    private int textColor = 0xFFFFFF;
    private int placeholderColor = 0x808080;
    private int cursorColor = 0xFFFFFF;
    private boolean editable = true;

    private @Nullable Consumer<String> onTextChanged;
    private @Nullable Consumer<String> onEnterPressed;

    private VisualTexture backgroundTexture = new ColorTexture(0xFF000000);
    private VisualTexture borderTexture = new ColorTexture(0xFFA0A0A0);

    private long cursorBlinkTime = 0;

    public static TextFieldWidget create() {
        return new TextFieldWidget();
    }

    public static TextFieldWidget create(String text) {
        return new TextFieldWidget().setText(text);
    }

    private TextFieldWidget() {
        setFocusable(true);

        onMouseClick((input, clickCount, context) -> {
            if (editable) {
                // Simple cursor positioning based on click
                var font = context().font();
                int clickX = (int) input.mouseX() - 4; // Account for padding
                int pos = 0;
                int width = 0;
                for (int i = 0; i < text.length(); i++) {
                    int charWidth = font.width(String.valueOf(text.charAt(i)));
                    if (width + charWidth / 2 > clickX) {
                        break;
                    }
                    width += charWidth;
                    pos++;
                }
                cursorPos = pos;
                selectionStart = -1;
                return EventDispatch.consumed;
            }
            return EventDispatch.pass;
        });

        onKeyPressed((input, context) -> {
            if (!editable || !focused()) return EventDispatch.pass;

            int keyCode = input.key().getValue();

            // Backspace
            if (keyCode == 259 && cursorPos > 0) {
                text = text.substring(0, cursorPos - 1) + text.substring(cursorPos);
                cursorPos--;
                notifyTextChanged();
            }
            // Delete
            else if (keyCode == 261 && cursorPos < text.length()) {
                text = text.substring(0, cursorPos) + text.substring(cursorPos + 1);
                notifyTextChanged();
            }
            // Left arrow
            else if (keyCode == 263 && cursorPos > 0) {
                cursorPos--;
            }
            // Right arrow
            else if (keyCode == 262 && cursorPos < text.length()) {
                cursorPos++;
            }
            // Home
            else if (keyCode == 268) {
                cursorPos = 0;
            }
            // End
            else if (keyCode == 269) {
                cursorPos = text.length();
            }
            // Enter
            else if (keyCode == 257 && onEnterPressed != null) {
                onEnterPressed.accept(text);
            }
            return EventDispatch.consumed;
        });

        onCharTyped((codePoint, modifiers, context) -> {
            if (!editable || !focused()) return EventDispatch.pass;

            if (Character.isISOControl(codePoint)) return EventDispatch.pass;

            if (text.length() < maxLength) {
                text = text.substring(0, cursorPos) + codePoint + text.substring(cursorPos);
                cursorPos++;
                notifyTextChanged();
            }
            return EventDispatch.consumed;

        });
    }

    private void notifyTextChanged() {
        if (onTextChanged != null) {
            onTextChanged.accept(text);
        }
    }

    public String text() {
        return text;
    }

    public TextFieldWidget setText(String text) {
        this.text = text;
        this.cursorPos = Math.min(cursorPos, this.text.length());
        return this;
    }

    public String placeholder() {
        return placeholder;
    }

    public TextFieldWidget setPlaceholder(String placeholder) {
        this.placeholder = placeholder;
        return this;
    }

    public int maxLength() {
        return maxLength;
    }

    public TextFieldWidget setMaxLength(int maxLength) {
        this.maxLength = maxLength;
        return this;
    }

    public boolean editable() {
        return editable;
    }

    public TextFieldWidget setEditable(boolean editable) {
        this.editable = editable;
        return this;
    }

    public TextFieldWidget onTextChanged(@Nullable Consumer<String> onTextChanged) {
        this.onTextChanged = onTextChanged;
        return this;
    }

    public TextFieldWidget onEnterPressed(@Nullable Consumer<String> onEnterPressed) {
        this.onEnterPressed = onEnterPressed;
        return this;
    }

    public TextFieldWidget setTextColor(int textColor) {
        this.textColor = textColor;
        return this;
    }

    public TextFieldWidget setPlaceholderColor(int placeholderColor) {
        this.placeholderColor = placeholderColor;
        return this;
    }

    public TextFieldWidget setBackgroundTexture(VisualTexture texture) {
        this.backgroundTexture = texture;
        return this;
    }

    public TextFieldWidget setBorderTexture(VisualTexture texture) {
        this.borderTexture = texture;
        return this;
    }

    @Override
    public void tick() {
        super.tick();
        cursorBlinkTime++;
    }

    @Override
    protected void renderInternal(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        // Render background
        backgroundTexture.render(graphics, 0, 0, width(), height());

        // Render border
        int borderWidth = 1;
        borderTexture.render(graphics, 0, 0, width(), borderWidth); // top
        borderTexture.render(graphics, 0, height() - borderWidth, width(), borderWidth); // bottom
        borderTexture.render(graphics, 0, 0, borderWidth, height()); // left
        borderTexture.render(graphics, width() - borderWidth, 0, borderWidth, height()); // right

        var font = context().font();
        int padding = 4;
        int textY = (height() - font.lineHeight) / 2;

        // Render text or placeholder
        if (text.isEmpty() && !placeholder.isEmpty() && !focused()) {
            graphics.drawString(font, placeholder, padding, textY, placeholderColor, false);
        } else {
            graphics.drawString(font, text, padding, textY, textColor, false);
        }

        // Render cursor
        if (focused() && editable && (cursorBlinkTime / 10) % 2 == 0) {
            String beforeCursor = text.substring(0, cursorPos);
            int cursorX = padding + font.width(beforeCursor);
            graphics.fill(cursorX, textY - 1, cursorX + 1, textY + font.lineHeight + 1, 0xFF000000 | cursorColor);
        }
    }
}
