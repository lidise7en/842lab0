/* 18-842 Distributed Systems
 * Lab 0
 * Group 41 - ajaltade & dil1
 */

import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;


public class MessagePasser {
	
	private LinkedList<Message> delaySendQueue = new LinkedList<Message>(); //store the delayed send msg
	private LinkedList<Message> delayRecvQueue = new LinkedList<Message>(); //store the delayed recv msg
	private LinkedList<Message> recvQueue = new LinkedList<Message>(); //store all the received msg from all receive sockets
	private HashMap<String, ObjectOutputStream> outputStreamMap = new HashMap<String, ObjectOutputStream>();
	private Map<SocketInfo, Socket> sockets = new HashMap<SocketInfo, Socket>();


	private String configFilename;
	private String localName;
	private ServerSocket hostListenSocket;
	private SocketInfo hostSocketInfo;
	private Config config;
	private static int currSeqNum;
	
	private enum RuleType {
		SEND,
		RECEIVE,
	}
	/*
	 * sub-class for listen threads
	 */
	
	public class startListen extends Thread {

		public startListen() {

		}
		public void run() {
			System.out.println("Running");
			try {
				while(true) {
					Socket sock = hostListenSocket.accept();	
					new ListenThread(sock).start();		
				}
			}catch(IOException e) {
				Thread.currentThread().interrupt();
			}
		}
	}

	public class ListenThread extends Thread {
		private Socket LisSock = null;

		public ListenThread(Socket sock) {
			this.LisSock = sock;
		}
		public void run() {

			try {
				ObjectInputStream in = new ObjectInputStream(this.LisSock.getInputStream());

				while(true) {
					Message msg = (Message)in.readObject();

					parseConfig();
					Rule rule = null;
					if((rule = matchRule(msg, RuleType.RECEIVE)) != null) {
						if(rule.getAction().equals("drop")) {
							synchronized (delayRecvQueue) {
								while(!delayRecvQueue.isEmpty()) {
									recvQueue.add(delayRecvQueue.pollLast());
								}
							}
							continue;
						}
						else if(rule.getAction().equals("duplicate")) {
							System.out.println("Duplicating message");
							synchronized(recvQueue) {
								recvQueue.add(msg);
								recvQueue.add(msg.makeCopy());
								
								synchronized (delayRecvQueue) {
									while(!delayRecvQueue.isEmpty()) {
										recvQueue.add(delayRecvQueue.pollLast());
									}
								}
							}
						}
						else if(rule.getAction().equals("delay")) {
							synchronized(delayRecvQueue) {
								delayRecvQueue.add(msg);
							}
						}
						else {
							System.out.println("We receive a wierd msg!");
						}
					}
					else {
						synchronized(recvQueue) {
							recvQueue.add(msg);
							synchronized (delayRecvQueue) {
								while(!delayRecvQueue.isEmpty()) {
									recvQueue.add(delayRecvQueue.pollLast());
								}
							}
						}
					}

				}
			} catch (EOFException e2) {
				System.out.println("A peer disconnected");
				for (Map.Entry<SocketInfo, Socket> entry : sockets.entrySet()) {
				    if(this.LisSock.getRemoteSocketAddress().equals(entry.getValue().getLocalSocketAddress())) {
				    	System.out.println("Lost connection to " + entry.getKey().getName());
				    	try {
							ObjectOutputStream out = outputStreamMap.get(entry.getKey().getName());
						   	outputStreamMap.remove(entry.getKey().getName());
							out.close();
	
						   	sockets.remove(entry.getKey());
							entry.getValue().close();
						} catch (IOException e) {
							//  Auto-generated catch block
							e.printStackTrace();
						}
				    	break ;
				    }
				}
				
			} catch (IOException e1) {
				//  Auto-generated catch block
				e1.printStackTrace();
			} catch (ClassNotFoundException e) {
				//  Auto-generated catch block
				e.printStackTrace();
			}
		}

		
	}

	public MessagePasser(String configuration_filename, String local_name) {
		configFilename = configuration_filename;
		localName = local_name;
		currSeqNum = 1;
		
		try {
			parseConfig();
		} catch (FileNotFoundException e) {
			//  Auto-generated catch block
			e.printStackTrace();
		}
		
		/* Now, using localName get *this* MessagePasser's SocketInfo and
		 * setup the listening socket and all other sockets to other hosts.
		 * 
		 * We can optionally, save this info in hostSocket and hostSocketInfo
		 * to avoid multiple lookups into the 'sockets' Map.
		 */
		hostSocketInfo = config.getConfigSockInfo(localName);
		if(hostSocketInfo == null) {
			/*** ERROR ***/
			System.out.println("The local name is not correct.");
			System.exit(0);
		}
		else {
			/* Set up socket */
			System.out.println("For this host: " + hostSocketInfo.toString());
			try {
				hostListenSocket = new ServerSocket(hostSocketInfo.getPort(), 10, 
						                     InetAddress.getByName(hostSocketInfo.getIp()));
			} catch (IOException e) {
				/*** ERROR ***/
				System.out.println("Cannot start listen on socket. "+ e.toString());
				System.exit(0);
			}
			/*start the listen thread */
			new startListen().start();

		}
	}
	
