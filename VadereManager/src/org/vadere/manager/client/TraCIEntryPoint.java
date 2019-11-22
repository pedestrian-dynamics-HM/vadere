package org.vadere.manager.client;

import py4j.GatewayServer;

import org.vadere.manager.TraCISocket;
import org.vadere.manager.client.traci.PersonAPI;
import org.vadere.manager.client.traci.PolygonAPI;
import org.vadere.manager.client.traci.SimulationAPI;

import java.io.IOException;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.Socket;

public class TraCIEntryPoint {
    protected org.vadere.manager.client.traci.SimulationAPI simulationapi;
    protected org.vadere.manager.client.traci.PolygonAPI polygonapi;
    protected org.vadere.manager.client.traci.PersonAPI personapi;

    public TraCIEntryPoint(TraCISocket socket) {
        simulationapi = new org.vadere.manager.client.traci.SimulationAPI(socket);
        polygonapi = new org.vadere.manager.client.traci.PolygonAPI(socket);
        personapi = new org.vadere.manager.client.traci.PersonAPI(socket);
    }

    public SimulationAPI getSimulationapi() {
        return simulationapi;
    }

    public PolygonAPI getPolygonapi() {
        return polygonapi;
    }

    public PersonAPI getPersonapi() {
        return personapi;
    }

    //todo main....  https://www.py4j.org/getting_started.html
    public static void main(String[] args) throws IOException, InterruptedException {
        int port = 9999;
        TraCISocket traCISocket;
        Socket socket = new Socket();
        socket.setTcpNoDelay(true);
        int waitTime = 500; //ms
        System.out.println("Connect to 127.0.0.1:" + port);
        for (int i = 0; i < 14; i++) {
            try {
                socket.connect(new InetSocketAddress("127.0.0.1", port));
                break;
            } catch (ConnectException ex) {
                Thread.sleep(waitTime);
                waitTime *= 2;
            }
        }

        if (!socket.isConnected()) {
            System.out.println("can't connect to Server!");
            return;
        }

        System.out.println("connected...");
        traCISocket = new TraCISocket(socket);

        GatewayServer gatewayServer = new GatewayServer(new TraCIEntryPoint(traCISocket));
        gatewayServer.start();
        System.out.println("Gateway Server Started");
    }
}