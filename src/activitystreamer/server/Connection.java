package activitystreamer.server;


import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.security.KeyStore;
import java.util.HashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;


import activitystreamer.util.Settings;
import java.lang.reflect.*;


public class Connection extends Thread {
	private static final Logger log = LogManager.getLogger();
	private DataInputStream in;
	private DataOutputStream out;
	private BufferedReader inreader;
	private PrintWriter outwriter;
	private boolean open = false;
	private Socket socket;
	private boolean term=false;
	
	Connection(Socket socket) throws IOException{
			/*try
			{
				TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
				KeyStore keystore = KeyStore.getInstance(KeyStore.getDefaultType());
				InputStream keystoreStream = this.getClass().getClassLoader().getResourceAsStream("store/MyPrivateKey.store"); // note, not getSYSTEMResourceAsStream
				keystore.load(keystoreStream, "123456".toCharArray());
				trustManagerFactory.init(keystore);
				TrustManager[] trustManagers = trustManagerFactory.getTrustManagers();
				SSLContext ctx = SSLContext.getInstance("SSL");
				KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
				kmf.init(keystore, "123456".toCharArray());
				ctx.init(kmf.getKeyManagers(), trustManagers, null);
				SSLContext.setDefault(ctx); 
			}
			catch(Exception e)
			{
				log.info("closing connection allapinne");
			}*/
		
		in = new DataInputStream(socket.getInputStream());
	    out = new DataOutputStream(socket.getOutputStream());
	    inreader = new BufferedReader( new InputStreamReader(in));
	    outwriter = new PrintWriter(out, true);
	    this.socket = socket;
	    open = true;
	    start();
	}
	
	/*
	 * returns true if the message was written, otherwise false
	 */
	public boolean writeMsg(String msg) {
		if(open){
			outwriter.println(msg);
			outwriter.flush();
			return true;	
		}
		return false;
	}
	
	public void closeCon(){
		if(open){
			log.info("closing connection "+Settings.socketAddress(socket));
			try {
				term=true;
				//inreader.close();
				out.close();
				in.close();
			} catch (IOException e) {
				// already closed?
				log.error("received exception closing the connection "+Settings.socketAddress(socket)+": "+e);
			}
		}
	}
	
	
	public void run(){
		try {
			String data;
			while(!term && (data = inreader.readLine())!=null){
				//term=Control.getInstance().process(this,data);
				Control.getInstance().process(this,data);
			}
			log.debug("connection closed to "+Settings.socketAddress(socket));
			Control.getInstance().connectionClosed(this);
			in.close();
		} catch (IOException e) {
			log.error("connection "+Settings.socketAddress(socket)+" closed with exception: "+e);
			Control.getInstance().connectionClosed(this);
			
		} finally {
			// Remove from login hash table
			String userName;
			HashMap<Connection, String> usercon = null;
			HashMap<String, Integer> userlogin = null;
			usercon = ControlSolution.getUserCon();
			userlogin = ControlSolution.getUserLogin();
			
			userName = usercon.get(this);
			if (userName != null) {
				userlogin.remove(userName);
				usercon.remove(this);
			
				ControlSolution.setUserLogin(userlogin);
				ControlSolution.setUserCon(usercon);
			
				// reduce client load
				//ControlSolution.decrementLoad();
			}
			ControlSolution.decrementLoad();
		}
		open=false;
	}
	
	public Socket getSocket() {
		return socket;
	}
	
	public boolean isOpen() {
		return open;
	}
}
