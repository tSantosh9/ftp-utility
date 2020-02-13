package hl.util.ftp;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Properties;

import org.apache.log4j.Logger;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;

public class SFTPClient extends FTPClient {
	
	private static Logger logger = Logger.getLogger(SFTPClient.class);
	
	protected JSch jsch;
	protected Session session;
	protected Channel channel;
	protected ChannelSftp channelSftp;
	
	public SFTPClient() {
		
	}
	
	public SFTPClient(String HOSTNAME, int PORT, String USERNAME, String PASSWORD) {
		super(HOSTNAME, PORT, USERNAME, PASSWORD);
	}

	protected void init() throws JSchException {
		logger.info("init() Initializing the session");
		if(this.jsch == null) {
			this.jsch 			= new JSch();
			this.session 		= this.jsch.getSession(USERNAME, HOSTNAME, PORT);
			if(this.identityPath == null || this.identityPath.isEmpty())
				this.session.setPassword(PASSWORD);
			else
				this.jsch.addIdentity(this.identityPath);
			logger.info("init() Session initialized successfully.");
		} else
			logger.info("init() Session is already initialized.");
	}
	
	@Override
	public boolean connect() {
		logger.info("connect() Initiating the connection.");
		try {
			init();
			if(this.session != null && !this.session.isConnected()) {
				Properties config 	= new Properties();
		        config.put("StrictHostKeyChecking", "no");
		        session.setConfig(config);
		        session.connect();
				this.channel		= this.session.openChannel("sftp");
				this.channel.connect();
				this.channelSftp	= (ChannelSftp) this.channel;
				logger.info("connect() Connection established.");
			} else
				logger.info("connect() Connection is already established.");
		} catch (JSchException e) {
			logger.error("connect() Couldn't establish the connection, " + e.getMessage());
			e.printStackTrace();
			return false;
		}
		return true;
	}

	@Override
	public void disconnect() {
		logger.info("disconnect() Closing the connection.");
		if(this.channel != null) {
			this.channel.disconnect();
			logger.info("disconnect() Channel disconnected.");
		}
		if(this.session != null && this.session.isConnected()) {
			this.session.disconnect();
			logger.info("disconnect() Session disconnected, Connection closed.");
		}
		this.jsch = null;
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<Object> listFiles(String pathname) throws Exception {
		return channelSftp.ls(pathname);
	}

	@Override
	public boolean checkIfDirExists(String pathname) {
		try {
			channelSftp.lstat(pathname);
	    } catch (Exception e) {
	    	logger.info("checkIfDirExists() Directory: " + pathname + ", doesn't exist.");
	    	return false;
	    }
		logger.info("checkIfDirExists() Directory: " + pathname + " exist.");
		return true;
	}

	@Override
	public boolean makeDirectory(String newPathname) throws SftpException {
		logger.info("makeDirectory() Directory: " + newPathname + " creation started.");
		channelSftp.mkdir(newPathname);
		if(checkIfDirExists(newPathname)) {
			logger.info("makeDirectory() Directory: " + newPathname + " creation successfully.");
			return true;
		}
		logger.error("makeDirectory() Directory: " + newPathname + " creation failed.");
		return false;
	}

	@Override
	public String getWorkingDirectory() throws Exception {
		return channelSftp.pwd();
	}

	@Override
	public boolean changeWorkingDirectory(String pathname) throws Exception {
		channelSftp.cd(pathname);
		logger.info("changeWorkingDirectory() Changing the working directory to: " + pathname);
		return true;
	}

	@Override
	public boolean removeDir(String pathname, boolean ... toClose) throws Exception {
		logger.info("removeDir() Directory: " + pathname);
		if(connect()) {
			try {
				channelSftp.rmdir(pathname);
				if(!checkIfDirExists(pathname)) {
					logger.info("removeDir() Directory: " + pathname + " removed successfully.");
					return true;
				}
				logger.error("removeDir() Directory: " + pathname + " removed failed.");
				return false;
			} finally {
				if(toClose.length > 0 && toClose[0])
					disconnect();
			}
		}
		logger.error("removeDir() Directory: " + pathname + " removed failed.");
		return false;
	}

	@Override
	public boolean moveFile(String filename, String srcPathname, String destPathname) {
		logger.info("moveFile() Filename: " + filename + ", to be moved from: " + srcPathname + " to: " + destPathname);
		try {
			if(connect()) {
				if(!checkIfDirExists(destPathname)) {
					logger.info("moveFile() Directory: " + destPathname + ", doesn't exist, creating it.");
					createDirectory(destPathname);
				}
				try {
					channelSftp.rename(srcPathname + File.separatorChar + filename,
							destPathname + File.separatorChar + filename);
					logger.info("moveFile() File: " + filename + " moved from: " + srcPathname + " to: " + destPathname +
							" successfully.");
					return true;
				} catch (SftpException e) {
					e.printStackTrace();
					logger.error("moveFile() Couldn't move file: " + filename + ", got an exception: " + e.getMessage());
					return false;
				}
			}
		} finally {
			disconnect();
		}
		logger.error("moveFile() Couldn't move file: " + filename);
		return false;
	}

	@Override
	public boolean storeFile(String filename, InputStream inputStream) throws IOException, SftpException {
		logger.info("storeFile() File: " + filename + " upload initiated.");
		channelSftp.put(inputStream, filename);
		logger.info("storeFile() File: " + filename + " uploaded successfully.");
		return true;
	}

	@Override
	public boolean getFile(String filename, String remotePathname, String localPathname) throws SftpException {
		logger.info("getFile() File: " + filename + " download initiated.");
		channelSftp.get(remotePathname + File.separatorChar + filename, localPathname);
		logger.info("getFile() File: " + filename + " downloaded successfully.");
		return true;
	}

	
	public JSch getJsch() {
		return jsch;
	}

	public void setJsch(JSch jsch) {
		this.jsch = jsch;
	}

	public Session getSession() {
		return session;
	}

	public void setSession(Session session) {
		this.session = session;
	}

	public Channel getChannel() {
		return channel;
	}

	public void setChannel(Channel channel) {
		this.channel = channel;
	}

	public ChannelSftp getChannelSftp() {
		return channelSftp;
	}

	public void setChannelSftp(ChannelSftp channelSftp) {
		this.channelSftp = channelSftp;
	}
	
}
