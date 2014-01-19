import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;


public class CmdTool {
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
            	break;
            }
            else if(cmdInput.equals("ps")) {
            	this.msgPasser.toString();// you have to overwrite toString yourself
            }
            else if (!cmdInput.equals(null) && !cmdInput.equals("\n")){
            	String[] array = cmdInput.split(" ");
            	if(array == null || array.length != 3) {
            		System.out.println("Invalid Command!");
            	}
            	else {
            		this.msgPasser.send(new Message(array[0], array[1], array[2]));
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
