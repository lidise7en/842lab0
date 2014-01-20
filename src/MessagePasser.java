import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;


public class MessagePasser {
	
	private Queue<Message> delaySendQueue = new LinkedList<Message>(); //store the delayed send msg
	private Queue<Message> delayRecvQueue = new LinkedList<Message>(); //store the delayed recv msg
	private Queue<Message> recvQueue = new LinkedList<Message>(); //store all the received msg from all receive sockets
	
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
	public class ListenThread implements Runnable {
		Socket sock;
		
		public ListenThread(Socket sock) {
			this.sock = sock;
		}
		public void run() {
			while(true) {
				ObjectInputStream in = null;
				try {
					in = new ObjectInputStream(sock.getInputStream());
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				Message msg = null;
				try {
					msg = (Message)in.readObject();
				} catch (ClassNotFoundException | IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				if(msg.isDuplicate())
					continue;
				Rule rule = null;
				if((rule = matchRule(msg, RuleType.RECEIVE)) != null) {
					if(rule.getAction().equals("drop")) {
						continue;
					}
					else if(rule.getAction().equals("duplicate")) {
						synchronized(recvQueue) {
							recvQueue.add(msg);
							recvQueue.add(msg.makeCopy());
						}
					}
					else if(rule.getAction().equals("delay")) {
						synchronized(recvQueue) {
							recvQueue.add(msg);
						}
					}
					else {
						System.out.println("We receive a wierd msg!");
					}
				}
				else {
					synchronized(recvQueue) {
						recvQueue.add(msg);
					}
				}
				try {
					in.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}
	
	public class startListen implements Runnable {
		
		public void run() {
			System.out.println("Running");
			try {
				hostListenSocket = new ServerSocket(hostSocketInfo.port);
				while(true) {
					Socket sock = hostListenSocket.accept();
					new Thread(new ListenThread(sock)).start();		
				}
			}catch(IOException e) {
				Thread.currentThread().interrupt();
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
			// TODO Auto-generated catch block
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
			/*start the listen thread */
			new Thread(new startListen()).start();

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
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		System.out.println("Send Rules --");
		for(Rule r : config.getSendRules()) {
			System.out.println(r.toString());
		}
		
		message.set_source(localName);
		message.set_seqNum(currSeqNum++);
				
		Rule rule = null;
		if((rule = matchRule(message, RuleType.SEND)) != null) {
			/* TODO - Fill this in */
			if(rule.getAction().equals("drop")) {
				return ;
			}
			else if(rule.getAction().equals("duplicate")) {
				Message dupMsg = message.makeCopy();
				dupMsg.set_duplicate(true);
				
				for(Message m : delaySendQueue) {
					doSend(m);
				}
				delaySendQueue.clear();
				
				/* Send 'message' and 'dupMsg' */
				doSend(message);
				doSend(dupMsg);
				
			}
			else if(rule.getAction().equals("delay")) {
				delaySendQueue.add(message);
			}
			else {
				System.out.println("We get a wierd message here!");
			}
		}
		else {
			for(Message m : delaySendQueue) {
				doSend(m);
			}
			delaySendQueue.clear();
			doSend(message);
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
				sendSock = new Socket(dest, config.getConfigSockInfo(dest).getPort());
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			sockets.put(config.getConfigSockInfo(dest), sendSock);
		}
		
		ObjectOutputStream out;
		try {
			out = new ObjectOutputStream(sendSock.getOutputStream());
			
			out.writeObject(message);
			out.flush();
			out.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public ArrayList<Message> receive() {
		/* Re-parse the config.
		 * Receive the message using sockets.
		 * Finally, check message against receiveRules.
		 */
		ArrayList<Message> result = new ArrayList<Message>();
		try {
			parseConfig();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		System.out.println("Receive Rules --");
		for(Rule r : config.getReceiveRules()) {
			System.out.println(r.toString());
		}
		/*
		 * TODO: You have to make some logic according to the type of msg
		 */
		synchronized(recvQueue) {
			if(!recvQueue.isEmpty()) {
				Message popMsg = recvQueue.remove();
				Rule rule = null;
				if((rule = matchRule(popMsg, RuleType.RECEIVE)) != null) {
					if(rule.getAction().equals("delay")) {
						delayRecvQueue.add(popMsg);
					}
				}
				else {
					while(delayRecvQueue.size() != 0) {
						result.add(delayRecvQueue.remove());
					}
					result.add(popMsg);
					
				}
			}
		}
		return result;
	}

	public Rule matchRule(Message message, RuleType type) {
		List<Rule> rules = null;
		boolean found = false;
		
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
			found = false;
			if(!r.getSrc().isEmpty()) {
				if(message.getSrc() == r.getSrc()) {
					found = true;
				}
				else
					continue;
			}
			
			if(!r.getDest().isEmpty()) {
				if(message.getDest() == r.getDest()) {
					found = true;
				}
				else
					continue;
			}
			
			if(!r.getKind().isEmpty()) {
				if(message.getKind() == r.getKind()) {
					found = true;
				}
				else
					continue;
			}
			
			if(r.getSeqNum() != -1) {
				if(message.getSeqNum() == r.getSeqNum()) {
					found = true;
				}
				else
					continue;
			}
			
			if(!r.getDuplicate().isEmpty()) {
				if(message.isDuplicate()) {
					found = true;
				}
				else
					continue;
			}
			
			if(found == true) {
				System.out.println("Rule matched - " + r.toString());
				return r;
			}
			else {
				return null;
			}
		}
		return null;
	}
	
	private void parseConfig() throws FileNotFoundException {
		//Need to add snakeYAML code for parsing the config.
		//Create a hostSocketInfo object.
	    InputStream input = new FileInputStream(new File(configFilename));
        Constructor constructor = new Constructor(Config.class);
	    Yaml yaml = new Yaml(constructor);
	    
	    /* SnakeYAML will parse and populate the Config object for us */
	    config = (Config) yaml.load(input);
	    
	}

	public void closeAllSockets() throws IOException {
		// TODO Auto-generated method stub
		hostListenSocket.close();
		
		/*Close all other sockets in the sockets map*/
		for (Map.Entry<SocketInfo, Socket> entry : sockets.entrySet()) {
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
	
	// TODO - only for testing
	public static void main(String[] args)
	{
		MessagePasser testPasser = new MessagePasser("src/sample_config.yml", "charlie");
		//testPasser.send(null);
		testPasser.receive();
	}

}
