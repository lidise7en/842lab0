import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.Socket;
import java.util.List;
import java.util.Queue;




public class ListenThread extends Thread {
		private Socket LisSock = null;
		private Queue<Message> recvQueue;
		private Config config;
		private enum RuleType {
			SEND,
			RECEIVE,
		}
		public ListenThread(Socket sock, Queue<Message> recvQueue, Config config) {
			this.LisSock = sock;
			this.recvQueue = recvQueue;
			this.config = config;
		}
		public void run() {

			try {
				ObjectInputStream in = new ObjectInputStream(this.LisSock.getInputStream());

				while(true) {
if(this.LisSock.isClosed())
		System.out.println("sock is closed!!!!");
				

					Message msg = (Message)in.readObject();

					if(msg.isDuplicate())
						continue;
					Rule rule = null;
					if((rule = matchRule(msg, RuleType.RECEIVE)) != null) {
						if(rule.getAction().equals("drop")) {
							continue;
						}
						else if(rule.getAction().equals("duplicate")) {
							System.out.println("Duplicating message");
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

				}
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			} catch (ClassNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
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
					if(message.getSrc().equals(r.getSrc())) {
						found = true;
					}
					else
						continue;
				}
				
				if(!r.getDest().isEmpty()) {
					if(message.getDest().equals(r.getDest())) {
						found = true;
					}
					else
						continue;
				}
				
				if(!r.getKind().isEmpty()) {
					if(message.getKind().equals(r.getKind())) {
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
		
	}