/*
 * Author: Philipp Schuegraf
 */

package org.vadere.manager.client;

import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.impl.Arguments;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.Namespace;
import net.sourceforge.argparse4j.internal.HelpScreenException;
import org.vadere.manager.Manager;
import org.vadere.manager.TraCISocket;
import org.vadere.manager.VadereServer;
import org.vadere.manager.client.traci.PersonAPI;
import org.vadere.manager.client.traci.PolygonAPI;
import org.vadere.manager.client.traci.SimulationAPI;
import org.vadere.util.logging.Logger;
import py4j.GatewayServer;

import java.io.IOException;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Paths;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TraCIEntryPoint implements Runnable {

    static Logger logger;

    protected org.vadere.manager.client.traci.SimulationAPI simulationapi;
    protected org.vadere.manager.client.traci.PolygonAPI polygonapi;
    protected org.vadere.manager.client.traci.PersonAPI personapi;
    protected TraCIControll traciControll;

    private boolean running;
    private int port;
    private TraCISocket traCISocket;
    private String basePath = "";
    private String defaultScenario = "";

    public static void main(String[] args) throws IOException, InterruptedException {

        Logger.setMainArguments(args);
        logger = Logger.getLogger(Manager.class);
        ArgumentParser p = createArgumentParser();
        Namespace ns;

        try {
            ns = p.parseArgs(args);
            ExecutorService pool = Executors.newFixedThreadPool(ns.getInt("clientNum"));
            ServerSocket serverSocket = new ServerSocket(ns.getInt("port"));
            logger.infof("Start Server(%s) with Loglevel: %s", VadereServer.currentVersion.getVersionString(), logger.getLevel().toString());
            Thread serverThread = new Thread(new VadereServer(serverSocket, pool, Paths.get(ns.getString("output-dir")), ns.getBoolean("guiMode")));
            serverThread.start();
            TraCIEntryPoint entryPoint = new TraCIEntryPoint(ns.getInt("port"));
            entryPoint.run();
        } catch (HelpScreenException ignored) {

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static ArgumentParser createArgumentParser() {
        ArgumentParser parser = ArgumentParsers.newArgumentParser("Vadere Server")
                .defaultHelp(true)
                .description("Runs the VADERE pedestrian simulator as a server for Python.");

        addOptionsToParser(parser);

        return parser;
    }

    private static void addOptionsToParser(ArgumentParser parser) {
        // no action required call to  Logger.setMainArguments(args) already configured Logger.
        parser.addArgument("--loglevel")
                .required(false)
                .type(String.class)
                .dest("loglevel")
                .choices("OFF", "FATAL", "ERROR", "WARN", "INFO", "DEBUG", "TRACE", "ALL")
                .setDefault("INFO")
                .help("Set Log Level.");

        // no action required call to  Logger.setMainArguments(args) already configured Logger.
        parser.addArgument("--logname")
                .required(false)
                .type(String.class)
                .dest("logname")
                .help("Write log to given file.");


        // no action required call to  Logger.setMainArguments(args) already configured Logger.
        parser.addArgument("--port")
                .required(false)
                .type(Integer.class)
                .setDefault(9999)
                .dest("port")
                .help("Set port number.");

        // no action required call to  Logger.setMainArguments(args) already configured Logger.
        parser.addArgument("--clientNum")
                .required(false)
                .type(Integer.class)
                .setDefault(4)
                .dest("clientNum")
                .help("Set number of clients to manager. Important: Each client has a separate simulation. No communication between clients");

        // boolean switch to tell server to start in gui mode.
        parser.addArgument("--gui-mode")
                .required(false)
                .action(Arguments.storeTrue())
                .type(Boolean.class)
                .dest("guiMode")
                .help("Start server with GUI support. If a scenario is received show the current state of the scenario");

        parser.addArgument("--output-dir", "-o")
                .required(false)
                .setDefault("./vadere-server-output")
                .dest("output-dir") // set name in namespace
                .type(String.class)
                .help("Supply output directory as base directory for received scenarios.");
    }

    TraCIEntryPoint(int port) {
        this.port = port;
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