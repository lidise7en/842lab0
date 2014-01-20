import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Map;
import java.util.Queue;

import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;


public class MessagePasser {
	
	private Queue<Message> delaySendQueue;//store the delayed send msg
	private Queue<Message> delayRecvQueue;//store the delayed recv msg
	private Queue<Message> recvQueue;//store all the received msg from all receive sockets
	private Map<SocketInfo, Socket> sockets;
	private String configFilename;
	private String localName;
	private Socket hostSocket;
	private SocketInfo hostSocketInfo;
	private Config config;
	
	private enum ruleType {
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
				if(checkRecvRule(msg)) {
					if(msg.getKind().equals("drop")) {
						continue;
					}
					else if(msg.getKind().equals("duplicate")) {
						synchronized(recvQueue) {
							recvQueue.add(msg);
							recvQueue.add(msg.makeCopy());
						}
					}
					else if(msg.getKind().equals("delay")) {
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
	
	public MessagePasser(String configuration_filename, String local_name) {
		configFilename = configuration_filename;
		localName = local_name;
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
			try {
				startListen();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
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
		
		if(checkSendRule(message)) {
			if(message.getKind().equals("drop")) {
				
			}
			else if(message.getKind().equals("duplicate")) {
				
			}
			else if(message.getKind().equals("delay")) {
				
			}
			else {
				System.out.println("We get a wierd message here!");
			}
		}
		else {
			
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
				if(popMsg.getKind().equals("delay")) {
					delayRecvQueue.add(popMsg);
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
	
	public void startListen() throws IOException {
		ServerSocket ListenSocket = new ServerSocket(this.hostSocketInfo.port);
		while(true) {
			Socket sock = ListenSocket.accept();
			new Thread(new ListenThread(sock)).start();
			
		}
	}
	public boolean checkSendRule(Message messsage) {
		//TODO
		/*
		 * check whether this message matches the send rules
		 */
		return false;
	}
	
	public boolean checkRecvRule(Message message) {
		/*
		 * check whether this message matches the receive rules
		 */
		return false;
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
	
	// TODO - only for testing
	public static void main(String[] args)
	{
		MessagePasser testPasser = new MessagePasser("src/sample_config.yml", "charlie");
		testPasser.send(null);
		testPasser.receive();
	}
}
