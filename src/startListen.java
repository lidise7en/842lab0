import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Queue;

	public class startListen extends Thread {
		private Queue<Message> recvQueue;
		private SocketInfo hostSocketInfo;
		private ServerSocket hostListenSocket;
		private Config config;
		public startListen(Queue<Message> recvQueue, SocketInfo hostSocketInfo, ServerSocket hostListenSocket, Config config) {
			this.recvQueue = recvQueue;
			this.hostSocketInfo = hostSocketInfo;
			this.hostListenSocket = hostListenSocket;
			this.config = config;
		}
		public void run() {
			System.out.println("Running");
			try {
				hostListenSocket = new ServerSocket(hostSocketInfo.port);
				while(true) {
					Socket sock = hostListenSocket.accept();
if(sock.isClosed())
	System.out.println("sock is closed!!!");	
					new ListenThread(sock, this.recvQueue, this.config).start();		
				}
			}catch(IOException e) {
				Thread.currentThread().interrupt();
			}
		}
	}