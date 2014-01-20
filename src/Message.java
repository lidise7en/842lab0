import java.io.Serializable;


public class Message implements Serializable {

	private String src;
	private String dest;
	private String kind;
	private Object data;
	private boolean duplicate;
	private int seqNum;
	
	public Message(String dest, String kind, Object data) {
		this.dest = dest;
		this.kind = kind;
		this.data = data;
		this.duplicate = false;
	}

	// These settors are used by MessagePasser.send, not your app
	public void set_source(String source) {
		this.src = source;
	}
	public void set_seqNum(int sequenceNumber) {
		this.seqNum = sequenceNumber;
	}
	public void set_duplicate(Boolean dupe) {
		this.duplicate = dupe;
	}
	// other accessors, toString, etc as needed
	
	public String getDest() {
		return dest;
	}
	public String getKind() {
		return kind;
	}
	public Object getData() {
		return data;
	}
	public boolean isDuplicate() {
		return duplicate;
	}
	public int getSeqNum() {
		return seqNum;
	}
	public String getSrc() {
		return src;
	}
	@Override
	public String toString() {
		return "Message [dest=" + dest + ", kind=" + kind + ", data=" + data
				+ ", duplicate=" + duplicate + ", seqNum=" + seqNum + "]";
	}
	public Message makeCopy() {
		Message result = new Message(this.dest, this.kind, this.data);
		result.set_duplicate(this.duplicate);
		result.set_seqNum(this.seqNum);
		return result;
	}
}
