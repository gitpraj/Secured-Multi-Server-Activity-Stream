package activitystreamer.server;

import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.security.KeyStore;
import java.util.ArrayList;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;


import activitystreamer.util.Settings;

public class Control extends Thread {
	private static final Logger log = LogManager.getLogger();
	// Client connections for each server.
	private static ArrayList<Connection> connections;
	// Server outgoing connections for each server.
	private static ArrayList<Connection> servConnections;
	// Server incoming connections for each server.
	private static ArrayList<Connection> servIncomConnections;
	
	private SSLSocketFactory sslsocketfactory ;

	private static boolean term=false;
	private static Listener listener;
	
	protected static Control control = null;
	
	public static Control getInstance() {
		if(control==null){
			control=new Control();
		} 
		return control;
	}
	
	public Control() {
		
		
		try
		{
		TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        KeyStore keystore = KeyStore.getInstance(KeyStore.getDefaultType());
        InputStream keystoreStream = this.getClass().getClassLoader().getResourceAsStream("store/mykey"); // note, not getSYSTEMResourceAsStream
        keystore.load(keystoreStream, "123456".toCharArray());
        trustManagerFactory.init(keystore);
        TrustManager[] trustManagers = trustManagerFactory.getTrustManagers();
        SSLContext ctx = SSLContext.getInstance("SSL");
        //SSLContext.setDefault(ctx); 
        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(keystore, "123456".toCharArray());
        ctx.init(kmf.getKeyManagers(), trustManagers, null);
        SSLContext.setDefault(ctx); 
		}
		catch(Exception e)
		{
			log.info("closing connection allapinne");
		}
		
		// initialize the connections array
		connections = new ArrayList<Connection>();
		servConnections = new ArrayList<Connection>();
		servIncomConnections = new ArrayList<Connection>();
		// start a listener
		try {
			listener = new Listener();
		} catch (IOException e1) {
			log.fatal("failed to startup a listening thread: "+e1);
			System.exit(-1);
		}	
	}
	
	public void initiateConnection(){
		// make a connection to another server if remote hostname is supplied
		if(Settings.getRemoteHostname()!=null){
			try {
				log.debug("outgoing to another server");
				sslsocketfactory = (SSLSocketFactory) SSLSocketFactory.getDefault();
				//socket = (SSLSocket) sslsocketfactory.createSocket(Settings.getRemoteHostname(), Settings.getRemotePort());
				outgoingConnection((SSLSocket) sslsocketfactory.createSocket(Settings.getRemoteHostname(), Settings.getRemotePort()));
			} catch (IOException e) {
				log.error("failed to make connection to "+Settings.getRemoteHostname()+":"+Settings.getRemotePort()+" :"+e);
				System.exit(-1);
			}
		}
	}
	
	public void initiateConnectionNonSSl(){
		// make a connection to another server if remote hostname is supplied
		if(Settings.getRemoteHostname()!=null){
			try {
				log.debug("outgoing to another server");
				outgoingConnectionNonSSl(new Socket(Settings.getRemoteHostname(),Settings.getRemotePort()));
			} catch (IOException e) {
				log.error("failed to make connection to "+Settings.getRemoteHostname()+":"+Settings.getRemotePort()+" :"+e);
				System.exit(-1);
			}
		}

	}
	
	/*
	 * Processing incoming messages from the connection.
	 * Return true if the connection should close.
	 */
	public synchronized boolean process(Connection con,String msg){
		return false;
	}
	
	/*
	 * The connection has been closed by the other party.
	 */
	public synchronized void connectionClosed(Connection con){
		if(!term) connections.remove(con);
	}
	
	/*
	 * A new incoming connection has been established, and a reference is returned to it
	 */
	public synchronized Connection incomingConnection(SSLSocket s) throws IOException{
		log.debug("incomming connection: "+Settings.socketAddress(s));
		Connection c = new Connection(s);
		connections.add(c);
		return c;
		
	}
	
	/*public synchronized Connection incomingConnectionNonSSl(Socket s) throws IOException {
		// TODO Auto-generated method stub
		log.debug("incomming connection: "+Settings.socketAddress(s));
		Connection c = new Connection(s);
		connections.add(c);
		return c;
	}*/
	
	/*
	 * A new outgoing connection has been established, and a reference is returned to it
	 */
	public synchronized Connection outgoingConnection(SSLSocket s) throws IOException{
		log.debug("outgoing connection: "+Settings.socketAddress(s));
		Connection c = new Connection(s);
		servConnections.add(c);
		return c;
		
	}
	
	public synchronized Connection outgoingConnectionNonSSl(Socket s) throws IOException {
		// TODO Auto-generated method stub
		log.debug("outgoing connection to non-ssl: "+Settings.socketAddress(s));
		Connection c = new Connection(s);
		servConnections.add(c);
		return c;
	}
	
	@Override
	public void run(){
		log.info("using activity interval of "+Settings.getActivityInterval()+" milliseconds");
		while(!term){
			// do something with 5 second intervals in between
			try {
				Thread.sleep(Settings.getActivityInterval());
			} catch (InterruptedException e) {
				log.info("received an interrupt, system is shutting down");
				break;
			}
			if(!term){
				log.debug("doing activity");
				term=doActivity();
			}
			
		}
		log.info("closing "+connections.size()+" connections");
		// clean up
		for(Connection connection : connections){
			connection.closeCon();
		}
		listener.setTerm(true);
	}
	
	public boolean doActivity(){
		return false;
	}
	
	public final void setTerm(boolean t){
		term=t;
	}
	
	public final ArrayList<Connection> getConnections() {
		return connections;
	}
	
	public final ArrayList<Connection> getServConnections() {
		return servConnections;
	}

	public static ArrayList<Connection> getServIncomConnections() {
		return servIncomConnections;
	}

	public static void setServIncomConnections(ArrayList<Connection> servIncomConnections) {
		Control.servIncomConnections = servIncomConnections;
	}
	
	public static void addServIncomConnections(Connection c) {
		Control.servIncomConnections.add(c);
	}
	
	public static void removeServIncomConnections(Connection c) {
		Control.servIncomConnections.remove(c);
	}

	
	
}
