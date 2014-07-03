package com.devicehive.view;


import com.devicehive.client.HiveDevice;
import com.devicehive.client.model.exceptions.HiveException;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;

public class DeviceView {

    private static final int gap = 10;
    private JFrame frame = new JFrame("Device Example");
    private ImageIcon green;
    private ImageIcon red;
    private JLabel state;

    public DeviceView(final HiveDevice deviceExample) throws IOException {
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

        green = new ImageIcon(ImageIO.read(DeviceView.class.getResourceAsStream("/Circle_Green.png"))
                .getScaledInstance(50, 50, Image.SCALE_SMOOTH));
        red = new ImageIcon(ImageIO.read(DeviceView.class.getResourceAsStream("/Circle_Red.png"))
                .getScaledInstance(50, 50, Image.SCALE_SMOOTH));

        JPanel main = new JPanel();

        main.setBorder(BorderFactory.createEmptyBorder(gap, gap, gap, gap));
        main.setLayout(new BoxLayout(main, BoxLayout.X_AXIS));
        main.add(new Label("LED"));
        state = new JLabel(red);
        main.add(state);

        main.setBackground(Color.WHITE);
        frame.add(main);
        frame.pack();
        frame.setVisible(true);
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                try {
                    deviceExample.close();
                } catch (HiveException e1) {
                    //TODO
                }
                super.windowClosed(e);
            }
        });
    }

    public void setGreen() {
        state.setIcon(green);
        frame.pack();
    }

    public void setRed() {
        state.setIcon(red);
        frame.pack();
    }


}
