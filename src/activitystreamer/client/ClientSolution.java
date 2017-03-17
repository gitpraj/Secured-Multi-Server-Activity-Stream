package activitystreamer.client;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.KeyStore;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;

import com.google.gson.Gson;

import activitystreamer.server.ControlSolution;
import activitystreamer.util.Settings;

public class ClientSolution extends Thread {
	private static final Logger log = LogManager.getLogger();
	private static ClientSolution clientSolution;
	private TextFrame textFrame;
	private DataInputStream in;
	private DataOutputStream out;
	private BufferedReader inreader;
	private PrintWriter outwriter;
	private Socket socketNonSSl;
	private int port;
	private String host;
	private SSLSocketFactory sslsocketfactory ;
	private SSLSocket socket;
	/*
	 * additional variables
	 */
	
	// this is a singleton object
	public static ClientSolution getInstance(){
		if(clientSolution==null){
			clientSolution = new ClientSolution();
		}
		return clientSolution;
	}
	
	public ClientSolution(){
		/*
		 * some additional initialization
		 */
		try
		{
			TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
	        KeyStore keystore = KeyStore.getInstance(KeyStore.getDefaultType());
	        InputStream keystoreStream = this.getClass().getClassLoader().getResourceAsStream("store/mykey"); // note, not getSYSTEMResourceAsStream
	        keystore.load(keystoreStream, "123456".toCharArray());
	        trustManagerFactory.init(keystore);
	        TrustManager[] trustManagers = trustManagerFactory.getTrustManagers();
	        SSLContext ctx = SSLContext.getInstance("SSL");
	        ctx.init(null, trustManagers, null);
	        SSLContext.setDefault(ctx); 
        port=Settings.getRemotePort();
        log.info(port);
        host = Settings.getRemoteHostname();
		//socket = new Socket(host,port);
        
        if (Settings.getNonSecure() == 0) {
        	sslsocketfactory = (SSLSocketFactory) SSLSocketFactory.getDefault();
        	socket = (SSLSocket) sslsocketfactory.createSocket(host, port);
        	in=new DataInputStream(socket.getInputStream());
        } else {
        	socketNonSSl = new Socket(host,port);
        	in=new DataInputStream(socketNonSSl.getInputStream());
        }
        
        
		inreader = new BufferedReader( new InputStreamReader(in));
		//Settings.setServerFlag(false);
		}
		catch(Exception ec) {
			log.info("Error connectiong to server:" + ec);
			System.exit(0);
		}
		log.info("connected");
		log.debug("opening the gui");
		textFrame = null;
		// start the client's thread
		sendMessage();
		start();
	}
	
	// This constructor is used only for redirection
	public ClientSolution(int port, String host){
		/*
		 * some additional initialization
		 */
		try
		{
		TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        KeyStore keystore = KeyStore.getInstance(KeyStore.getDefaultType());
        InputStream keystoreStream = this.getClass().getClassLoader().getResourceAsStream("store/JohnsPrivateKey.store"); // note, not getSYSTEMResourceAsStream
        keystore.load(keystoreStream, "123456".toCharArray());
        trustManagerFactory.init(keystore);
        TrustManager[] trustManagers = trustManagerFactory.getTrustManagers();
        SSLContext ctx = SSLContext.getInstance("SSL");
        //SSLContext.setDefault(ctx); 
        ctx.init(null, trustManagers, null);
        SSLContext.setDefault(ctx); 
		}
		catch(Exception e)
		{
			log.info("closing connection allapinne");
		}
		try
		{
        //port=Settings.getRemotePort();
        log.info(port);
		//socket = new Socket(host, port);
        
        if (Settings.getNonSecure() == 0) {
        	sslsocketfactory = (SSLSocketFactory) SSLSocketFactory.getDefault();
        	socket = (SSLSocket) sslsocketfactory.createSocket(host, port);
        	in=new DataInputStream(socket.getInputStream());
        } else {
        	socketNonSSl = new Socket(host,port);
        	in=new DataInputStream(socketNonSSl.getInputStream());
        }
        
		inreader = new BufferedReader( new InputStreamReader(in));
		//Settings.setServerFlag(false);
		}
		catch(Exception ec) {
			log.info("Error connectiong to server:" + ec);
			//return false;
		}
		log.info("connected");
		log.debug("opening the gui");
		textFrame = null;
		// start the client's thread
		sendMessageAfterRedirect();
		start();
	}
	
