package hl.util.ftp;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.net.PrintCommandListener;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;
import org.apache.commons.net.ftp.FTPSClient;

public class VSFTPClient extends FTPClient {
	
	private FTPSClient ftpClient;
	
	public VSFTPClient() { 
		this.ftpClient	= new FTPSClient("TLS");
	}

	public VSFTPClient(String HOSTNAME, int PORT, String USERNAME, String PASSWORD) {
		super(HOSTNAME, PORT, USERNAME, PASSWORD);
		this.ftpClient	= new FTPSClient("TLS");
		this.ftpClient.addProtocolCommandListener(new PrintCommandListener(new PrintWriter(System.out)));
	}
	
	public boolean connect() {
		if(!ftpClient.isConnected()) {
			try {
				ftpClient.connect(HOSTNAME, PORT);
				if(!FTPReply.isPositiveCompletion(ftpClient.getReplyCode())) {
					ftpClient.disconnect();
					return false;
				}
				if(!ftpClient.login(USERNAME, PASSWORD)) {
					ftpClient.disconnect();
					return false;
				}
				ftpClient.setFileType(FTP.BINARY_FILE_TYPE);
				ftpClient.enterLocalPassiveMode();
				
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return true;
	}
	
	/**
	 * 
	 */
	public void disconnect() {
		if(ftpClient != null && ftpClient.isConnected())
			try {
				ftpClient.logout();
				ftpClient.disconnect();
			} catch (IOException e) {
				e.printStackTrace();
			}
	}

	@Override
	public List<FTPFile> listFiles(String dir) throws Exception {
		if(!checkIfDirExists(dir))
			throw new Exception("Directory not found -> " + dir);
		try {
			return Arrays.asList(ftpClient.listFiles(dir));
		} catch (IOException e) {
			e.printStackTrace();
		}
		return new ArrayList<>();
	}

	@Override
	public boolean checkIfDirExists(String dir) {
		try {
			String workingDirectory = ftpClient.printWorkingDirectory();
			ftpClient.changeWorkingDirectory(dir);
			if(ftpClient.getReplyCode() == 550)
				return false;
			ftpClient.changeWorkingDirectory(workingDirectory);
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}

	@Override
	public boolean removeDir(String dir, boolean ... toClose) {
		return false;
	}

	@Override
	public boolean moveFile(String filename, String srcDir, String destDir) {
		if(connect()) {
			try {
				return ftpClient.rename(getFilePath(filename, srcDir), getFilePath(filename, destDir));
			} catch (IOException e) {
				e.printStackTrace();
				return false;
			} finally {
				disconnect();
			}
		}
		return false;
	}

	@Override
	public String getWorkingDirectory() throws IOException {
		return ftpClient.printWorkingDirectory();
	}

	@Override
	protected boolean makeDirectory(String newDirectory) throws IOException {
		return ftpClient.makeDirectory(newDirectory);
	}

	@Override
	public boolean changeWorkingDirectory(String pathname) throws IOException {
		return ftpClient.changeWorkingDirectory(pathname);
	}

	@Override
	protected boolean storeFile(String filename, InputStream inputStream) throws IOException {
		return ftpClient.storeFile(filename, inputStream);
	}

	@Override
	protected boolean getFile(String filename, String remotePathname, String localPathname) {
		return false;
	}


}
