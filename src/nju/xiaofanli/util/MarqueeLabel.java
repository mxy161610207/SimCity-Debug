package nju.xiaofanli.util;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class MarqueeLabel extends JLabel implements ActionListener {
    private final Timer timer = new Timer(100, this);
    private final String text;
    private final int window;
    private int index = 0;
    public MarqueeLabel(String text, Icon icon, int horizontalAlignment, int window) {
        super();
        if(text == null || window < 1)
            throw new IllegalArgumentException("Null string or window less than 1");
        String blanks = new String(new char[window-1]).replace("\0", " ");
        this.text = " " + blanks + text + blanks;
        this.window = window;
        setText(blanks);
        setIcon(icon);
        setHorizontalAlignment(horizontalAlignment);
        timer.start();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        index++;
        if (index > text.length() - window)
            index = 0;
        setText(text.substring(index, index + window));
    }
}
