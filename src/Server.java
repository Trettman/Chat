import java.io.*;
import java.net.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

import org.json.JSONException;
import org.json.JSONObject;

public class Server extends JFrame{

	private JTextField userText;
	private JTextArea chatWindow;
	private ServerSocket serverSocket;
	private ArrayList<ObjectOutputStream> outputList;
	private String[] listOfUsers;
	private int maxAmountOfUsers;
	private JSONObject obj;
	private String jsonObjectString;
	private Connection con;
	//Replace the JTextArea with a JPanel with JLabels in it
	
	public Server(){
		
		super("Chat application server");
		
		maxAmountOfUsers = 100;
		outputList = new ArrayList<ObjectOutputStream>();
		listOfUsers = new String[maxAmountOfUsers];
		
		obj = new JSONObject();
		try{
			obj.put("file", "");
			obj.put("listOfUsernames", listOfUsers);
			obj.put("username", "");
			obj.put("disconnectedUser", "");
			jsonObjectString = obj.toString();
		}catch (JSONException e){
			e.printStackTrace();
		}
		
		userText = new JTextField();
		userText.setEditable(false); //You are not allowed to type anything before there's a connection
		userText.addActionListener( //When the user presses ENTER the action of sending a message will be performed
			new ActionListener(){
				public void actionPerformed(ActionEvent event){
					sendMessage(event.getActionCommand());
					userText.setText(""); //Clear the typing area after the user pressed ENTER
				}
				
			}
		);
		add(userText, BorderLayout.SOUTH);
		
		chatWindow = new JTextArea();
		add(new JScrollPane(chatWindow));
		setSize(640, 480);
		setVisible(true);
		
		//Disables the fading out of the disabled window
		chatWindow.addFocusListener(new FocusListener(){

	        public void focusLost(FocusEvent e){
	        	chatWindow.setEditable(true);
	        }

	        public void focusGained(FocusEvent e){
	        	chatWindow.setEditable(false);
	        }
	    });		
	}	
	
	//Set up and run the server. This will be called after the GUI is created
	public void run(){
		
		try{			
		    serverSocket = new ServerSocket(6789, maxAmountOfUsers);
			showMessage("Waiting for someone to connect... \n");
	   
			new Thread(new SocketThread(new Socket())).start();    
			
		}catch (IOException ioException){
		    ioException.printStackTrace();
		}		
	}	

	//Sends a message to the client
	private void sendMessage(String message){
		
		for(ObjectOutputStream output : outputList){
			
			try{
				obj.put("message", ": " + message);
				obj.put("username", "Server");
				jsonObjectString = obj.toString();
				output.writeObject(jsonObjectString);
				output.flush();
			}catch(IOException ioException){

			}catch(JSONException e){
				e.printStackTrace();
			}
		}

		showMessage("\nServer: " + message); //So a user can see the conversation history
	}
	
	private void sendMessageToClients(){
		
		for(ObjectOutputStream output : outputList){
			try{
				obj.put("message", obj.getString("message"));
				jsonObjectString = obj.toString();
				output.writeObject(jsonObjectString);
				output.flush();
			}catch(IOException ioException){

			}catch(JSONException e){
				e.printStackTrace();
			}
		}
	}
	
	//Displays all messages on the chat area and updates it
	private void showMessage(final String text){
		
		//Sets aside a thread that updates parts of the GUI, instead creating a whole new one
		SwingUtilities.invokeLater(
			new Runnable(){
				public void run(){
					chatWindow.append(text);
				}
			}
		);
	}
	
	private void sendChatLog(ObjectOutputStream output) throws SQLException, IOException, JSONException{
		
		getCon();
		
		Statement stat = null;		
		String query = "select * from messages";
		
		if(con != null){
			try{
		         stat = con.createStatement();		      
		    }catch(SQLException e){
		        System.out.println("Connection couldn't be obtained");
		    }
		}
		
		ResultSet rs = stat.executeQuery(query);		
		
		while(rs.next()){
			
			String username = rs.getString("username");
			String message = rs.getString("message");						
//			Time dateTime = rs.getTime("date");	I don't know if I'll use this or not
			if(!username.equals("null"))
				obj.put("message", username + ": " + message);
			else
				obj.put("message", message);
			jsonObjectString = obj.toString();
			output.writeObject(jsonObjectString);
			output.flush();
					
		}			
	}
	
	//Lets a user type
	private void ableToType(final boolean tof){
		
		SwingUtilities.invokeLater(
			new Runnable(){
				public void run(){
					userText.setEditable(tof);
				}
			}
		);
	}
	
	//Takes a string array and a value and checks if the array contains the said value
	private boolean doesArrayContain(String[] array, String value){
		
		for(int i = 0; i < array.length; i++)
			if(array[i] != null)
				if(array[i].equals(value))
					return true;
		
		return false;
	}
	
