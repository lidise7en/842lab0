import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.net.Socket;
import java.util.Map;
import java.util.Queue;

import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;


public class MessagePasser {
	
	private Queue<Message> sendQueue;
	private Queue<Message> recvQueue;
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
		}
		/* Set up socket */
		System.out.println("For this host: " + hostSocketInfo.toString());
	}
	
	void send(Message message) {
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
	}
	
	Message receive() {
		/* Re-parse the config.
		 * Receive the message using sockets.
		 * Finally, check message against receiveRules.
		 */
		
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
	
	// TODO - only for testing
	public static void main(String[] args)
	{
		MessagePasser testPasser = new MessagePasser("src/sample_config.yml", "charlie");
		testPasser.send(null);
		testPasser.receive();
	}
}
