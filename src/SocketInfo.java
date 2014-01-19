
public class SocketInfo {
	
	private String ipAddr;
	private int port;
	
	public SocketInfo(String ip, int port) {
		this.ipAddr = ip;
		this.port = port;
	}
	public String getIpAddr() {
		return ipAddr;
	}
	public void setIpAddr(String ipAddr) {
		this.ipAddr = ipAddr;
	}
	public int getPort() {
		return port;
	}
	public void setPort(int port) {
		this.port = port;
	}
	
	@Override
	public String toString() {
		return "SocketInfo [ipAddr=" + ipAddr + ", port=" + port + "]";
	}
	
}