	// called by the gui when the user clicks "send"
	@SuppressWarnings("unchecked")
	public void sendActivityObject(JSONObject activityObj){
		try {
			JSONObject obj = new JSONObject();
			obj.put("command", "ACTIVITY_MESSAGE");
			obj.put("username", Settings.getUsername());
			obj.put("secret", Settings.getSecret());
			obj.put("activity", activityObj);
			
			/*String userName = (String) activityObj.get("username");
			if (userName.compareTo(Settings.getUsername()) != 0) {
				// Logged in username and the username given in the activity message
				// is not the same.
				System.out.println("AUTHENTICATION_FAIL");
				System.out.println("Logged in username and given username not similar.");
				in.close();
				out.close();
				ControlSolution.decrementLoad();
				textFrame.setVisible(false);
				System.exit(0);
			} else {*/
				sendToServer(obj);
			//}
		}
		catch(Exception ec) {
			log.info("Error " + ec);
		}

	}
	
	// called by the gui when the user clicks disconnect
	public void disconnect(){
		try {
			JSONObject obj = new JSONObject();
			obj = createLogout();
			Gson gs=new Gson();
			String data=gs.toJson(obj);
			//System.out.println(data);
			if (Settings.getNonSecure() == 0) {
				out  = new DataOutputStream(socket.getOutputStream());
			} else {
				out  = new DataOutputStream(socketNonSSl.getOutputStream());
			}
			//out.flush();
			outwriter = new PrintWriter(out, true);
			outwriter.println(data);
			outwriter.flush();
			//out.writeUTF(data+"\n");
		}
		catch(Exception ec) {
			log.info("Error " + ec);
		}
		textFrame.setVisible(false);
		// close connection
		try {
			in.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		try {
			out.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.exit(0);
		/*
		 * other things to do
		 */
	}
	

	// the client's run method, to receive messages
	@Override
	public void run(){
		String fromServer;
		try {
			while((fromServer=inreader.readLine())!=null)
			{
				//log.debug(fromServer);
				processMsg(fromServer);
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			//e.printStackTrace();
			try {
				in.close();
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				//e1.printStackTrace();
				log.error("Closing connection failed."+e);
			}
			
		}
	}
	/*
	 * additional methods
	 */
	public void processMsg(String msg) {
		String msg1 = msg.trim().replaceAll("\r","").replaceAll("\n","").replaceAll("\t", "");
		//String msg1 = msg.replaceAll("[^a-zA-Z0-9]","");
		//log.debug(msg1);
		JSONObject obj1;
		Gson gs=new Gson();
		
		JSONParser parser = new JSONParser();
		//parser.
		//JSONObject activityObj = gs.fromJson(msg, JSONObject.class);
		Object obj = null;
		try {
			obj = parser.parse(msg1);
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		JSONObject jsonObject = (JSONObject) obj;
		String command = (String) jsonObject.get("command");
		
		if ((command != null) && (command.length() > 0)) {
			//System.out.println(command);
			String info = (String) jsonObject.get("info");
			if ((info != null) && (info.length() > 0)) {
				System.out.println(info);
			}
			if (command.compareTo("REGISTER_SUCCESS") == 0) {
				System.out.println("Secret is: " + Settings.getSecret());
				//System.out.println(command);
				obj1 = createLogin();
				sendToServer(obj1);
				return;
			} else if (command.compareTo("REDIRECT") == 0) {
				long port = (long) jsonObject.get("port");
				String host = (String) jsonObject.get("hostname");
				redirectToServer((int)port, host);
				
			} else if (command.compareTo("ACTIVITY_BROADCAST") == 0) {
				activity_display(jsonObject);
			} else if (command.compareTo("LOGIN_SUCCESS") == 0) {
				//System.out.println(command);
				textFrame = new TextFrame();
			} else if (command.compareTo("REGISTER_FAILED") == 0) {
				//textFrame.setVisible(false);
				System.exit(0);
			}
		}
	}
	
	@SuppressWarnings("unchecked")
	public void sendMessage() {
		try {
			int redirect = 0;
			JSONObject obj = null;
				if (Settings.getUsername().compareTo("anonymous") != 0) {
					if ((Settings.getUsername().length() != 0) && (Settings.getSecret() == null)) {
						obj = createRegister(redirect);
					} else if ((Settings.getUsername().length() != 0) && (Settings.getSecret().length() != 0)){
						obj = createLogin();
					} 
				} else {
					System.out.println("Anonymous user");
					if ((Settings.getUsername().length() != 0) && (Settings.getSecret() == null)) {
						// Anonymous user trying to login
						
						obj = createLogin();
					}
				}
				sendToServer(obj);
		}
		catch(Exception ec) {
			log.info("Error " + ec);
		}
	}
	
	public void sendMessageAfterRedirect() {
		try {
			JSONObject obj = null;
			// Register at new server
			if ((Settings.getUsername().length() != 0)) {
				obj = createRegister(1);
			} else if ((Settings.getUsername().length() != 0) && (Settings.getSecret().length() != 0)){
				obj = createLogin();
			}
			sendToServer(obj);
		}
		catch(Exception ec) {
			log.info("Error " + ec);
		}
	}	
	
	@SuppressWarnings("unchecked")
	public void activity_display(JSONObject jsonObject) {
		String command = (String) jsonObject.get("ACTIVITY_BROADCAST");
		String userName = (String) jsonObject.get("authenticated_user");
		//String activity = (String) jsonObject.get("activity");
		JSONObject activity = (JSONObject) jsonObject.get("activity");
		
		JSONObject obj = new JSONObject();
		//obj.put("authenticated_user", userName);
		//obj.put("activity", activity);
		
		textFrame.setOutputText(activity);
	}
	
	public void redirectToServer(int port, String host) {
		// Send it to the specified server.
		// first disconnect. close it
		try {
			in.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		// then create new socket
		clientSolution = new ClientSolution(port, host);
		
	}
	
	@SuppressWarnings("unchecked")
	public JSONObject createRegister(int redirect) {
		JSONObject obj = new JSONObject();
		String secret;
		if (redirect == 0) {
			secret = Settings.nextSecret();
		} else {
			secret = Settings.getSecret();
		}
		//String secret;
		Settings.setSecret(secret);
		obj.put("command", "REGISTER");
		obj.put("username", Settings.getUsername());
		obj.put("secret", secret);
		return obj;
	}
	
	@SuppressWarnings("unchecked")
	public JSONObject createLogin() {
		JSONObject obj = new JSONObject();
		obj.put("command", "LOGIN");
		obj.put("username", Settings.getUsername());
		obj.put("secret", Settings.getSecret());
		return obj;
	}
	
	@SuppressWarnings("unchecked")
	public JSONObject createLogout() {
		JSONObject obj = new JSONObject();
		obj.put("command", "LOGOUT");
		return obj;
	}
	
	public void sendToServer(JSONObject obj) {
		Gson gs=new Gson();
		String data=gs.toJson(obj);
			//	System.out.println(data);
		try {
			if (Settings.getNonSecure() == 0) {
				out  = new DataOutputStream(socket.getOutputStream());
			} else {
				out  = new DataOutputStream(socketNonSSl.getOutputStream());
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		//out.flush();
		outwriter = new PrintWriter(out, true);
		outwriter.println(data);
		outwriter.flush();
		//out.writeUTF(data+"\n");
	}
}
