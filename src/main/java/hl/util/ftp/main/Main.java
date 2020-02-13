package hl.util.ftp.main;

import hl.util.ftp.FTPClient;
import hl.util.ftp.factory.FTPFactory;
import hl.util.ftp.factory.FTPFactory.FTPClientType;

public class Main {

	public static void main(String[] args) {
		try {
			FTPClient ftpClient = FTPFactory.getFTPClient(FTPClientType.FTP);
			// ftpClient.upload("customLogo.gif", "/home/santosh/FTP/local/", 
			//		"/home/santosh/FTP/remote/img");
			ftpClient.download("curl.sh", "/home/santosh/Test/ftp", "Test");
		} catch (Exception e) {
			e.printStackTrace();
		}
		/*
		FTPClient ftpClient = null;
		try {
			ftpClient = FTPFactory.getFTPClient(FTPClientType.SFTP);
			//ftpClient.setInterceptor(new DefaultInterceptor());
			// ftpClient.moveFile("commission_statement1.sql", "/HL_CMS/", "/HL_CMS/sql"); // Working fine
			// ftpClient.removeDir("/HL_CMS/newsql", true);									 // Working fine
			// ftpClient.upload("commission_statement.sql", "/home/santosh", 
			//	"/HL_CMS/files/");														 // Working fine
			// Working fine
			// ftpClient.upload("/home/santosh/FTP/SFTP/download/", "/HL_CMS/files/", new String[] {"one.sql", "two.sql"});
			 ftpClient.download("commission_statement1.sql", "/home/santosh", "/HL_CMS/sql");	 // Working fine
			String regex = "[a-zA-Z]+[0-9]*_(H|I)V_(R_)?.csv"; // Regex of a filename for WURTH
			System.out.println(Pattern.compile(regex).matcher("a_IV_.csv").matches());
			// Working fine
			// ftpClient.download("/home/santosh/FTP/SFTP/download/", "/HL_CMS/files/", new String[] { "[a-zA-Z0-9]+.sql" });
		} catch (IOException e) {
			e.printStackTrace();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			
		}
		*/
	}
	
}