	public void send(Message message) {
		/* Re-parse the config.
		 * Check message against sendRules.
		 * Finally, send the message using sockets.
		 */
		
		try {
			parseConfig();
		} catch (FileNotFoundException e) {
			//  Auto-generated catch block
			e.printStackTrace();
		}
		message.set_source(localName);
		message.set_seqNum(currSeqNum++);
				
		Rule rule = null;
		if((rule = matchRule(message, RuleType.SEND)) != null) {
			if(rule.getAction().equals("drop")) {
				return ;
			}
			else if(rule.getAction().equals("duplicate")) {
				Message dupMsg = message.makeCopy();
				dupMsg.set_duplicate(true);
				
				
				/* Send 'message' and 'dupMsg' */
				doSend(message);
				doSend(dupMsg);
				
				/* We need to send delayed messages after new message.
				 * This was clarified in Live session by Professor.
				 */
				for(Message m : delaySendQueue) {
					doSend(m);
				}
				delaySendQueue.clear();

				
			}
			else if(rule.getAction().equals("delay")) {
				delaySendQueue.add(message);
			}
			else {
				System.out.println("We get a wierd message here!");
			}
		}
		else {
			doSend(message);
			
			/* We need to send delayed messages after new message.
			 * This was clarified in Live session by Professor.
			 */
			for(Message m : delaySendQueue) {
				doSend(m);
			}
			delaySendQueue.clear();
		}
		
	}
	
	private void doSend(Message message) {
		String dest = message.getDest();
		Socket sendSock = null;
		for(SocketInfo inf : sockets.keySet()) {
			if(inf.getName().equals(dest)) {
				sendSock = sockets.get(inf);
				break;
			}
		}
		if(sendSock == null) {
			try {
				SocketInfo inf = config.getConfigSockInfo(dest);
				if(inf == null) {
					System.out.println("Cannot find config for " + dest);
					return ;
				}
				sendSock = new Socket(inf.getIp(), inf.getPort());
			} catch(ConnectException e2) { 
				System.out.println("Connection refused to " + dest);
				return ;
			}catch (IOException e) {
				//  Auto-generated catch block
				e.printStackTrace();
				return ;
			}
			sockets.put(config.getConfigSockInfo(dest), sendSock);
			try {
				outputStreamMap.put(dest, new ObjectOutputStream(sendSock.getOutputStream()));
			} catch (IOException e) {
				//  Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		ObjectOutputStream out;
		try {
			out = outputStreamMap.get(dest);
			
			out.writeObject(message);
			out.flush();
			
		} catch (SocketException e1) {
			System.out.println("Peer " + dest + " is offline. Cannot send");
			
		} catch (IOException e) {
			//  Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public Message receive() {
		/* Re-parse the config.
		 * Receive the message using sockets.
		 * Finally, check message against receiveRules.
		 */
		
		synchronized(recvQueue) {
			if(!recvQueue.isEmpty()) {
				Message popMsg = recvQueue.remove();
				return popMsg;
			}
		}
		
		return null;
	}
				

	public Rule matchRule(Message message, RuleType type) {
		List<Rule> rules = null;
		
		if(type == RuleType.SEND) {
			rules = config.getSendRules();
		}
		else {
			rules = config.getReceiveRules();
		}
		
		if(rules == null) {
			return null;
		}
		
		for(Rule r : rules) {
			if(!r.getSrc().isEmpty()) {
				if(!message.getSrc().equals(r.getSrc())) {
					continue;
				}
			}
			
			if(!r.getDest().isEmpty()) {
				if(!message.getDest().equals(r.getDest())) {
					continue;
				}
			}
			
			if(!r.getKind().isEmpty()) {
				if(!message.getKind().equals(r.getKind())) {
					continue;
				}

			}
			
			if(r.getSeqNum() != -1) {
				if(message.getSeqNum() != r.getSeqNum()) {
					continue;
				}
			}
			
			if(!r.getDuplicate().isEmpty()) {
				if(!(message.isDuplicate() == true && r.getDuplicate().equals("true") || 
						message.isDuplicate() == false && r.getDuplicate().equals("false"))) {
					continue;
				}
			}
			
			return r;
		}
		return null;
	}
	
	private void parseConfig() throws FileNotFoundException {
	    InputStream input = new FileInputStream(new File(configFilename));
        Constructor constructor = new Constructor(Config.class);
	    Yaml yaml = new Yaml(constructor);
	    
	    /* SnakeYAML will parse and populate the Config object for us */
	    config = (Config) yaml.load(input);
	    
	}

	public void closeAllSockets() throws IOException {
		//  Auto-generated method stub
		hostListenSocket.close();
		
		/*Close all other sockets in the sockets map*/
		for (Map.Entry<SocketInfo, Socket> entry : sockets.entrySet()) {
		    entry.getValue().close();
		}
		for(Map.Entry<String, ObjectOutputStream> entry : outputStreamMap.entrySet()) {
			entry.getValue().close();
		}
	}
	
	@Override
	public String toString() {
		return "MessagePasser [configFilename=" + configFilename
				+ ", localName=" + localName + ", hostListenSocket=" + hostListenSocket
				+ ", hostSocketInfo=" + hostSocketInfo + ", config=" + config
				+ "]";
	}

}
