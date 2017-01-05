/**
 * Copyright (c) 2014-2016 by the respective copyright holders.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.mihome.internal.socket;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * Takes care of the communication with the bridge.
 *
 * @author Patrick Boos - Initial contribution
 */
public class XiaomiSocket {
    private static final int BUFFER_LENGTH = 1024;
    // private final int DEST_PORT = 9898;
    private static final String MCAST_ADDR = "224.0.0.50";
    private static final int MCAST_PORT = 4321;
    private static final int DEFAULT_PORT = 9898;
    private static byte[] buffer = new byte[BUFFER_LENGTH];
    private static DatagramPacket datagramPacket = new DatagramPacket(buffer, buffer.length);

    private static MulticastSocket socket = null;
    private static Thread socketReceiveThread;

    private static List<XiaomiSocketListener> listeners = new ArrayList<>();

    private static final JsonParser parser = new JsonParser();

    private static Logger logger = LoggerFactory.getLogger(XiaomiSocket.class);

    public static void registerListener(XiaomiSocketListener listener) {
        listeners.add(listener);

        if (socket == null) {
            setupSocket();
        }
    }

    public static void unregisterListener(XiaomiSocketListener listener) {
        listeners.remove(listener);

        if (listeners.isEmpty() && socket != null) {
            closeSocket();
        }
    }

    private static void setupSocket() {
        synchronized (XiaomiSocket.class) {
            try {
                logger.debug("Setup socket");
                // TODO check if this can be any port. if yes, than we can have a socket for each discovery, and each
                // bridge
                socket = new MulticastSocket(DEFAULT_PORT); // must bind receive side
                socket.joinGroup(InetAddress.getByName(MCAST_ADDR));
                logger.debug("network interface: {}", socket.getNetworkInterface().getName());
            } catch (IOException e) {
                logger.error("Setup socket error", e);
            }

            socketReceiveThread = new ReceiverThread();
            socketReceiveThread.start();
        }
    }

    private static void closeSocket() {
        synchronized (XiaomiSocket.class) {
            if (socketReceiveThread != null) {
                socketReceiveThread.interrupt();
            }
            if (socket != null) {
                logger.debug("Socket closed");
                socket.close();
                socket = null;
            }
        }
    }

    public static void sendMessage(String message) {
        try {
            sendMessage(message, InetAddress.getByName(MCAST_ADDR), MCAST_PORT);
        } catch (UnknownHostException e) {
            logger.error("Sending error", e);
        }
    }

    public static void sendMessage(String message, InetAddress address, int port) {
        try {
            byte[] sendData = message.getBytes("UTF-8");
            DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, address, port);
            socket.send(sendPacket);
            logger.debug("Sent message: {}", message);
        } catch (IOException e) {
            logger.error("Sending error", e);
        }
    }

    private static class ReceiverThread extends Thread {
        @Override
        public void run() {
            receiveData(socket, datagramPacket);
        }

        private void receiveData(MulticastSocket socket, DatagramPacket dgram) {
            try {
                while (true) {
                    socket.receive(dgram);
                    String sentence = new String(dgram.getData(), 0, dgram.getLength());

                    JsonObject message = parser.parse(sentence).getAsJsonObject();
                    String command = message.get("cmd").getAsString();

                    // new ArrayList to avoid concurrent modification
                    for (XiaomiSocketListener listener : new ArrayList<>(listeners)) {
                        listener.onDataReceived(command, message);
                    }
                }
            } catch (IOException e) {
                if (!isInterrupted()) {
                    logger.error("Error while receiving", e);
                }
            }

            logger.debug("Receiver thread ended");
        }
    }
}
