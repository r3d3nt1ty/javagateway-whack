package com.developerbiz.gateway;

import org.jivesoftware.whack.ExternalComponentManager;
import org.xmpp.component.ComponentException;

public class ExternalGatewayComponent {

	static String IP = "";
	static String PORT = "";
    public static void main(String[] args) {
        // Create a manager for the external components that will connect to the server "localhost"
        // at the port 5225
    	
    	for (String arg:args) {
    		String [] argSplit = arg.split("=");
    		if (argSplit[0].equalsIgnoreCase("ip")) {
    			IP = argSplit[1];
    		} else if (argSplit[0].equalsIgnoreCase("port")) {
    			PORT = argSplit[1];
    		}
    		System.out.println(argSplit[0]+","+argSplit[1]);
    	}
    	// MAV NOTE: replace "localhost" with IP address...
    	// JH Note: replaced "localhost" with IP
        final ExternalComponentManager manager = new ExternalComponentManager(IP, Integer.parseInt(PORT), false); //"99.15.113.82", 5275, false);

        manager.setSecretKey("gateway", "password");
        // Set the manager to tag components as being allowed to connect multiple times to the same
        // JID.
        manager.setMultipleAllowed("gateway", false);
        manager.setProperty("name", "Sentinel gateway");

        try {
            // Register that this component will be serving the given subdomain of the server
            manager.addComponent("gateway", new GatewayComponent(args));

            while (true) {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        } catch (ComponentException e) {
            e.printStackTrace();
        }
    }
}
