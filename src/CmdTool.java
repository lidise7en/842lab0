import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;


public class CmdTool {
	/*
	 * Command Type:
	 * quit: quit the whole process
	 * ps: print the information of current MessagePasser
	 * send command: Kind dest data duplicate SeqNum
	 * receive command: receive
	 */
	private MessagePasser msgPasser;
	public CmdTool(MessagePasser msgPasser) {
		this.msgPasser = msgPasser;
	}
	public void executing() {
		String cmdInput = new String();
        BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
        while (!cmdInput.equals("quit")) {
            System.out.print("CommandLine% ");
            try {
                cmdInput = in.readLine();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            if(cmdInput.equals("quit")) {
            	System.exit(0);
            }
            else if(cmdInput.equals("ps")) {
            	this.msgPasser.toString();
            }
            else if (!cmdInput.equals(null) && !cmdInput.equals("\n")){
            	String[] array = cmdInput.split(" ");
            	if(array == null || array.length != 1 || array.length != 5) {
            		System.out.println("Invalid Command!");
            	}
            	else {
            		if(array.length == 5)
            			this.msgPasser.send(new Message(array[0], array[1], array[2]));
            		else if(array[0].equals("receive")) {
            			ArrayList<Message> msgList = this.msgPasser.receive();
            			if(msgList.size() == 0) {
            				System.out.println("Nothing to pass to Aplication!");
            			}
            			else {
            				System.out.println("We receive");
            				for(Message m : msgList) {
            					System.out.println(m.toString());
            				}
            			}
            		}
            		else {
            			System.out.println("Invalid Command!");
            		}
            	}
            }
        }
	}
	

	public static void main(String[] args) {
		MessagePasser msgPasser = new MessagePasser("", "");
		CmdTool tool = new CmdTool(msgPasser);
		tool.executing();
	}
}
