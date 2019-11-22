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

    class TraCIControll{

        TraCISocket socket;
        String basePath = "";
        String defaultScenario = "";

        TraCIControll(TraCISocket socket, String basePath, String defaultScenario){
            this.socket = socket;
            this.basePath = basePath;
            this.defaultScenario = defaultScenario;
        }

        public void getVersion() throws IOException {
            TraCIPacket p = TraCIGetVersionCommand.build();
            socket.sendExact(p);

            TraCIPacketBuffer buf = socket.receiveExact();
            TraCIResponse cmd = buf.nextResponse();

            System.out.println(cmd.toString());

        }

        public void close(String[] args) throws IOException {

            socket.sendExact(TraCICloseCommand.build());

            TraCIResponse cmd = socket.receiveResponse();
            System.out.println(cmd);

            System.out.println("Bye");
        }

        public void nextSimTimeStep(String[] args) throws IOException{
            double nextSimTime = -1.0;

            if (args.length > 1)
                nextSimTime = Double.parseDouble(args[1]);

            TraCIPacket packet = TraCISimStepCommand.build(nextSimTime);
            socket.sendExact(packet);

            TraCISimTimeResponse cmd = (TraCISimTimeResponse) socket.receiveResponse();
            System.out.println(cmd.toString());
        }

        public void sendFile(String[] args) throws IOException {

            String filePath;

            if (args.length > 1) {
                if (!basePath.isEmpty()){
                    filePath = Paths.get(basePath, args[1] + ".scenario").toString();
                } else {
                    filePath = args[1];
                }
            } else {
                if (!basePath.isEmpty() && !defaultScenario.isEmpty()){
                    filePath = Paths.get(basePath, defaultScenario).toString();
                    System.out.println("use default " + defaultScenario);
                } else {
                    System.out.println("no default scenario set");
                    return;
                }
            }

            String data;
            try{
                data = IOUtils.readTextFile(filePath);
            } catch (IOException e){
                System.out.println("File not found: " + filePath);
                return;
            }

            TraCIPacket packet = TraCISendFileCommand.TraCISendFileCommand("Test", data);

            traCISocket.sendExact(packet);

            TraCIPacketBuffer buf = traCISocket.receiveExact();
            TraCIResponse cmd = buf.nextResponse();

            System.out.println(cmd.toString());
        }
    }
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