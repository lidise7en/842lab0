import java.net.Socket;
import java.util.Map;
import java.util.Queue;


public class MessagePasser {
	
	private Queue<Message> sendQueue;
	private Queue<Message> recvQueue;
	private Map<SocketInfo, Socket> sockets;
	String configFilename;
	String localName;
	
	private enum ruleType {
		SEND,
		RECEIVE,
	}
	
	public MessagePasser(String configuration_filename, String local_name)
	{
		
	}
	
	void send(Message message)
	{
		
	}
	
	Message receive()
	{
		return null;
	}
	
	void parseConfig()
	{
		
	}
	
	void parseRules(ruleType type)
	{
		
	}
	
}
