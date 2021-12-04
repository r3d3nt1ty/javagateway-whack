package com.developerbiz.gateway;

import org.dom4j.DocumentHelper;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

import org.dom4j.Document;
import org.dom4j.Element;
import org.xmpp.component.Component;
import org.xmpp.component.ComponentManager;
import org.xmpp.component.ComponentManagerFactory;
import org.xmpp.component.ComponentException;
import org.xmpp.packet.*;

public class GatewayComponent implements Component {
	public static final String COMPONENT_NS = "jabber:component:accept",
			PING_NS = "urn:xmpp:ping",
			MUC_NS = "http://jabber.org/protocol/muc",
			MUC_USER_NS = MUC_NS + "#user",
			MUC_ADMIN_NS = MUC_NS + "#admin",
			STANZA_NS = "urn:ietf:params:xml:ns:xmpp-stanzas",
			XHTMLIM_NS = "http://jabber.org/protocol/xhtml-im",
			STABLE_ID_NS = "urn:xmpp:sid:0",
			ADHOC_NS = "http://jabber.org/protocol/commands",
			REG_SRV_NS = "tacticalsolutions:iq:registeredServers",
			REG_SRV_RSP_NS = REG_SRV_NS + "#response",
			DISCO_NS = "http://jabber.org/protocol/disco",
			DISCO_ITEMS_NS = DISCO_NS + "#items",
			DISCO_INFO_NS = DISCO_NS + "#info",
			MUC_TRAFFIC_NS = "http://jabber.org/protocol/muc#traffic",
			VERSION_NS = "jabber:iq:version",
			RSM_NS = "http://jabber.org/protocol/rsm",
			MAM_NS = "urn:xmpp:mam:2",
			STABLE_MUC_ID_NS = "http://jabber.org/protocol/muc#stable_id",
			DATAFORM_NS = "jabberdata";
	static String IRC = "irc.tacticalchat.com",
				  IRCPORT = "12345";
	
	HashMap<JID, JID> activeUsers = new HashMap<>();
	IRC irc = null;
	
	public GatewayComponent(String[] args) {
		super();
		for (String arg: args) {
			String [] splitArg = arg.split("=");
			if (splitArg[0].equalsIgnoreCase("IRC")) {
				IRC = splitArg[1];
			} else if (splitArg[0].equalsIgnoreCase("IRCPORT")) {
				IRCPORT = splitArg[1];
			}
		}
	}
	
    public String getName() {
        return "Gateway";
    }

