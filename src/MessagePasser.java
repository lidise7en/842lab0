import java.net.Socket;
import java.util.Map;
import java.util.Queue;


public class MessagePasser {
	
	private Queue<Message> sendQueue;
	private Queue<Message> recvQueue;
	private Map<SocketInfo, Socket> sockets;
	private String configFilename;
	private String localName;
	private Socket hostSocket;
	private SocketInfo hostSocketInfo;
	
	private enum ruleType {
		SEND,
		RECEIVE,
	}
	
	public MessagePasser(String configuration_filename, String local_name)
	{
		configFilename = configuration_filename;
		localName = local_name;
		parseConfig();
		
		//Create hostSocket from hostSocketInfo
	}
	
	void send(Message message)
	{
		
	}
	
	Message receive()
	{
		return null;
	}
	
	private void parseConfig()
	{
		//Need to add snakeYAML code for parsing the config.
		//Create a hostSocketInfo object.
	}
	
	private void parseRules(Message msg, ruleType type)
	{
		//Check the rule type and parse that section against the Message msg.
	}
	
	//TODO - May need to add more funcs to break parsing functionality
}
