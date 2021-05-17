package org.vadere.simulator.entrypoints.cmd.commands;

import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.Namespace;
import org.vadere.simulator.entrypoints.cmd.SubCommandRunner;

import java.util.HashMap;
import java.util.Objects;

public class CommandWrapper {

    private SubCommandRunner runner;
    private String help;
    private HashMap<String, Object> args;

    public CommandWrapper(SubCommandRunner runner,String help, HashMap<String, Object> args) {
        this.runner = runner;
        this.help = help;
        this.args = Objects.requireNonNullElseGet(args, HashMap::new);
    }

    public void run(Namespace ns, ArgumentParser parser) throws Exception {
        String args = ns.getString("args");
        if (!args.equals("")){
            String[] kvPairs = args.split(";");

            for (String kvPair : kvPairs) {
                String[] kv = kvPair.split("=");
                if (kv.length == 1) {
                    // key only
                    this.args.put(kv[0], true);
                } else if (kv.length == 2) {
                    this.args.put(kv[0], kv[1]);
                } else {
                    throw new Exception(String.format("Expected 1 or 2 items in key/value pair got %d", kv.length));
                }
            }
        }
        this.runner.run(ns, parser, this.args);
    }


    public String getHelp() {
        StringBuilder sb = new StringBuilder();
        sb.append("[").append(this.help);
        for(var e: this.args.entrySet()){
            sb.append(e.getKey()).append("=").append(e.getValue()).append(";");
        }
        int last = sb.lastIndexOf(";");
        if (last > 0){
            sb.deleteCharAt(last).append("]");
        }
        return sb.toString();
    }
}
