import java.util.List;


public class Config {
	List<SocketInfo> configuration;
	List<Rule> sendRules;
	List<Rule> receiveRules;
	
	public Config() {
		
	}
	
	public List<SocketInfo> getConfiguration() {
		return configuration;
	}
	public void setConfiguration(List<SocketInfo> hosts) {
		this.configuration = hosts;
	}
	public List<Rule> getSendRules() {
		return sendRules;
	}
	public void setSendRules(List<Rule> sendRules) {
		this.sendRules = sendRules;
	}
	public List<Rule> getReceiveRules() {
		return receiveRules;
	}
	public void setReceiveRules(List<Rule> receiveRules) {
		this.receiveRules = receiveRules;
	}
	
	public SocketInfo getConfigSockInfo(String name) {
		for(SocketInfo s : configuration) {
			if(s.getName().compareTo(name) == 0) {
				return s;
			}
		}
		return null;
	}
}
