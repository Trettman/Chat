import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.HeadlessException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.Socket;

import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.text.BadLocationException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.apache.commons.codec.binary.Base64;

public class Client extends JFrame {

	private JTextField userText;
	private JTextArea chatWindow;
	private JTextArea usersOnlineWindow;
	private JPanel panel;
	private JTabbedPane tabbedPane;
	private JMenuBar mb;
	private JMenu connectionBar;
	private JMenu optionBar;
	private JMenuItem exit;
	private JMenuItem sendImage;
	private ObjectOutputStream output;
	private ObjectInputStream input;
	private Socket connection;	
	private JSONObject obj;
	private String jsonObjectString;
	private String serverIP;
	private String username;
	private String[] arrayOfAddedUsernames;
	private boolean downloadFile; // If a client sent a file the said client won't download it
	private File selectedFile;

	/** 
	 * Constructor
	 * @param String host. The ip address of the server
	 *
	 */
	public Client(String host){
		
		super("Chat application client");

		arrayOfAddedUsernames = new String[100]; //It's 100 because only 100 people can connect to the server
		
		// Create JSONObject
		obj = new JSONObject();
		
		try {
			obj.put("file", "");
			obj.put("disconnectedUser", "");
		} catch (JSONException e) {
			e.printStackTrace();
		}
		
		downloadFile = true;
		
		// Create JPanel
		panel = new JPanel();
		/* Set layout to GridBagLayout. I use a GridBagLayout because it's one of the most
		 * flexible layout managers in Java.
		 */
		panel.setLayout(new GridBagLayout());
		
		GridBagConstraints c = new GridBagConstraints();

		// Sets the menu bar
		setMenuBar();
		
		// Create JTabbedPane
		tabbedPane = new JTabbedPane();

		// Create text area which shows the users that are currently online
		usersOnlineWindow = new JTextArea(1, 10);
		c.fill = GridBagConstraints.BOTH;
		c.gridwidth = 1;
		c.gridheight = 10;
		c.weightx = 0.30;
		c.weighty = 1;
		c.gridx = 0;
		c.gridy = 0;
		panel.add(new JScrollPane(usersOnlineWindow), c);

		// Create the text area that the chat conversations appear in
		chatWindow = new JTextArea(1, 8000);
		chatWindow.setLineWrap(true);
		c.fill = GridBagConstraints.BOTH;
		c.gridwidth = 2;
		c.gridheight = 9;
		c.weightx = 0.70;
		c.weighty = 0.99;
		c.gridx = 4;
		c.gridy = 0;
		panel.add(new JScrollPane(chatWindow), c);		
		
		// Create the text field that the user types in
		userText = new JTextField();
		c.fill = GridBagConstraints.BOTH;
		c.gridwidth = 2;
		c.gridheight = 1;
		c.weightx = 0.70;
		c.weighty = 0.01;
		c.gridx = 4;
		c.gridy = 9;
		userText.setEditable(false);
		userText.addActionListener(
			new ActionListener(){
				public void actionPerformed(ActionEvent event){
					if(!event.getActionCommand().equals("")){
						sendMessage(event.getActionCommand());
					}
					userText.setText("");
				}	
			}
		);
		panel.add(userText, c);		

		chatWindow.setEditable(false);		
		usersOnlineWindow.setEditable(false);
		
		tabbedPane.add("Public room", panel);

		// Add the panel to the JFrame
		add(panel);
		
		// Set some JFrame attributes
		setSize(640, 480);
		setVisible(true);	
		setResizable(false);
		setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		addWindowListener(new WindowAdapter(){
			public void windowClosing(WindowEvent event){
				try {
					removeUser();
				} catch (JSONException | IOException e) {
					e.printStackTrace();
				}
				dispose();
				System.exit(0);
			}
		});
		
		serverIP = host;	
	}
	
	// Connect to the server
	public void run(){
		
		try{
			connectToServer();
			setupStreams();
			whileChatting();
		} catch(EOFException eofException){
			showMessage("\nClient terminated connection");
		} catch(IOException ioException){
			ioException.printStackTrace();
			showMessage("\nFailed to connect to server");
		}
	}
	
