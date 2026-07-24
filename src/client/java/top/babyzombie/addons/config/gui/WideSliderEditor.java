package top.babyzombie.addons.config.gui;

import io.github.notenoughupdates.moulconfig.GuiTextures;
import io.github.notenoughupdates.moulconfig.common.IMinecraft;
import io.github.notenoughupdates.moulconfig.gui.GuiComponent;
import io.github.notenoughupdates.moulconfig.gui.GuiImmediateContext;
import io.github.notenoughupdates.moulconfig.gui.KeyboardEvent;
import io.github.notenoughupdates.moulconfig.gui.MouseEvent;
import io.github.notenoughupdates.moulconfig.gui.component.TextFieldComponent;
import io.github.notenoughupdates.moulconfig.gui.editors.ComponentEditor;
import io.github.notenoughupdates.moulconfig.observer.GetSetter;
import io.github.notenoughupdates.moulconfig.processor.ProcessedOption;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.function.BiFunction;
import java.util.function.Supplier;

/**
 * Custom slider editor with wider slider track and wider number input,
 * registered via {@code customProcessor} so it only affects our config.
 * <p>
 * Layout: [=====slider=====] [text field]
 * Total: 150px  (vs default 75px)
 * Slider: 100px (vs default 55px)
 * Text:   45px  (vs default 20px, shows ~6 digits)
 */
public class WideSliderEditor extends ComponentEditor {

    private final GuiComponent component;

    public WideSliderEditor(ProcessedOption option, float minValue, float maxValue, float minStep) {
        super(option);
        if (minStep < 0) minStep = 0.01f;
        component = wrapComponent(new WideSliderComponent(
                option, minValue, maxValue, minStep
        ));
    }

    @Override
    public @NotNull GuiComponent getDelegate() {
        return component;
    }

    // ── inner component ──

    private static final int TEXT_W = 45;
    private static final int GAP = 5;
    private static final int SLIDER_W = 100;
    private static final int TOTAL_W = SLIDER_W + GAP + TEXT_W;

    private static class WideSliderComponent extends GuiComponent {

        private final ProcessedOption option;
        private final float minValue, maxValue, minStep;
        private boolean clicked;
        private final TextFieldComponent textField;

        WideSliderComponent(ProcessedOption option, float min, float max, float step) {
            this.option = option;
            this.minValue = min;
            this.maxValue = max;
            this.minStep = step;

            GetSetter<String> textValue = new SliderTextValue(this);
            this.textField = new TextFieldComponent(
                    textValue,
                    TEXT_W,
                    (Supplier<Boolean>) () -> true,
                    "",
                    IMinecraft.INSTANCE.getDefaultFontRenderer(),
                    Collections.emptySet()
            );
        }

        // ── safe int/float access (MoulConfig fields can be either) ──

        float getValueF() {
            return ((Number) option.get()).floatValue();
        }

        void setValueF(float v) {
            Object cur = option.get();
            if (cur instanceof Integer) {
                option.set((int) v);
            } else {
                option.set(v);
            }
        }

        @Override
        public int getWidth() {
            return TOTAL_W;
        }

        @Override
        public int getHeight() {
            return 20;
        }

