package com.developerbiz.gateway;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;


import org.kitteh.irc.client.library.Client;
import org.kitteh.irc.client.library.defaults.feature.DefaultServerInfo;
import org.kitteh.irc.client.library.element.Channel;

public class IRC extends Thread{
	static String IRC = "irc.tacticalchat.com";
	static int IRCPORT = 12345;
	static GatewayComponent gateway = null;
	String xmppFrom = null;
	String xmppTo = null;
	Client client = null;
	String nick = null;
	
	String xmppRoom = ""; //"#trusted-irc-channel-1%irc.tacticalchat.com__12345@gateway.desktop/jeff";
	String ircRoom = "";
	
	public IRC(String from, String to) {
		super();
		System.out.println("from:"+from+", to:"+to);
		this.xmppFrom = to;
		this.xmppTo = from;
		this.nick = from.substring(0,from.indexOf("@"));
		this.xmppRoom = to;
		//this.xmppTo = this.xmppRoom;
		this.ircRoom = to.substring(0,  to.indexOf("%"));
		//this.nick = to.substring(to.indexOf("/")+1);
		System.out.println("irc.Nick:"+this.nick);
		
	}


	public void setGateway(GatewayComponent gateway) {
		this.gateway = gateway;
	}
	
	public void setNick(String nick) {
		this.nick = nick;
	}
	
	public String getNick() {
		return this.nick;
	}
	
	public void run() {
		try {
		    // Displaying the thread that is running
		    System.out.println(
		        "Thread " + Thread.currentThread().getId()
		       + " is running");
		    
			    IRCHandler ircHandler = new IRCHandler();
			    ircHandler.setGateway(gateway);
			    ircHandler.setTo(this.xmppTo);
			    ircHandler.setFrom(xmppFrom);
			    System.out.println("ircRoom:"+ircRoom);
			    client = ircHandler.connect(IRC, this.nick , ircRoom);
			            System.out.println("Client object created");
		        DefaultServerInfo defaultServerInfo = (DefaultServerInfo) client.getServerInfo();

		        String message = "This is the message"; // after MOTD";
		        System.out.println("run.xmppTo:"+this.xmppTo);
		       // ircHandler.sendXMPPResponse(message, this.xmppTo, xmppFrom);
		} catch (Exception e) {
		    // Throwing an exception
		   System.out.println("Exception is caught");
		}
	}
	public void sendIRC(String ircRoom, String message) {
		ircRoom = ircRoom.substring(0,  ircRoom.indexOf("%"));
		client.sendMessage(ircRoom, message);
		System.out.println("Sent to IRC:"+message);
	}
	
	public void leaveRoom(String ircRoom) {
		System.out.println("*** User :"+ client.getNick() + ", leaving room: "+this.ircRoom);
		client.removeChannel(this.ircRoom);
	}
	
	public void joinRoom(String ircRoom) {
        System.out.println("Joining:"+ircRoom);
        //client.addChannel("#ai_whos_on_first"); //room);
        System.out.println("Room:"+ircRoom);
        client.addChannel(ircRoom);

	}
}
	

