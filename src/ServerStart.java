import javax.swing.JFrame;

public class ServerStart {

	public static void main(String[] args){
		
		Server server = new Server();
		
		server.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		server.run();		
	}	
}
