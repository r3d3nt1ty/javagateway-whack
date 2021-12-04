package com.developerbiz.gateway;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.kitteh.irc.client.library.Client;
import org.kitteh.irc.client.library.defaults.DefaultClient;
import org.kitteh.irc.client.library.element.Channel;
import org.kitteh.irc.client.library.element.ServerMessage;
import org.kitteh.irc.client.library.element.User;
import org.kitteh.irc.client.library.event.channel.ChannelJoinEvent;
import org.kitteh.irc.client.library.event.channel.ChannelKickEvent;
import org.kitteh.irc.client.library.event.channel.ChannelMessageEvent;
import org.kitteh.irc.client.library.event.channel.ChannelNamesUpdatedEvent;
import org.kitteh.irc.client.library.event.channel.ChannelPartEvent;
import org.kitteh.irc.client.library.event.client.ClientReceiveMotdEvent;
import org.kitteh.irc.client.library.event.client.NickRejectedEvent;
import org.kitteh.irc.client.library.event.user.ServerNoticeEvent;
import org.xmpp.component.ComponentException;
import org.xmpp.component.ComponentManagerFactory;
import org.xmpp.packet.Message;
import org.kitteh.irc.client.library.event.connection.ClientConnectionEndedEvent;
import net.engio.mbassy.listener.Listener;
import net.engio.mbassy.listener.Handler;

@Listener
public class IRCHandler {
	DefaultClient client;
	String room = null;
	String nick = null;
	String from = null;
	String to = null;
	GatewayComponent gateway = null;
	
	public DefaultClient connect(String host, String nick, String room) {
		System.out.println("Nick:"+nick+", Room:"+room);
		this.nick = nick; //gateway.irc.getNick();
		this.room = room;
		String realName = nick.substring(nick.indexOf("/")+1);
		System.out.println("RealName:"+ realName);
//        this.client = (DefaultClient)Client.builder().realName("jeff").nick(nick).server().host(host).port(12345, Client.Builder.Server.SecurityType.INSECURE).password("jeff1029384756!a").then().buildAndConnect();
        this.client = (DefaultClient)Client.builder().realName(nick.substring(nick.indexOf("/")+1)).nick(nick).server().host(host).port(12345, Client.Builder.Server.SecurityType.INSECURE).then().buildAndConnect();
        this.client.getEventManager().registerEventListener(this);
        return this.client;
		
	}
	public DefaultClient getClient() {
		return this.client;
	}
	
	public void setGateway(GatewayComponent gateway) {
		this.gateway = gateway;
	}
	
	public void setTo(String to) {
		this.to = to;
	}
	
	public void setFrom(String from) {
		this.from = from;
	}
	
	@Handler
	public void onServerNoticeEvent(ServerNoticeEvent event) {
		System.out.println("ServerNotice Event");
	}
	
	@Handler
	public void onNickRejectedEvent(NickRejectedEvent event) {
		System.out.println("Nick Rejected");
	}
	
	@Handler
	public void onClientReceivedMotdEvent(ClientReceiveMotdEvent event) {
		System.out.println("MOTD event");
		System.out.println("******:"+event.getClient().getChannels().size());
		System.out.println(event.getMotd());
		Optional<List<String>> motd = event.getMotd();
		List<String> motdArray = motd.get();
		StringBuilder sb = new StringBuilder();
		for (String m : motdArray) {
			sb.append(m).append("\n");
			m = m.replaceAll("\\p{C}", " ");
			System.out.println("**:"+m);
		}
		System.out.println("MOTD: "+sb.toString().replaceAll("\\p{C}", " "));
		System.out.println("****** FROM:"+from);
		if (from.contains("/")) {
			sendXMPPResponse(sb.toString().replaceAll("\\p{C}", " "),to,from);
			System.out.println("***** ROOM:"+room);
			gateway.irc.joinRoom(room);
		}
		
	}
	
	@Handler
	public void onChannelNameUpdatedEvent(ChannelNamesUpdatedEvent event) {
		System.out.println("ChannelNameUpdatedEvent");
		List<ServerMessage> names = event.getSource();
		for (int ix=0;ix < names.size(); ix++) {
			System.out.println(names.get(ix).toString());
		}
	}
	
	@Handler
	public void onChannelMessageEvent(ChannelMessageEvent event) {
		System.out.println("ChannelMessageEvent");
		System.out.println(event.getMessage());
		sendXMPPResponse(event.getMessage(), to, from);
	}

    @Handler
    public void onJoin(ChannelJoinEvent event) {
        Channel channel = event.getChannel();
        channel.sendMessage("Hi " + event.getUser().getNick() + "!");
        System.out.println("!!!!!!!!User:"+event.getUser().getNick() + " joined");
        List<User> usersList = channel.getUsers();
        for (User m : usersList) {
        	System.out.println("M.Nick:"+m.getNick()+", number of users in list:"+usersList.size());
    		String userInfo = m.getNick();
    		sendXMPPPresence(userInfo);
    	}
    }
        
    @Handler 
    public void onLeave(ChannelPartEvent event) {
       	System.out.println("User quit the room!?");
    }
        
    @Handler
    public void onKick(final ChannelKickEvent e) {
        System.out.println("Kicked from " + e.getChannel().getName() + ".");
    }
        
    @Handler
    public void onConnectionEnded(ClientConnectionEndedEvent ccee) {
      	System.out.println("Connection ended:"+ccee.toString());
    }
        
    public void sendXMPPResponse(String message, String to, String from) {
      	Message xmlMessage = new Message();
       	System.out.println("sendXMPPResponse.To:"+to);
   		xmlMessage.setTo(to);
   		System.out.println("From:"+from);
   		xmlMessage.setFrom(from); //+"/"+this.nick);
   		xmlMessage.setBody(message);
   		xmlMessage.setType(Message.Type.normal);
        try {
            ComponentManagerFactory.getComponentManager().sendPacket(gateway, xmlMessage);
        } catch (ComponentException e) {
            e.printStackTrace();
        }
    }
        
    public void sendXMPPPresence(String user) {
       	gateway.sendPresence(user, this.to, this.from);
    }

}
