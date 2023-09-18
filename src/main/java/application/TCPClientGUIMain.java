package application;

import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.Date;

public class TCPClientGUIMain {
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private JTextPane chatTextPane;
    private JTextPane messageTextPane;
    private JTextField serverIpField;
    private JTextField serverPortField;
    private JButton connectButton;
    private JButton disconnectButton;
    private StyledDocument chatDoc;
    private Style defaultStyle;
    private Style timeStyle;
    private Style connectStyle;
    private Style disconnectStyle;
    private Style sentStyle;
    private Style messageOwnerStyle;

    public TCPClientGUIMain() {
        JFrame frame = new JFrame("TCP Client");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(700, 400);
        frame.setLayout(new BorderLayout());

        chatTextPane = new JTextPane();
        chatTextPane.setEditable(false);

        JScrollPane chatScrollPane = new JScrollPane(chatTextPane);
        chatScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);

        JPanel topPanel = new JPanel();
        topPanel.setLayout(new FlowLayout(FlowLayout.LEFT));

        serverIpField = new JTextField("localhost", 15);
        serverPortField = new JTextField("12345", 6);

        connectButton = new JButton("Connect");
        disconnectButton = new JButton("Disconnect");

        topPanel.add(new JLabel("Server IP:"));
        topPanel.add(serverIpField);
        topPanel.add(new JLabel("Server Port:"));
        topPanel.add(serverPortField);
        topPanel.add(connectButton);
        topPanel.add(disconnectButton);

        frame.add(topPanel, BorderLayout.NORTH);
        frame.add(chatScrollPane, BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel();
        bottomPanel.setLayout(new BorderLayout());

        messageTextPane = new JTextPane();
        messageTextPane.setPreferredSize(new Dimension(400, 100));

        JButton sendButton = new JButton("Send");
        bottomPanel.add(new JScrollPane(messageTextPane), BorderLayout.CENTER);
        bottomPanel.add(sendButton, BorderLayout.EAST);

        frame.add(bottomPanel, BorderLayout.SOUTH);

        connectButton.addActionListener(e -> connectToServer(serverIpField.getText(), Integer.parseInt(serverPortField.getText())));
        disconnectButton.addActionListener(e -> disconnectFromServer());
        sendButton.addActionListener(e -> sendMessage());

        initializeChatStyles();
        frame.setVisible(true);
    }

    private void initializeChatStyles() {
        chatDoc = chatTextPane.getStyledDocument();

        defaultStyle = chatTextPane.addStyle("DefaultStyle", null);
        StyleConstants.setFontFamily(defaultStyle, "SansSerif");

        timeStyle = chatTextPane.addStyle("TimeStyle", defaultStyle);
        StyleConstants.setForeground(timeStyle, Color.GREEN);

        connectStyle = chatTextPane.addStyle("ConnectStyle", defaultStyle);
        StyleConstants.setForeground(connectStyle, Color.BLUE);

        disconnectStyle = chatTextPane.addStyle("DisconnectStyle", defaultStyle);
        StyleConstants.setForeground(disconnectStyle, Color.RED);

        messageOwnerStyle = chatTextPane.addStyle("MessageOwnerStyle", defaultStyle);
        StyleConstants.setForeground(messageOwnerStyle, Color.MAGENTA);

        sentStyle = chatTextPane.addStyle("SentStyle", defaultStyle);
    }
    private void sendMessage() {
        String message = messageTextPane.getText();
        if(message.isEmpty()) {
            return;
        }
        String[] messageSplitByLine = message.split("\n");
        for(String messageLine: messageSplitByLine){
            logMessage("You: " + messageLine, sentStyle); // Log in black color
        }
        // Send the message to the client
        out.println(message);
        messageTextPane.setText("");
    }

    private void connectToServer(String serverIp, int serverPort) {
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }

            socket = new Socket(serverIp, serverPort);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            logMessage("Connected to server " + serverIp + ":" + serverPort, connectStyle);

            Thread receiveThread = new Thread(() -> {
                try {
                    String serverMessage;
                    while ((serverMessage = in.readLine()) != null) {
                        logMessage("Server: " + serverMessage, defaultStyle);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    disconnectFromServer();
                }
            });
            receiveThread.start();

            connectButton.setEnabled(false);
            disconnectButton.setEnabled(true);

        } catch (IOException e) {
            e.printStackTrace();
            logMessage("Failed to connect to server.", disconnectStyle);
        }
    }

    private void disconnectFromServer() {
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
                logMessage("Disconnected from the server.", connectStyle);
                connectButton.setEnabled(true);
                disconnectButton.setEnabled(false);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void logMessage(String message, Style style) {
        try {
            SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");
            String timestamp = "[" + dateFormat.format(new Date()) + "] ";

            // Check if the message starts with "You:" or "Server:" and apply different styles
            if (message.startsWith("You: ")) {
                chatDoc.insertString(chatDoc.getLength(), timestamp, timeStyle);
                chatDoc.insertString(chatDoc.getLength(), "You: ", messageOwnerStyle); // Apply a different style for "You:"
                chatDoc.insertString(chatDoc.getLength(), message.substring(5) + "\n", style); // Log the rest of the message
            } else if (message.startsWith("Server: ")) {
                chatDoc.insertString(chatDoc.getLength(), timestamp, timeStyle);
                chatDoc.insertString(chatDoc.getLength(), "Server: ", messageOwnerStyle); // Apply a different style for "Server:"
                chatDoc.insertString(chatDoc.getLength(), message.substring(8) + "\n", style); // Log the rest of the message
            } else {
                // For messages without "You:" or "Server:" prefixes, apply default style to the whole message
                chatDoc.insertString(chatDoc.getLength(), timestamp, timeStyle);
                chatDoc.insertString(chatDoc.getLength(), message + "\n", style);
            }
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new TCPClientGUIMain();
        });
    }
}
