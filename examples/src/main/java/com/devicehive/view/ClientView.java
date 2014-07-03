package com.devicehive.view;


import com.devicehive.client.model.exceptions.HiveException;
import com.devicehive.examples.ClientExample;
import com.devicehive.exceptions.ExampleException;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.net.URI;

public class ClientView {
    private static final int gap = 10;
    private JFrame frame = new JFrame("Client Example");
    private JTextField textField = new JTextField(50);
    private JButton on = new JButton("On");
    private JButton off = new JButton("Off");
    private JButton start = new JButton("Start");

    public ClientView(final ClientExample example) {
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                super.windowClosed(e);
                example.close();
            }
        });

        JPanel main = new JPanel();
        main.setBorder(BorderFactory.createEmptyBorder(gap, gap, gap, gap));
        BoxLayout vertical = new BoxLayout(main, BoxLayout.Y_AXIS);
        main.setLayout(vertical);

        JPanel url = new JPanel();
        url.setLayout(new BoxLayout(url, BoxLayout.X_AXIS));
        url.add(new JLabel("Server url"));
        url.add(Box.createHorizontalStrut(gap));
        url.add(textField);
        url.add(Box.createHorizontalStrut(gap));
        start.addActionListener(onStart(example));
        url.add(start);


        JPanel commandPanel = new JPanel();
        commandPanel.setLayout(new BoxLayout(commandPanel, BoxLayout.X_AXIS));
        on.addActionListener(example.createTurnOnListener());
        commandPanel.add(on);
        commandPanel.add(Box.createHorizontalStrut(gap));
        off.addActionListener(example.createTurnOffListener());
        commandPanel.add(off);

        main.add(url);
        main.add(Box.createRigidArea(new Dimension(gap, gap)));
        main.add(commandPanel);
        on.setVisible(false);
        off.setVisible(false);
        frame.add(main);
        frame.pack();
        frame.setVisible(true);
    }


    private ActionListener onStart(final ClientExample example){
        return new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    URI uri = URI.create(textField.getText());
                    example.run(uri, false);
                    start.setVisible(false);
                    on.setVisible(true);
                    off.setVisible(true);
                    frame.pack();
                }  catch (HiveException | ExampleException | IOException e1) {
                    System.err.println(e1.getMessage());
                }
            }
        };
    }

}
