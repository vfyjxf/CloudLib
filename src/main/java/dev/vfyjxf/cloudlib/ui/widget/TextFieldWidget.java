package dev.vfyjxf.cloudlib.ui.widget;

import com.mojang.blaze3d.platform.InputConstants;
import dev.vfyjxf.cloudlib.api.event.EventDispatch;
import dev.vfyjxf.cloudlib.api.ui.base.Widget;
import dev.vfyjxf.cloudlib.api.ui.canvas.SceneCanvas;
import dev.vfyjxf.cloudlib.api.ui.debug.InspectionInfoCollector;
import dev.vfyjxf.cloudlib.api.ui.debug.InspectionProperty;
import dev.vfyjxf.cloudlib.api.ui.texture.ColorTexture;
import dev.vfyjxf.cloudlib.api.ui.texture.VisualTexture;
import dev.vfyjxf.taffy.geometry.FloatSize;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

/**
 * Single-line text input with cursor.
 */
public class TextFieldWidget extends Widget {

    //region state

    private String text = "";
    private String placeholder = "";
    private int maxLength = 256;
    private int cursorPos = 0;
    private int selectionStart = -1;
    private boolean editable = true;
    private long cursorBlinkTime = 0;

    //endregion

    //region colors

    private int textColor = 0xFFFFFF;
    private int placeholderColor = 0x808080;
    private int cursorColor = 0xFFFFFF;

    //endregion

    //region textures

    private VisualTexture backgroundTexture = new ColorTexture(0xFF000000);
    private VisualTexture borderTexture = new ColorTexture(0xFFA0A0A0);

    //endregion

    //region callbacks

    private @Nullable Consumer<String> onTextChanged;
    private @Nullable Consumer<String> onEnterPressed;

    //endregion

    //region factory

    public static TextFieldWidget create() {
        return new TextFieldWidget();
    }

    public static TextFieldWidget create(String text) {
        return new TextFieldWidget().setText(text);
    }

    private TextFieldWidget() {
        setFocusable(true);

        onMount((scene, context, handle) -> {
            scene.layoutTree().setMeasureFunc(nodeId(), (style, availableSpace) -> {
                var font = context.font();
                int textWidth = Math.max(font.width(text), font.width(placeholder));
                return new FloatSize(textWidth + 8, font.lineHeight + 6);
            });
        });

        setupInputHandlers();
    }

    //endregion

    //region input

    private void setupInputHandlers() {
        onMouseClick((input, clickCount, context) -> {
            if (!editable) return EventDispatch.pass;
            var font = context().font();
            int clickX = (int) input.mouseX() - 4;
            int pos = 0;
            int width = 0;
            for (int i = 0; i < text.length(); i++) {
                int charWidth = font.width(String.valueOf(text.charAt(i)));
                if (width + charWidth / 2 > clickX) break;
                width += charWidth;
                pos++;
            }
            cursorPos = pos;
            selectionStart = -1;
            return EventDispatch.consumed;
        });

        onKeyPressed((input, context) -> {
            if (!editable || !focused()) return EventDispatch.pass;
            int keyCode = input.key().getValue();

            if (keyCode == InputConstants.KEY_BACKSPACE && cursorPos > 0) {
                text = text.substring(0, cursorPos - 1) + text.substring(cursorPos);
                cursorPos--;
                notifyTextChanged();
            } else if (keyCode == InputConstants.KEY_DELETE && cursorPos < text.length()) {
                text = text.substring(0, cursorPos) + text.substring(cursorPos + 1);
                notifyTextChanged();
            } else if (keyCode == InputConstants.KEY_LEFT && cursorPos > 0) {
                cursorPos--;
            } else if (keyCode == InputConstants.KEY_RIGHT && cursorPos < text.length()) {
                cursorPos++;
            } else if (keyCode == InputConstants.KEY_HOME) {
                cursorPos = 0;
            } else if (keyCode == InputConstants.KEY_END) {
                cursorPos = text.length();
            } else if ((keyCode == InputConstants.KEY_RETURN || keyCode == InputConstants.KEY_NUMPADENTER) && onEnterPressed != null) { // Enter
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

    //endregion

    //region configuration

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

    public TextFieldWidget onTextChanged(@Nullable Consumer<String> callback) {
        this.onTextChanged = callback;
        return this;
    }

    public TextFieldWidget onEnterPressed(@Nullable Consumer<String> callback) {
        this.onEnterPressed = callback;
        return this;
    }

    public TextFieldWidget setTextColor(int color) {
        this.textColor = color;
        return this;
    }

    public TextFieldWidget setPlaceholderColor(int color) {
        this.placeholderColor = color;
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

    //endregion

    //region lifecycle

    @Override
    public void tick() {
        super.tick();
        cursorBlinkTime++;
    }

    //endregion

    //region rendering

    @Override
    protected void renderInternal(SceneCanvas canvas, int mouseX, int mouseY, float partialTicks) {
        canvas.texture(backgroundTexture, 0, 0, width(), height());

        // Border
        int bw = 1;
        canvas.texture(borderTexture, 0, 0, width(), bw);
        canvas.texture(borderTexture, 0, height() - bw, width(), bw);
        canvas.texture(borderTexture, 0, 0, bw, height());
        canvas.texture(borderTexture, width() - bw, 0, bw, height());

        var font = context().font();
        int padding = 4;
        int textY = (height() - font.lineHeight) / 2;

        // Text or placeholder
        if (text.isEmpty() && !placeholder.isEmpty() && !focused()) {
            canvas.text(placeholder, padding, textY, placeholderColor, false);
        } else {
            canvas.text(text, padding, textY, textColor, false);
        }

        // Cursor
        if (focused() && editable && (cursorBlinkTime / 10) % 2 == 0) {
            String beforeCursor = text.substring(0, cursorPos);
            int cursorX = padding + font.width(beforeCursor);
            canvas.fill(cursorX, textY - 1, 1, font.lineHeight + 2, 0xFF000000 | cursorColor);
        }
    }

    //endregion

    //region inspection

    @Override
    public void collectInspectionInfo(InspectionInfoCollector collector) {
        super.collectInspectionInfo(collector);
        collector.addWithDefault("text", text.isEmpty() ? "(empty)" : truncate(text, 20), "(empty)", InspectionProperty.categoryData);
        collector.addWithDefault("textLength", text.length(), 0, InspectionProperty.categoryData);
        collector.addWithDefault("cursorPos", cursorPos, 0, InspectionProperty.categoryData);
        collector.addWithDefault("maxLength", maxLength, 256, InspectionProperty.categoryData);
        collector.addWithDefault("editable", editable, true, InspectionProperty.categoryState);
        if (!placeholder.isEmpty()) {
            collector.add("placeholder", placeholder, InspectionProperty.categoryVisual);
        }
    }

    private static String truncate(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max - 3) + "...";
    }

    //endregion
}
