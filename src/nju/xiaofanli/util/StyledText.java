package nju.xiaofanli.util;

import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyleContext;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class StyledText {
    private final List<Pair<String, Style>> text;

    public StyledText() {
        text = new ArrayList<>();
    }

    public StyledText(String string) {
        this();
        append(string);
    }

    public StyledText(String string, Color color) {
        this();
        append(string, color);
    }

    public StyledText append(String string) {
        text.add(new Pair<>(string, null));
        return this;
    }

    public StyledText append(String string, Color color) {
        text.add(new Pair<>(string, getTextStyle(color)));
        return this;
    }

    public StyledText append(String string, boolean isBold) {
        text.add(new Pair<>(string, getTextStyle(isBold)));
        return this;
    }

    public StyledText append(String string, Color color, boolean isBold) {
        text.add(new Pair<>(string, getTextStyle(color, isBold)));
        return this;
    }

    public static Style getTextStyle(Color color) {
        StyleContext sc = StyleContext.getDefaultStyleContext();
        Style style = sc.getStyle(color.toString());
        if (style == null) {
            style = sc.addStyle(color.toString(), null);
            StyleConstants.setForeground(style, color);
        }
        return style;
    }

    public static Style getTextStyle(boolean bold) {
        StyleContext sc = StyleContext.getDefaultStyleContext();
        if (bold) {
            Style style = sc.getStyle("bold");
            if (style == null) {
                style = sc.addStyle("bold", null);
                StyleConstants.setBold(style, true);
            }
            return style;
        }
        else
            return sc.getStyle(StyleContext.DEFAULT_STYLE);
    }

    public static Style getTextStyle(Color color, boolean bold) {
        if (!bold)
            return getTextStyle(color);

        String name = color.toString() + "_bold";
        StyleContext sc = StyleContext.getDefaultStyleContext();
        Style style = sc.getStyle(name);
        if (style == null) {
            style = sc.addStyle(name, null);
            StyleConstants.setForeground(style, color);
            StyleConstants.setBold(style, true);
        }
        return style;
    }

    public static Style getTextStyle(Font font, float lineSpacing) {
        String name = font.getName()+"_lineSpacing" + lineSpacing;
        StyleContext sc = StyleContext.getDefaultStyleContext();
        Style style = sc.getStyle(name);
        if (style == null) {
            style = sc.addStyle(name, null);
            StyleConstants.setFontSize(style, font.getSize());
            StyleConstants.setFontFamily(style, font.getFamily());
            StyleConstants.setLineSpacing(style, lineSpacing);
        }
        return style;
    }

    public List<Pair<String, Style>> getText() {
        return text;
    }
}
