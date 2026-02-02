package dev.vfyjxf.cloudlib.api.ui.blueprint;

import dev.vfyjxf.cloudlib.api.ui.base.Blueprint;
import dev.vfyjxf.cloudlib.api.ui.base.Scene;
import dev.vfyjxf.cloudlib.api.ui.base.SceneContext;
import dev.vfyjxf.cloudlib.api.ui.base.ScopedReceiver;
import dev.vfyjxf.cloudlib.api.ui.style.UIStyle;
import dev.vfyjxf.cloudlib.ui.widgets.TextFieldWidget;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

/**
 * Blueprint for {@link TextFieldWidget}.
 * <p>
 * Usage:
 * <pre>{@code
 * TextField("")
 * TextField("Initial text", text -> System.out.println("Changed: " + text))
 * TextField("").placeholder("Enter text...").maxLength(100)
 * }</pre>
 */
public final class TextFieldBlueprint implements Blueprint<TextFieldWidget> {

    private String text;
    private String placeholder = "";
    private int maxLength = 256;
    private boolean editable = true;
    private int textColor = 0xFFFFFF;
    private int placeholderColor = 0x808080;

    private @Nullable Consumer<String> onTextChanged;
    private @Nullable Consumer<String> onEnterPressed;
    private @Nullable Object key;
    private UIStyle style = UIStyle.EMPTY;

    private TextFieldBlueprint(String text) {
        this.text = text != null ? text : "";
    }

    //region dsl entry points

    public static TextFieldBlueprint TextField(String text) {
        return ScopedReceiver.add(new TextFieldBlueprint(text));
    }

    public static TextFieldBlueprint TextField(String text, Consumer<String> onTextChanged) {
        return ScopedReceiver.add(new TextFieldBlueprint(text).onTextChanged(onTextChanged));
    }

    //endregion

    //region builder methods

    public TextFieldBlueprint text(String text) {
        this.text = text;
        return this;
    }

    public TextFieldBlueprint placeholder(String placeholder) {
        this.placeholder = placeholder;
        return this;
    }

    public TextFieldBlueprint maxLength(int maxLength) {
        this.maxLength = maxLength;
        return this;
    }

    public TextFieldBlueprint editable(boolean editable) {
        this.editable = editable;
        return this;
    }

    public TextFieldBlueprint textColor(int color) {
        this.textColor = color;
        return this;
    }

    public TextFieldBlueprint placeholderColor(int color) {
        this.placeholderColor = color;
        return this;
    }

    public TextFieldBlueprint onTextChanged(@Nullable Consumer<String> onTextChanged) {
        this.onTextChanged = onTextChanged;
        return this;
    }

    public TextFieldBlueprint onEnterPressed(@Nullable Consumer<String> onEnterPressed) {
        this.onEnterPressed = onEnterPressed;
        return this;
    }

    public TextFieldBlueprint key(@Nullable Object key) {
        this.key = key;
        return this;
    }

    public TextFieldBlueprint style(UIStyle style) {
        this.style = style;
        return this;
    }

    //endregion

    //region blueprint implementation

    @Override
    public @Nullable Object key() {
        return key;
    }

    @Override
    public TextFieldWidget createWidget(Scene scene, SceneContext context) {
        return TextFieldWidget.create();
    }

    @Override
    public void updateWidget(TextFieldWidget widget, Scene scene, SceneContext context) {
        widget.setText(text)
              .setPlaceholder(placeholder)
              .setMaxLength(maxLength)
              .setEditable(editable)
              .setTextColor(textColor)
              .setPlaceholderColor(placeholderColor)
              .onTextChanged(onTextChanged)
              .onEnterPressed(onEnterPressed)
              .applyStyle(style);
    }

    //endregion
}