	//Pushes a value to the first empty spot in an array
	private void pushValueToArray(String[] array, String value){
		
		for(int i = 0; i < array.length; i++){
			if(array[i] == null){
				array[i] = value;
				break;
			}
		}
	}
	
	//Removes an element from an array
	private void removeElementFromArray(String[] array, String element){
		
		for(int i = 0; i < array.length; i++){
			if(array[i] != null){
				if(array[i].equals(element)){
					array[i] = null;
				}
			}
		}
	}
	
	/*
	 * ---------------------------------------------------------------------------------------------------------------
	 * This section is all about my database.
	 */
	public Connection getCon(){

		try {
			con = getConnection();
		}catch (SQLException e) {
			System.out.println("Connection couldn't be obtained");
	    }
	    	return con;
	}
	
	public static Connection getConnection() throws SQLException {

        String drivers = "com.mysql.jdbc.Driver";
        String url = "jdbc:mysql://localhost:3306/testdb";
        String username = "root";
        String password = ""; //There is no password

        System.setProperty(drivers,"");

        return DriverManager.getConnection(url,username,password);
    }
	
	public void addItems(String username, String message) {

        PreparedStatement pstat = null;

        String sql = "INSERT INTO messages(username, message, date) VALUES (?,?, NOW());";

        if(con != null){
            try{
                 pstat = con.prepareStatement(sql);
            }catch(SQLException e){
                System.out.println("Connection couldn't be obtained");
            }
        }

        if(pstat != null){
            try{
                 pstat.setString(1, username);
                 pstat.setString(2, message);
                 pstat.executeUpdate();
            }catch(SQLException e){
                e.printStackTrace();
                System.out.println("Insertion of the entry was unsuccessful");
            }
        }
    }
	
	/*
	 * -------------------------------------------------------------------------------------------------------------
	 * The code for my database ends here 
	 */
	
	public class SocketThread implements Runnable {

	    private Socket socket;
	    private ObjectInputStream input;
	    private ObjectOutputStream output;
	    private boolean executeOnce;
		private String username;

	    public SocketThread(Socket socket) {
	        this.socket = socket;
	    }

	    public void run(){
	    	
	    	try{	    		
	    		executeOnce = true;
	    		
				//Connect and have a conversation
	    		socket = serverSocket.accept(); //Accepts a connection to the socket
	    		
	    		output = new ObjectOutputStream(socket.getOutputStream());
	    		output.flush();
	    		input = new ObjectInputStream(socket.getInputStream());
	    		
	    		new Thread(new SocketThread(new Socket())).start();

	    		outputList.add(output);
	    		
	    		try {
					sendChatLog(output);
				} catch(SQLException | JSONException e1){
					e1.printStackTrace();
				}
	    		
	    		ableToType(true);
	    			    		
	    		do{	    			
	    			try{
	    				jsonObjectString = (String)input.readObject();
	    				obj = new JSONObject(jsonObjectString);
	    				
	    				if(!obj.getString("message").equals("")){
	    					showMessage("\n" + obj.getString("message"));
	    					sendMessageToClients();
	    					addItems(obj.getString("username"), obj.getString("message").substring(obj.getString("message").indexOf(" ") + 1)); 
	    					//I want to remove the ": " from the beginning of the string
	    				}
	    				
	    				//(Yes, it would be better using an ArrayList of strings, but JSONObjects are stupid...)
	    				//If the array of usernames do not contain the current username, then put it in the array
	    				if(!doesArrayContain(listOfUsers, obj.getString("username"))){
	    					pushValueToArray(listOfUsers, obj.getString("username"));
	    				}
	    				obj.put("listOfUsernames", listOfUsers);
	    				
	    				if(executeOnce){
	    					username = obj.getString("username"); //The username of the person that just connected
	    					obj.put("message", username + " connected to the server");
	    					addItems("null", obj.getString("message")); 
		    				showMessage("" + obj.getString("message"));
		    				sendMessageToClients();		    				
		    				executeOnce = false;
	    				}
	    			}catch(ClassNotFoundException classNotFoundException){
	    				showMessage("\nUnrecognized class");
	    			}catch(JSONException e){
	    				e.printStackTrace();
	    			}
	    		}while(true);

			}catch(EOFException eofException){ //The exception is End Of Stream Exception
				showMessage("\nServer ended the connection");
			} catch (IOException e) {
				e.printStackTrace();
			}finally{
				try{					
					showMessage("\n" + username + " disconnected from the server");
					obj.put("username", username);
					obj.put("message", username + " disconnected from the server");
					obj.put("disconnectedUser", username);
					addItems("null", obj.getString("message")); 
					sendMessageToClients();
					removeElementFromArray(listOfUsers, username); //When a user disconnects if removes their username from the array
					output.close();
					input.close();
					socket.close();
				}catch(IOException ioException){
					ioException.printStackTrace();
				}catch(JSONException e){
					e.printStackTrace();
				}		
			}	
	    }
	}
}
