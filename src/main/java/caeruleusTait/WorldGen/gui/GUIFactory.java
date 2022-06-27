// Copyright 2022 - 2022, Caeruleus Draconis and Taiterio
// SPDX-License-Identifier: Apache-2.0

package caeruleusTait.WorldGen.gui;

import caeruleusTait.WorldGen.gui.widgets.WGCheckbox;
import caeruleusTait.WorldGen.gui.widgets.WGLabel;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class GUIFactory {

    public final static int TOP_START = 40;
    public final static int BOT_OFFSET = 28;

    public final static int LINE_HEIGHT = 20;
    public final static int LINE_WIDTH = 300;
    public final static int LINE_CENTER_HORIZONTAL = LINE_WIDTH / 2;
    public final static int LINE_CENTER_VERTICAL = LINE_HEIGHT / 2;
    public final static int LINE_HSPACE = 3;
    public final static int LINE_VSPACE = 4;

    private final Screen screen;
    private final int centerHorizontal;
    private final int centerVertical;

    private int top;
    private int bottom;

    public GUIFactory(Screen _screen) {
        screen = _screen;

        centerHorizontal = screen.width / 2;
        centerVertical = screen.height / 2;

        bottom = screen.height - BOT_OFFSET;
        top = TOP_START;
    }

    /**
     * Gets the horizontal center of the current screen.
     * @return The horizontal center
     */
    public int hCenter() {
        return centerHorizontal;
    }

    /**
     * Gets the vertical center of the current screen.
     * @return The vertical center
     */
    public int vCenter() {
        return centerVertical;
    }


    /**
     * Creates a cycle button spanning an entire row.
     *
     * @param vPos The vertical position to start at (top)
     * @param label The label to assign the button
     * @param builder The CycleButton builder
     * @param onValueChange The function to call onValueChange
     * @return The built CycleButton
     * @param <T> The type of value the CycleButton uses
     */
    public <T> CycleButton<T> cycleButton(int vPos, Component label, CycleButton.Builder<T> builder, CycleButton.OnValueChange<T> onValueChange) {
        return cycleButton(vPos, label, builder, onValueChange, 0, 1);
    }

    /**
     * Creates a cycle button at the specified point in the row.
     *
     * @param vPos The vertical position to start at (top)
     * @param label The label to assign the button
     * @param builder The CycleButton builder
     * @param onValueChange The function to call onValueChange
     * @param hPos The column index in this row (from 0)
     * @param hMax The maximum amount of columns in this row
     * @return The built CycleButton
     * @param <T> The type of value the CycleButton uses
     */
    public <T> CycleButton<T> cycleButton(int vPos, Component label, CycleButton.Builder<T> builder, CycleButton.OnValueChange<T> onValueChange, int hPos, int hMax) {
        int buttonSection = (LINE_WIDTH - ((hMax-1) * LINE_HSPACE)) / hMax;
        return builder.create(centerHorizontal - LINE_CENTER_HORIZONTAL + (hPos * (buttonSection + LINE_HSPACE)), vPos, buttonSection, LINE_HEIGHT, label, onValueChange);
    }



    /**
     * Creates a button spanning an entire row.
     *
     * @param vPos The vertical position to start at (top)
     * @param label The label to assign the button
     * @param onPress The function to call onPress
     * @return The built Button
     */
    public Button button(int vPos, Component label, Button.OnPress onPress) {
        return button(vPos, label, onPress, 0, 1);
    }

    /**
     * Creates a button at the specified point in the row.
     *
     * @param vPos The vertical position to start at (top)
     * @param label The label to assign the button
     * @param onPress The function to call onPress
     * @param hPos The column index in this row (from 0)
     * @param hMax The maximum amount of columns in this row
     * @return The built Button
     */
    public Button button(int vPos, Component label, Button.OnPress onPress, int hPos, int hMax) {
        int buttonSection = ((LINE_WIDTH - ((hMax-1) * LINE_HSPACE)) / hMax);
        return new Button(centerHorizontal - LINE_CENTER_HORIZONTAL + (hPos * (buttonSection + LINE_HSPACE)), vPos, buttonSection, LINE_HEIGHT, label, onPress);
    }



    /**
     * Creates an EditBox spanning an entire row.
     *
     * @param vPos The vertical position to start at (top)
     * @param font The font to use
     * @param label The label to assign the button
     * @return The built EditBox
     */
    public EditBox editBox(int vPos, Font font, Component label) {
        return editBox(vPos, font, label, 0, 1);
    }

    /**
     * Creates an EditBox at the specified point in the row.
     *
     * @param vPos The vertical position to start at (top)
     * @param font The font to use
     * @param label The label to assign the button
     * @param hPos The column index in this row (from 0)
     * @param hMax The maximum amount of columns in this row
     * @return The built EditBox
     */
    public EditBox editBox(int vPos, Font font, Component label, int hPos, int hMax) {
        int buttonSection = (((LINE_WIDTH) - ((hMax-1) * LINE_HSPACE)) / hMax) - 2; // Editboxes have 1 pixel more to either side
        return new EditBox(font, centerHorizontal - LINE_CENTER_HORIZONTAL + 1 + (hPos * (buttonSection + LINE_HSPACE + 2)), vPos, buttonSection, LINE_HEIGHT, label);
    }



    /**
     * Creates a WGCheckbox spanning an entire row.
     *
     * @param vPos The vertical position to start at (top)
     * @param label The label to assign the button
     * @param onPress The function to call onPress
     * @return The built WGCheckbox
     */
    public WGCheckbox checkbox(int vPos, Component label, WGCheckbox.OnPress onPress) {
        return checkbox(vPos, label, onPress, 0, 1);
    }

    /**
     * Creates a WGCheckbox at the specified point in the row.
     *
     * @param vPos The vertical position to start at (top)
     * @param label The label to assign the button
     * @param onPress The function to call onPress
     * @param hPos The column index in this row (from 0)
     * @param hMax The maximum amount of columns in this row
     * @return The built WGCheckbox
     */
    public WGCheckbox checkbox(int vPos, Component label, WGCheckbox.OnPress onPress, int hPos, int hMax) {
        int buttonSection = (LINE_WIDTH - ((hMax-1) * LINE_HSPACE)) / hMax;
        return new WGCheckbox(centerHorizontal - LINE_CENTER_HORIZONTAL + (hPos * (buttonSection + LINE_HSPACE)), vPos, buttonSection, LINE_HEIGHT, label, onPress, true, true);
    }



    /**
     * Creates a WGLabel spanning an entire row.
     *
     * @param vPos The vertical position to start at (top)
     * @param font The font to use
     * @param color The color of the text
     * @param alignment The text alignment
     * @param label The label to assign the button
     * @return The built WGLabel
     */
    public WGLabel label(int vPos, Font font, int color, WGLabel.TextAlignment alignment, Component label) {
        return label(vPos, font, color, alignment, label, 0, 1);
    }

    /**
     * Creates a WGLabel at the specified point in the row.
     *
     * @param vPos The vertical position to start at (top)
     * @param font The font to use
     * @param color The color of the text
     * @param alignment The text alignment
     * @param label The label to assign the button
     * @param hPos The column index in this row (from 0)
     * @param hMax The maximum amount of columns in this row
     * @return The built WGLabel
     */
    public WGLabel label(int vPos, Font font, int color, WGLabel.TextAlignment alignment, Component label, int hPos, int hMax) {
        int buttonSection = (LINE_WIDTH - ((hMax-1) * LINE_HSPACE)) / hMax;
        return new WGLabel(font, centerHorizontal - LINE_CENTER_HORIZONTAL + (hPos * (buttonSection + LINE_HSPACE)), vPos, buttonSection, LINE_HEIGHT, alignment, label, color);
    }



    // Helper functions to get HPos and more easily build the GUI

    /**
     * Returns the top coordinate and moves down to the next line.
     *
     * @return The top position
     */
    public int pushFromTop() {
        return pushFromTop(LINE_HEIGHT + LINE_VSPACE);
    }

    /**
     * Returns the top coordinate and increases it by the given amount.
     * Note: Positive values move downwards, negative values upwards.
     *
     * @return The top position
     */
    public int pushFromTop(int change) {
        int res = top;
        top += change;
        return res;
    }

    /**
     * Returns the bottom coordinate and moves up to the next line.
     *
     * @return The bottom position
     */
    public int pushFromBottom() {
        return pushFromBottom(LINE_HEIGHT + LINE_VSPACE);
    }

    /**
     * Returns the bottom coordinate and increases it by the given amount moving upwards.
     * Note: Positive values move upwards, negative values downwards.
     *
     * @return The bottom position
     */
    public int pushFromBottom(int change) {
        int res = bottom;
        bottom -= change;
        return res;
    }

    /**
     * Resets the top and bottom positions to default
     */
    public void reset() {
        top = TOP_START;
        bottom = screen.height - BOT_OFFSET;
    }

    /**
     * Gets the top value.
     * @return The top value
     */
    public int peekFromTop() {
        return top;
    }

    /**
     * Gets the bottom value.
     * @return The bottom value
     */
    public int peekFromBottom() {
        return bottom;
    }

    /**
     * Sets the top value.
     * @param top The new top value
     */
    public void setTop(int top) {
        this.top = top;
    }

    /**
     * Sets the bottom value.
     * @param bottom The new bottom value
     */
    public void setBottom(int bottom) {
        this.bottom = bottom;
    }
}
