package org.vadere.manager.client;

import org.vadere.manager.traci.commands.control.TraCICloseCommand;
import org.vadere.manager.traci.commands.control.TraCIGetVersionCommand;
import org.vadere.manager.traci.commands.control.TraCISendFileCommand;
import org.vadere.manager.traci.commands.control.TraCISimStepCommand;
import org.vadere.manager.traci.reader.TraCIPacketBuffer;
import org.vadere.manager.traci.respons.TraCIResponse;
import org.vadere.manager.traci.respons.TraCISimTimeResponse;
import org.vadere.manager.traci.writer.TraCIPacket;
import org.vadere.util.io.IOUtils;
import py4j.GatewayServer;

import org.vadere.manager.TraCISocket;
import org.vadere.manager.client.traci.PersonAPI;
import org.vadere.manager.client.traci.PolygonAPI;
import org.vadere.manager.client.traci.SimulationAPI;

import java.io.IOException;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.file.Paths;

public class TraCIEntryPoint implements Runnable {
    protected org.vadere.manager.client.traci.SimulationAPI simulationapi;
    protected org.vadere.manager.client.traci.PolygonAPI polygonapi;
    protected org.vadere.manager.client.traci.PersonAPI personapi;
    protected TraCIControll traciControll;

    private boolean running;
    private int port;
    private TraCISocket traCISocket;
    private String basePath = "";
    private String defaultScenario = "";

    TraCIEntryPoint(int port) {
        this.port = port;
    }

    public static void main(String[] args) throws IOException, InterruptedException {

        TraCIEntryPoint entryPoint = new TraCIEntryPoint(9999);
        entryPoint.run();
    }

    private void init() {
        simulationapi = new org.vadere.manager.client.traci.SimulationAPI(traCISocket);
        polygonapi = new org.vadere.manager.client.traci.PolygonAPI(traCISocket);
        personapi = new org.vadere.manager.client.traci.PersonAPI(traCISocket);
        traciControll = new TraCIControll(traCISocket, "/Users/Philipp/Repos/vadere/Scenarios/Demos/roVer/scenarios/", "scenario002.scenario");
    }

    private void establishConnection() throws IOException, InterruptedException {
        Socket socket = new Socket();
        socket.setTcpNoDelay(true);
        int waitTime = 500; //ms
        System.out.println("Connect to 127.0.0.1:" + this.port);
        for (int i = 0; i < 14; i++) {
            try {
                socket.connect(new InetSocketAddress("127.0.0.1", this.port));
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

        running = true;
    }

    synchronized private void handleConnection() throws IOException, InterruptedException {
        try {
            init();
            GatewayServer gatewayServer = new GatewayServer(this);
            gatewayServer.start();
            while(true){
                wait(10000);
            }
        } finally {
            if (traCISocket != null)
                traCISocket.close();
        }
    }

    @Override
    public void run() {

        try {
            establishConnection();
            handleConnection();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

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

    public TraCIControll getTraciControll() { return traciControll; }

}