    public String getDescription() {
        return "IRC Bridge for XMPP";
    }
    
    
    public void processPacket(Packet packet) {
        System.out.println("Received packet:"+packet.toXML());
        
        Document doc = DocumentHelper.createDocument();
        
        // Only process Message packets
        if (packet instanceof Message) {
            Message message = (Message) packet;
            // String body = message.getBody(); // <-- use this to get message body
         
            // Build the answer
            Message reply = new Message();
            reply.setTo(message.getFrom());
            reply.setFrom(message.getTo());
            reply.setType(message.getType());
            reply.setBody(message.getBody());
            sendIRC(message.getTo().toString(), message.getBody());
            System.out.println("Sent message to IRC");
        }
        
        if(packet instanceof IQ ) {
        	IQ message = (IQ) packet;
        	String xmlns = message.getElement().element("query").getNamespaceURI();
        	
        	if(xmlns == VERSION_NS) {
	            // Build the answer
	            IQ reply = new IQ();
	            Element query = doc.addElement("query");
	            query.addAttribute("xmlns", VERSION_NS);
	            
	            Element name = query.addElement("name");
	            name.addText("1.0");
	            reply.setChildElement(query);
	            
	            reply.setTo(message.getFrom());
	            reply.setFrom(message.getTo());
	            reply.setType(IQ.Type.result);
	           
	            // Send the response to the sender of the request
	            try {
	                ComponentManagerFactory.getComponentManager().sendPacket(this, reply);
	                System.out.println("sent packet:"+reply.toXML());
	
	            } catch (ComponentException e) {
	                e.printStackTrace();
	            }
	            
        	} else if(xmlns == DISCO_INFO_NS) {
  
	            // Build the answer
	            IQ reply = new IQ();
	            Element query = doc.addElement("query");
	            
	            //String[] ns = {DISCO_INFO_NS, MUC_NS, ADHOC_NS, PING_NS, MAM_NS, VERSION_NS, STABLE_MUC_ID_NS};
	            String[] ns = {DISCO_INFO_NS,VERSION_NS};

	            for(String e: ns) {
	                Element feature = query.addElement("feature");
	                feature.addAttribute("var",  e);
	            }
	            
	            query.addAttribute("xlmns", DISCO_INFO_NS);
	            
	            Element identity = query.addElement("identity");
	            identity.addAttribute("category", "gateway");
	            identity.addAttribute("type", "irc");
	            identity.addAttribute("name", "Trusted Chat XMPP-IRC gateway");
	            
	            reply.setChildElement(query);
	            
	            reply.setTo(message.getFrom());
	            reply.setFrom(message.getTo());
	            reply.setType(IQ.Type.result);
	           
	            // Send the response to the sender of the request
	            try {
	                ComponentManagerFactory.getComponentManager().sendPacket(this, reply);
	                System.out.println("sent packet:"+reply.toXML());
	
	            } catch (ComponentException e) {
	                e.printStackTrace();
	            }
        	} 
        } else if (packet instanceof Presence) {
        	Presence presence = (Presence) packet;
    		Message reply = new Message();

    		System.out.println("Received Presence:"+presence+", JID:"+presence.getFrom());
    		// Change this to maintain a hash of "rooms" with lists of users... So that a user (from) can 
    		// be in multiple rooms as the same time
    		if (presence.getType() == null) {
    			activeUsers.put(presence.getFrom(), presence.getFrom());
    			System.out.println("Adding "+presence.getFrom()+" to active users list, type:"+presence.getType());
    			doIRC(presence.getFrom().toString(), presence.getTo().toString());
    		} else if (presence.getType().toString().equals("unavailable") ) {
    			System.out.println("Removing "+presence.getFrom()+" from active users list, type:"+presence.getType());
    			activeUsers.remove(presence.getFrom());
    			if (irc != null) {
    				irc.leaveRoom(presence.getTo().toString());
    			}
    		} else if (presence.getType().toString().equalsIgnoreCase("subscribe")) {
    			System.out.println("We have someone that joined!");
    		}
    		Set<JID> set = activeUsers.keySet();
    		Iterator<JID> it = set.iterator();
    		while (it.hasNext())
    		{
    			System.out.println("User in active list:"+it.next());
    		}
    		/*System.out.println("GatewayComponent: sending message to:"+presence.getFrom());
    		reply.setTo(presence.getFrom());
    		System.out.println("GatewayComponent: SendingMessage From:"+presence.getTo());
    		reply.setFrom(presence.getTo());
    		reply.setBody("send a response to the xmpp receiveing thread");
            try {
                ComponentManagerFactory.getComponentManager().sendPacket(this, reply);
            } catch (ComponentException e) {
                e.printStackTrace();
            }*/
    	}
    }
    
    public void sendPresence(String user, String xmppTo, String xmppFrom) {
        Presence reply = new Presence();
        //reply.setID(user);
        reply.setFrom(xmppFrom);
        reply.setTo(xmppTo);
        reply.setType(Presence.Type.subscribe);
        System.out.println("Presence:"+reply.toXML());
        try {
            ComponentManagerFactory.getComponentManager().sendPacket(this, reply);
        } catch (ComponentException e) {
            e.printStackTrace();
        }
        Message message = new Message();
		System.out.println("GatewayComponent: sending message to:"+xmppTo);
		message.setTo(xmppTo);
		System.out.println("GatewayComponent: SendingMessage From:"+xmppFrom);
		message.setFrom(xmppFrom);
		message.setBody(xmppTo + " Joined!");
        try {
            ComponentManagerFactory.getComponentManager().sendPacket(this, message);
        } catch (ComponentException e) {
            e.printStackTrace();
        }
    }

    public void initialize(JID jid, ComponentManager componentManager) {
    }

    public void start() {
    }

    public void shutdown() {
    }
    
    private void doIRC(String jid, String room) {
    	System.out.println("jid:"+jid);
    	irc = new IRC(jid, room);
    	irc.setGateway(this);
    	irc.start();
    }
    
    private void sendIRC(String room, String message) {
    	System.out.println("Room/From:"+room+", Message/Body"+message);
    	irc.sendIRC(room, message);
    	
    }
}