	// Creates and sets the menu bars
	private void setMenuBar(){
		
		mb = new JMenuBar();
		add(mb, BorderLayout.NORTH);
		
		connectionBar = new JMenu("Chat");
		mb.add(connectionBar);

		sendImage = new JMenuItem("Send file");
		
		// Converts the file to base64 and puts the string in the JSONObject
		sendImage.addActionListener(
			new ActionListener(){
				public void actionPerformed(ActionEvent event){
					
					JFileChooser fileChooser = new JFileChooser();
					
			        int returnValue = fileChooser.showOpenDialog(null);
			        
			        if(returnValue == JFileChooser.APPROVE_OPTION){			        	
			          selectedFile = fileChooser.getSelectedFile();
			        }
			        
			        try {           
			            //Reads a Image file from file system
			            FileInputStream imageInFile = new FileInputStream(selectedFile);
			            
			            byte imageData[] = new byte[(int)selectedFile.length()];
			            
			            imageInFile.read(imageData);
			 
			            //Converts image byte array into base64 string
			            String imageDataString = encodeImage(imageData);
			            
			            obj.put("file", imageDataString);
			            obj.put("fileName", selectedFile.getName());

			            imageInFile.close();
	
			        } catch (FileNotFoundException e){
			            System.out.println("File not found" + e);
			        } catch (IOException ioe){
			            System.out.println("Exception while reading the file " + ioe);
			        } catch (JSONException e){
						e.printStackTrace();
					}
					try{
						obj.put("message", " sent the file: " + obj.getString("fileName"));
						obj.put("sendingUser", username);
						output.writeObject(obj.toString());
						output.flush();
					} catch(IOException | JSONException ioException){
						//I don't feel like doing anything here...
					}
					
					downloadFile = false;
				}
			}
		);	
		connectionBar.add(sendImage);
		
		exit = new JMenuItem("Exit");
		exit.addActionListener(
			new ActionListener(){
				public void actionPerformed(ActionEvent event){
					dispose();												
				}
			}
		);
		connectionBar.add(exit);
		
		optionBar = new JMenu("Options");
		mb.add(optionBar);
		
	}
	
	//Connect to server
	private void connectToServer() throws IOException{
		
		showMessage("Attempting connection...\n");
		
		try {
			do{
				try{
					username = JOptionPane.showInputDialog("Enter a username");
					obj.put("username", username);
				} catch (JSONException e) {
					e.printStackTrace();
				}
			} while(obj.getString("username").contains(" ") || obj.getString("username") == null);
		} catch (HeadlessException | JSONException e) {
			e.printStackTrace();
		}
		
		connection = new Socket(InetAddress.getByName(serverIP), 6789); //The IP address of the server and the port
		showMessage("Connected to: " + connection.getInetAddress().getHostName());
	}
	
	// Set up streams to send and receive messages
	private void setupStreams() throws IOException{
		
		output = new ObjectOutputStream(connection.getOutputStream());
		output.flush();
		input = new ObjectInputStream(connection.getInputStream());
		showMessage("\nStreams are setup \n");
		usersOnlineWindow.append("Users currently online:\n");
	}
	
	// While chatting with the server
	public void whileChatting() throws IOException{
		
		try{
			obj.put("message", "");
			jsonObjectString = obj.toString();
			output.writeObject(jsonObjectString);
			output.flush();
		} catch(IOException | JSONException e){
			e.printStackTrace();
		}

		ableToType(true);
		
		do{
			try{				
				obj = new JSONObject((String)input.readObject());
				
				/*
				 * If the "file" directory in the JSONObject is not empty it means someone just sent a file, so then
				 * we'll go ahead and ask the user if they want to download the file. 
				 */
				if(!obj.getString("file").equals("") && downloadFile){
					int result = JOptionPane.showConfirmDialog(null, obj.getString("sendingUser") 
								+ " wants to send you the file: " + obj.getString("fileName") 
								+ ".\nAccept?", null, JOptionPane.YES_NO_OPTION);
					if(result == JOptionPane.YES_OPTION) {
						saveFile();
					} 
				}
				
				downloadFile = true;
				
				showUsersOnline();
				showMessage("\n" + obj.get("message"));
			} catch(ClassNotFoundException classNotFoundException){
				showMessage("\n Unrecognized class");
			} catch(JSONException e){
				e.printStackTrace();
			}
		} while(true);
	}
	
