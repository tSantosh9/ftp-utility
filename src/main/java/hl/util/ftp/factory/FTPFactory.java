package hl.util.ftp.factory;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import hl.util.ftp.AWS_SFTPClient;
import hl.util.ftp.FTPClient;
import hl.util.ftp.FTPnClient;
import hl.util.ftp.SFTPClient;
import hl.util.ftp.VSFTPClient;

public class FTPFactory {

	private static Properties hostDetailProperties;
	
	/**
	 * Based on the client type, it returns the respective FTPClient object
	 * FTP, SFTP, FTPS(for vsftp)
	 * 
	 * @param clientType
	 * @return FTPClient
	 * @throws IOException
	 */
	public static FTPClient getFTPClient(FTPClientType clientType) throws IOException {
		FTPClient ftpClient = null;
		
		if(clientType.equals(FTPClientType.FTPS))
			ftpClient = new VSFTPClient();
		else if(clientType.equals(FTPClientType.FTP))
			ftpClient = new FTPnClient();
		else if(clientType.equals(FTPClientType.SFTP))
			ftpClient = new SFTPClient();
		else if(clientType.equals(FTPClientType.AWS_SFTP))
			ftpClient = new AWS_SFTPClient();
		
		System.out.println("ftpClient: "+ftpClient);
		ftpClient.setHOSTNAME(getProperties("ftphostdetails.properties").getProperty("hostname"));
		ftpClient.setPORT(Integer.parseInt(getProperties("ftphostdetails.properties").getProperty("port")));
		ftpClient.setPASSWORD(getProperties("ftphostdetails.properties").getProperty("password"));
		ftpClient.setUSERNAME(getProperties("ftphostdetails.properties").getProperty("username"));
		ftpClient.setBaseDir(getProperties("ftphostdetails.properties").getProperty("baseDir"));
		ftpClient.setToProcessedDir(getProperties("ftphostdetails.properties").getProperty("move.processed.dir"));
		ftpClient.setIdentityPath(getProperties("ftphostdetails.properties").getProperty("identity_path"));
		return ftpClient;
	}
	
	public static Properties getProperties(String filename) throws IOException {
		if(hostDetailProperties == null) {
			synchronized (FTPFactory.class) {
				if(hostDetailProperties == null) {
					InputStream inputStream = null;
					try {
						try {
							inputStream = new FileInputStream("./resource/" + filename);
						} catch (Exception e) {
							inputStream	= FTPFactory.class.getClassLoader()
									.getResourceAsStream(filename);
						}
						hostDetailProperties = new Properties();
						hostDetailProperties.load(inputStream);
					} finally {
						if(inputStream != null)
							inputStream.close();
					}
				}
			}
		}
		return hostDetailProperties;
	}
	
	public enum FTPClientType { FTP, FTPS, SFTP , AWS_SFTP}
	
}
