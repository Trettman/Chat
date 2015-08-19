import javax.swing.JFrame;

public class ClientStart {

	public static void main(String[] args){
		
		Client client = new Client("127.0.0.1");
		
		client.run();	
	}
}
