package hl.util.ftp;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;

public class FTPnClient extends FTPClient {
	
	private org.apache.commons.net.ftp.FTPClient ftpClient;
	
	public FTPnClient() {
		this.ftpClient	= new org.apache.commons.net.ftp.FTPClient();
	}

	public FTPnClient(String HOSTNAME, int PORT, String USERNAME, String PASSWORD) {
		super(HOSTNAME, PORT, USERNAME, PASSWORD);
		this.ftpClient	= new org.apache.commons.net.ftp.FTPClient();
	}

	/**
	 * 
	 * @return
	 */
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
				System.out.println("Connected");
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
		FileOutputStream out = null;		
		remotePathname = this.baseDir.concat(File.separatorChar + remotePathname);		
		try(InputStream in = ftpClient.retrieveFileStream(filename)) {
			ftpClient.completePendingCommand();
			byte[] buffer = new byte[in.available()];
			in.read(buffer);
			out = new FileOutputStream(localPathname + File.separatorChar + filename);
			out.write(buffer);
		} catch(Exception e) {
			e.printStackTrace();
			return false;
		} finally {
			if(out != null)
				try {
					out.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
		}
		return true;
	}


}
