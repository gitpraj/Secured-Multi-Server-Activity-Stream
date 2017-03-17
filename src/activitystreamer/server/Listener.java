package activitystreamer.server;

import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.KeyStore;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;


import activitystreamer.util.Settings;

public class Listener extends Thread{
	private static final Logger log = LogManager.getLogger();
	//private ServerSocket serverSocket_nonssl=null;
	private boolean term = false;
	private int portnum;
	private SSLServerSocketFactory sslserversocketfactory ;
	private SSLServerSocket serverSocket;
	
	public Listener() throws IOException{
		portnum = Settings.getLocalPort(); // keep our own copy in case it changes later
		//serverSocket = new ServerSocket(portnum);
		
			try
			{
				//System.setProperty("javax.net.debug","all");
				TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
				KeyStore keystore = KeyStore.getInstance(KeyStore.getDefaultType());
				InputStream keystoreStream = this.getClass().getClassLoader().getResourceAsStream("store/mykey"); // note, not getSYSTEMResourceAsStream
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
				log.info("received exception, shutting down"+ e);
			}
			log.debug("going in");
			sslserversocketfactory = (SSLServerSocketFactory) SSLServerSocketFactory
				.getDefault();
			serverSocket = (SSLServerSocket) sslserversocketfactory.createServerSocket(portnum);
		
		start();
	}
	
	@Override
	public void run() {
		log.info("listening for new connections on "+portnum);
		while(!term){
				SSLSocket clientSocket;
			try {
				clientSocket = (SSLSocket) serverSocket.accept();
				Control.getInstance().incomingConnection(clientSocket);
			} catch (IOException e) {
				log.info("received exception, shutting down");
				term=true;
			}
		}
	}

	public void setTerm(boolean term) {
		this.term = term;
		if(term) interrupt();
	}
	
	
}