        @Override
        public void render(@NotNull GuiImmediateContext ctx) {
            int sliderW = ctx.getWidth() - GAP - TEXT_W;
            int h = ctx.getHeight();
            float frac = Math.clamp((getValueF() - minValue) / (maxValue - minValue), 0, 1);
            int pos = (int) (frac * sliderW);

            // ── Slider track (original MoulConfig textures) ──
            ctx.getRenderContext().drawTexturedRect(GuiTextures.SLIDER_ON_CAP, 0, 0, 4, h);
            ctx.getRenderContext().drawTexturedRect(GuiTextures.SLIDER_OFF_CAP, sliderW - 4, 0, 4, h);
            if (pos > 5) {
                ctx.getRenderContext().drawTexturedRect(GuiTextures.SLIDER_ON_SEGMENT, 4, 0, pos - 4, h);
            }
            if (pos < sliderW - 5) {
                ctx.getRenderContext().drawTexturedRect(GuiTextures.SLIDER_OFF_SEGMENT, pos, 0, sliderW - 4 - pos, h);
            }
            for (int i = 0; i < 4; i++) {
                int notchX = sliderW * i / 4 - 1;
                ctx.getRenderContext().drawTexturedRect(
                        notchX > pos ? GuiTextures.SLIDER_OFF_NOTCH : GuiTextures.SLIDER_ON_NOTCH,
                        notchX, (h - 4) / 2f, 2, 4);
            }
            ctx.getRenderContext().drawTexturedRect(GuiTextures.SLIDER_BUTTON, pos - 4, 0, 8, h);

            // ── Text field ──
            int tfX = sliderW + GAP;
            var rc = ctx.getRenderContext();
            rc.pushMatrix();
            rc.translate(tfX, 0);
            textField.render(ctx.translated(tfX, 0, TEXT_W, h));
            rc.popMatrix();

            // ── Drag update ──
            if (clicked) {
                float rawFrac = (float) ctx.getMouseX() / sliderW;
                float v = rawFrac * (maxValue - minValue) + minValue;
                v = Math.round(v / minStep) * minStep;
                v = Math.clamp(v, minValue, maxValue);
                setValueF(v);
            }
        }

        @Override
        public boolean mouseEvent(@NotNull MouseEvent event, @NotNull GuiImmediateContext ctx) {
            int sliderW = ctx.getWidth() - GAP - TEXT_W;
            if (!ctx.getRenderContext().isMouseButtonDown(0)) clicked = false;
            if (event instanceof MouseEvent.Click c && c.getMouseState() && c.getMouseButton() == 0) {
                if (ctx.getMouseX() >= 0 && ctx.getMouseX() < sliderW
                        && ctx.getMouseY() >= 0 && ctx.getMouseY() < ctx.getHeight()) {
                    clicked = true;
                    return true;
                }
            }
            if (clicked) return true;
            return textField.mouseEvent(event,
                    ctx.translated(sliderW + GAP, 0, TEXT_W, ctx.getHeight()));
        }

        @Override
        public boolean keyboardEvent(@NotNull KeyboardEvent event, @NotNull GuiImmediateContext ctx) {
            int sliderW = ctx.getWidth() - GAP - TEXT_W;
            textField.setShouldExpandToFit(true);
            return textField.keyboardEvent(event,
                    ctx.translated(sliderW + GAP, 0, TEXT_W, ctx.getHeight()));
        }

        @Override
        public <T> T foldChildren(@NotNull T initial, @NotNull BiFunction<GuiComponent, T, T> visitor) {
            return visitor.apply(textField, initial);
        }
    }

    /**
     * Bridges the slider's float value ↔ text field's string value.
     * Works for both int and float config fields via {@link WideSliderComponent#getValueF()}.
     */
    private static class SliderTextValue implements GetSetter<String> {
        private final WideSliderComponent comp;
        private String buf = "";

        SliderTextValue(WideSliderComponent comp) {
            this.comp = comp;
        }

        @Override
        public String get() {
            if (comp.textField.isInFocus()) return buf;
            float v = comp.getValueF();
            String s = stripDotZero(v);
            buf = s;
            return s;
        }

        @Override
        public void set(String newValue) {
            buf = newValue;
            float v;
            try {
                v = Float.parseFloat(buf);
            } catch (NumberFormatException e) {
                return;
            }
            v = Math.round(v / comp.minStep) * comp.minStep;
            v = Math.clamp(v, comp.minValue, comp.maxValue);
            comp.setValueF(v);
            buf = stripDotZero(v);
        }

        private static String stripDotZero(float v) {
            if (v == (int) v) return String.valueOf((int) v);
            return String.valueOf(v);
        }
    }
}
