package activitystreamer.server;

public class ServerList {
	private String serverID;
	private int port;
	private String host;
	private int clientLoad;
	
	public ServerList() {
		serverID = null;
		port = -1;
		host = null;
		clientLoad = -1;
	}
	
	public String getServerID() {
		return serverID;
	}
	public void setServerID(String serverID) {
		this.serverID = serverID;
	}
	public int getPort() {
		return port;
	}
	public void setPort(int port) {
		this.port = port;
	}
	public String getHost() {
		return host;
	}
	public void setHost(String host) {
		this.host = host;
	}
	public int getClientLoad() {
		return clientLoad;
	}
	public void setClientLoad(int clienLoad) {
		this.clientLoad = clienLoad;
	}	       

}