	// Send messages to the server
	private void sendMessage(String message){
		
		try {
			obj.put("message", username + ": " + message);
			obj.put("username", username);
		} catch (JSONException e) {
			e.printStackTrace();
		}
		
		try{
			output.writeObject(obj.toString());
			output.flush();
		} catch(IOException ioException){
			chatWindow.append("\nSomething went wrong when sending message");
		}
	}
	
	// Displays all messages on the chat area and updates it
	private void showMessage(final String message){
		
		SwingUtilities.invokeLater(
			new Runnable(){
				public void run(){
					chatWindow.append(message);
				}
			}
		);
	}

	// Lets a user type or not
	private void ableToType(final boolean tof){
		
		SwingUtilities.invokeLater(
			new Runnable(){
				public void run(){
					userText.setEditable(tof);
				}
			}
		);
	}
	
	// Displays the users currently online in the usersOnlineWindow JTextArea
	private void showUsersOnline() throws JSONException{
		
		// Fuck det här under. Det fucking sög att skriva. JSONArrays är retarderade.
		
		for(int i = 0; i < ((JSONArray)obj.get("listOfUsernames")).length(); i++){
			if(!((JSONArray)obj.get("listOfUsernames")).isNull(i)){				
				if(!doesArrayContain(arrayOfAddedUsernames, ((JSONArray)obj.get("listOfUsernames")).getString(i))){
					usersOnlineWindow.append("\n" + ((JSONArray)obj.get("listOfUsernames")).getString(i));
					pushValueToArray(arrayOfAddedUsernames, ((JSONArray)obj.get("listOfUsernames")).getString(i));
				}
			}
		}

		if(!obj.getString("disconnectedUser").equals("")){
			int offset = usersOnlineWindow.getText().indexOf(obj.getString("disconnectedUser"));
			
			if(offset != -1){
				try {
					int line = usersOnlineWindow.getLineOfOffset(offset);
					int start = usersOnlineWindow.getLineStartOffset(line);
					int end = usersOnlineWindow.getLineEndOffset(line);
					
					usersOnlineWindow.replaceRange("", start, end);
				} catch (BadLocationException e) {
					e.printStackTrace();
				}				
			}
			obj.put("disconnectedUser", "");
		}
	}	
	
	private void removeUser() throws JSONException, IOException{

		for(int i = 0; i < ((JSONArray)obj.get("listOfUsernames")).length(); i++){
			if(!((JSONArray)obj.get("listOfUsernames")).isNull(i)){		
				if(((JSONArray)obj.get("listOfUsernames")).getString(i).equals(username)){
					((JSONArray)obj.get("listOfUsernames")).remove(i);
				}
			}
		}
	}
	
	//Takes a string array and a value and checks if the array contains the said value
	private boolean doesArrayContain(String[] array, String value){
		
		for(int i = 0; i < array.length; i++){
			if(array[i] != null){
				if(array[i].equals(value)){
					return true;
				}
			}
		}
		
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
	
	private void saveFile(){
		
		try {
			//Converts a base64 string into an image byte array
			byte[] imageByteArray = decodeImage(obj.getString("file"));
						             
			//Writes an image byte array into the file system
			FileOutputStream fileOutFile = new FileOutputStream("downloads/" + obj.getString("fileName"));
			fileOutFile.write(imageByteArray);
						            
			fileOutFile.close();
		            
			obj.put("file", "");
		} catch (JSONException e) {
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	//Encodes the byte array into a base64 string
    private String encodeImage(byte[] imageByteArray){
        return Base64.encodeBase64URLSafeString(imageByteArray);
    }
     
    //Decodes the base64 string into a byte array
    private byte[] decodeImage(String imageDataString){
        return Base64.decodeBase64(imageDataString);
    }
}
