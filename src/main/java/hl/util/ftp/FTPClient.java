package hl.util.ftp;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;
import java.util.regex.Pattern;

import org.apache.commons.net.ftp.FTPFile;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.SftpException;
import com.jcraft.jsch.ChannelSftp.LsEntry;

import hl.util.ftp.interceptor.Interceptor;

public abstract class FTPClient {
	
	private static Logger logger = Logger.getLogger(FTPClient.class);
	private final String PROCESSED_DIR = "processed";
	
	protected Interceptor interceptor;
	
	protected String HOSTNAME;
	protected int PORT;
	protected String USERNAME, PASSWORD;
	protected String baseDir;
	protected String toProcessedDir;
	protected String identityPath;
	
	public FTPClient() { }
	
	public FTPClient(String HOSTNAME, int PORT, String USERNAME, String PASSWORD) {
		this.USERNAME	= USERNAME;
		this.PASSWORD	= PASSWORD;
		this.HOSTNAME	= HOSTNAME;
		this.PORT		= PORT;
	}
		
	public String getHOSTNAME() {
		return HOSTNAME;
	}

	public void setHOSTNAME(String hostname) {
		HOSTNAME = hostname;
	}

	public int getPORT() {
		return PORT;
	}

	public void setPORT(int port) {
		PORT = port;
	}

	public String getUSERNAME() {
		return USERNAME;
	}

	public void setUSERNAME(String username) {
		USERNAME = username;
	}

	public void setPASSWORD(String password) {
		PASSWORD = password;
	}	
	
	public String getBaseDir() {
		return baseDir;
	}

	public void setBaseDir(String baseDir) {
		this.baseDir = baseDir;
	}

	public String getToProcessedDir() {
		return toProcessedDir;
	}

	public void setToProcessedDir(String toProcessedDir) {
		this.toProcessedDir = toProcessedDir;
	}

	public String getIdentityPath() {
		return identityPath;
	}

	public void setIdentityPath(String identityPath) {
		this.identityPath = identityPath;
	}

	public Interceptor getInterceptor() {
		return interceptor;
	}

	public void setInterceptor(Interceptor interceptor) {
		this.interceptor = interceptor;
	}
	
	/**
	 * Establishes the connection to the remote server
	 * 
	 * @return true iff the connection is established, false otherwise.
	 */
	public abstract boolean connect();
	
	/**
	 * Invalidates the active session, then closes the connection
	 */
	public abstract void disconnect();
		
	public List<?> listFiles(String pathname, Type type) {
		logger.info("listFiles() Listing down the files present in " + pathname + ", of type " + type.toString());		
		if(connect()) {
			try {
				@SuppressWarnings("unchecked")
				List<FTPFile> files 	= (List<FTPFile>) listFiles(pathname);
				if(type.equals(Type.ALL)) return files;
				List<FTPFile> newList 	= new ArrayList<FTPFile>();
				Iterator<FTPFile> fileIterator = files.iterator();
				while (fileIterator.hasNext()) {
					FTPFile ftpFile = (FTPFile) fileIterator.next();
					if(type.equals(Type.FILE) && ftpFile.isFile())
						newList.add(ftpFile);
					else if(type.equals(Type.DIR) && ftpFile.isDirectory())
						newList.add(ftpFile);					
				}
				return newList;
			} catch (Exception e) {
				e.printStackTrace();
				logger.error("listFiles() got an exception: " + e.getMessage());
			} finally {
				disconnect();
			}
		}
		return new ArrayList<>();
	}
	
	public abstract List<?> listFiles(String pathname) throws Exception;
	
	public abstract boolean checkIfDirExists(String pathname);
	
	/**
	 * Creates the <b>new directory</b> if the given directory isn't present in the remote system.
	 * It first compares to the working directory i.e. if the working directory is '/home/user/'
	 * and the given directory is '/home/user/some/directory', then create directory only
	 * from /some/directory.
	 * 
	 * @param directory
	 * @return true iff the directory is created successfully, false otherwise
	 */
	public boolean createDirectory(String directory) {
		logger.info("createDirectory() directory creation initiated: " + directory);
		if(directory == null) return false;
		boolean result 		= false;
		String[] dirNames 	= directory.split(Pattern.quote("" + File.separatorChar));
		try {
			String[] workingDirNames = getWorkingDirectory().split(Pattern.quote("" + File.separatorChar));
			int leastIndex			 = dirNames.length < workingDirNames.length ? 
									   		dirNames.length : workingDirNames.length;
			int index				 = 0;
				
			for(int i = 0; i < leastIndex; i++)
				if(dirNames[i].equalsIgnoreCase(workingDirNames[i]))
					if(!dirNames[i].isEmpty()) 
						index = i;
				
			String newpathname	= getWorkingDirectory() + File.separatorChar;
			
			if(index + 1 < dirNames.length)
				for(int i = index + 1; i < dirNames.length; i++) {
					newpathname = newpathname.concat(dirNames[i] + File.separatorChar);
					logger.info("createDirectory() Dir names to be created: " + newpathname);
					result = createDir(newpathname);
					logger.info("createDirectory() Result: " + result);
				}
		} catch (Exception e) {
			e.printStackTrace();
			logger.error("createDirectory() Got an exception while creating a directory");
			result = false;
		}
		return result;
	}
	
	private boolean createDir(String newPathname) throws Exception {
		logger.info("createDir() To create directory: " + newPathname);
		if(checkIfDirExists(newPathname)) {
			logger.info("createDir() Directory already exists: " + newPathname);
			return false;
		}
		return newPathname != null ? makeDirectory(newPathname) : false;
	}
	
	protected abstract boolean makeDirectory(String newPathname) throws Exception;
		
	public abstract String getWorkingDirectory() throws IOException, Exception;
	
	public abstract boolean changeWorkingDirectory(String pathname) throws IOException, Exception;
	
	public abstract boolean removeDir(String pathname, boolean ... toClose) throws Exception;
	
//	/public abstract boolean remove(String pathname) throws Exception;
	
	public abstract boolean moveFile(String filename, String srcPathname, String destPathname);
	
	/**
	 * Downloads the file(with filename) from the mentioned remote file path name, to the
	 * local system in the local file path name.
	 * 
	 * @param filename
	 * @param localPathname
	 * @param remotePathname
	 * @return true iff download is successful, false otherwise
	 * @throws Exception 
	 */
	public boolean download(String filename, String localPathname, String remotePathname) {
		logger.info("download() File: " + filename + " download initiated.");
		try {
			if(connect()) {
				
				JSONObject downloadParams = new JSONObject();
				downloadParams.put("task", "download");
				downloadParams.put("filename", filename);
				downloadParams.put("localDir", localPathname);
				downloadParams.put("remoteDir", remotePathname);
				
				File local = new File(localPathname);
				if(local.mkdirs())
					logger.warn("download() " + localPathname + " doesn't exist, creating the same.");
				
				getFile(filename, remotePathname, localPathname);
				
				if(interceptor != null)	interceptor.intercept(downloadParams);
				
				logger.info("download() File: " + filename + " downloaded from: " + remotePathname + " to: "
						+ localPathname + " successfully.");
				return true;
			}
		} catch (Exception e) {
			e.printStackTrace();
			logger.info("download() File: " + filename + " failed. Got an exception: " + e.getMessage());
		} finally {
			disconnect();
		}
		logger.error("download() File: " + filename + " download failed.");
		return false;
	}
		
	/**
	 * Downloads all files present in the remote path to the local destination.
	 * regex is used to download only those file with a filename that matches
	 * regex pattern
	 * 
	 * @param localPathname
	 * @param remotePathname
	 * @param regex
	 * @return true iff download is successful, false otherwise
	 * @throws Exception 
	 */
	public boolean download(String localPathname, String remotePathname, String[] regex) {
		logger.info("download() Files download initiated.");
		try {
			if(connect()) {
				
				JSONObject downloadParams = new JSONObject();
				downloadParams.put("task", "downloadMultiple");
				downloadParams.put("localDir", localPathname);
				downloadParams.put("remoteDir", remotePathname);
				
				File local = new File(localPathname);
				if(local.mkdirs())
					logger.warn("download() " + localPathname + " doesn't exist, creating the same.");
				
				JSONArray filenames = new JSONArray();
				
				@SuppressWarnings("unchecked")
				Vector<ChannelSftp.LsEntry> list = (Vector<LsEntry>) listFiles(remotePathname);
				for (Iterator<LsEntry> iterator = list.iterator(); iterator.hasNext();) {
					LsEntry lsEntry = (LsEntry) iterator.next();
					if(regex != null && regex.length > 0) {
						if(Pattern.compile(regex[0]).matcher(lsEntry.getFilename()).matches()) {							
							getFile(lsEntry.getFilename(), remotePathname, localPathname);
							filenames.put(lsEntry.getFilename());							
							logger.info("download() File: " + lsEntry.getFilename() + ", downloaded successfully.");
						}
					} else {
						getFile(lsEntry.getFilename(), remotePathname, localPathname);
						filenames.put(lsEntry.getFilename());						
						logger.info("download() File: " + lsEntry.getFilename() + ", downloaded successfully.");
					}
				}				
				downloadParams.put("filenames", filenames);				
				if(interceptor != null) interceptor.intercept(downloadParams);				
				return true;
			}
		} catch(Exception e) {
			e.printStackTrace();
			logger.info("download() Downloads from: " + remotePathname + " failed. Got an exception: " 
			+ e.getMessage());
		} finally {
			disconnect();
		}
		logger.error("download() Files download failed.");
		return false;
	}
	
	/**
	 * Retrieves the file from the remote file path to the local file path.
	 * 
	 * @param filename
	 * @param remotePathname
	 * @param localPathname
	 * @return
	 * @throws Exception 
	 */
	protected abstract boolean getFile(String filename, String remotePathname, String localPathname) throws Exception;
	
	/**
	 * Once the file are uploaded to the remote system, it is
	 * then moved to the processed folder under the source directory
	 * of the local system.
	 * 
	 * @param to
	 * @param filename
	 */
	private boolean onPostUpload(String to, String filename) {
		logger.info("onPostUpload(): post upload action initiated.");
		
		String localDestFileLoc  = to + File.separatorChar + this.PROCESSED_DIR;
		File newDir	 = new File(localDestFileLoc);
		if(!newDir.exists()) {
			logger.info("onPostUpload(): Creating the directory, " + localDestFileLoc);
			newDir.mkdirs();
		}
		
		String localDestFilePath = localDestFileLoc + File.separatorChar + filename;
		File oldFile = new File(to + File.separatorChar + filename);
		File newFile = new File(localDestFilePath);
		
		if(!oldFile.renameTo(newFile))
			if(newFile.delete()) {
				logger.info("onPostUpload(): Moving(to processed) " + oldFile.getName() + " to " + newFile.getName());
				return oldFile.renameTo(new File(localDestFilePath));
			}
		return false;
	}
	
	/**
	 * Uploads the file with a given filename to the remote system file path.
	 * 
	 * @param filename
	 * @param localPathname
	 * @param remotePathname
	 * @return
	 * @throws Exception 
	 */
	public boolean upload(String filename, String localPathname, String remotePathname) throws Exception {
		logger.info("upload() File upload initiated, filename: " + filename + ", from: " + localPathname + 
				", to: " + remotePathname);
		if(connect()) {
			
			System.out.println("Local Pathname: " + localPathname);
			JSONObject uploadParams = new JSONObject();
			uploadParams.put("task", "upload");
			uploadParams.put("filename", filename);
			
			if(this.toProcessedDir != null && this.toProcessedDir.equalsIgnoreCase("yes"))
				uploadParams.put("localDir", localPathname + File.separatorChar + this.PROCESSED_DIR);
			else
				uploadParams.put("localDir", localPathname);
			
			uploadParams.put("remoteDir", remotePathname);
			
			remotePathname = this.baseDir.concat(remotePathname);
			
			if(remotePathname == null || remotePathname.isEmpty()) { // Upload in the root directory
				logger.info("upload() Remote path name not mentioned, uploading in home directory.");
				boolean result = upload(filename, localPathname);
				
				if(result && interceptor != null) {	
					if(this.toProcessedDir != null && this.toProcessedDir.equalsIgnoreCase("yes"))
						onPostUpload(localPathname, filename);
					interceptor.intercept(uploadParams);					
				}
				
				return result;
			}
			
			createDirectory(remotePathname);
			
			logger.info("upload() After directory creation, if it wasn't present.");
			try {
				changeWorkingDirectory(remotePathname);
				logger.info("upload() Working directory changed to: " + remotePathname);				
				
				boolean result = upload(filename, localPathname);				
				if(result && interceptor != null) interceptor.intercept(uploadParams);	
								
				return result;
				
			} catch (IOException e) {
				e.printStackTrace();
				logger.error("upload() Got an exception while uploading the file: " + filename);
			} finally {
				disconnect();
			}
		}
		logger.error("upload() File: " + filename + " upload failed.");
		return false;
	}
	
	/**
	 * Uploads the list of files to the remote file path.
	 * 
	 * @param localPathname
	 * @param remotePathname
	 * @param filenames
	 * @return true iff upload is successful, false otherwise
	 * @throws Exception 
	 */
	public boolean upload(String localPathname, String remotePathname, String[] filenames) throws Exception {
		
		boolean result = false;
		if(connect()) {
			try {
				
				System.out.println("Localpath: " + localPathname);
				
				JSONObject uploadParams = new JSONObject();
				uploadParams.put("task", "uploadMultiple");
				
				if(this.toProcessedDir != null && this.toProcessedDir.equalsIgnoreCase("yes"))
					uploadParams.put("localDir", localPathname + File.separatorChar + this.PROCESSED_DIR);
				else
					uploadParams.put("localDir", localPathname);
				
				uploadParams.put("remoteDir", remotePathname);
				JSONArray jsonArray = new JSONArray();
				
				remotePathname = this.baseDir.concat(remotePathname);
				
				createDirectory(remotePathname);
				logger.info("upload() After directory creation, if it was present!");
				changeWorkingDirectory(remotePathname);
				logger.info("upload() Changed working directory to: " + remotePathname);
				for(String filename : filenames) {
					result = upload(filename, localPathname);
					
					if(this.toProcessedDir != null && this.toProcessedDir.equalsIgnoreCase("yes"))
						onPostUpload(localPathname, filename);
					
					if(result)
						jsonArray.put(filename);
					logger.info("upload() File: " + filename + ", upload result: " + result);
				}
				
				uploadParams.put("filenames", jsonArray);
				if(interceptor != null) interceptor.intercept(uploadParams);
				
				
			} catch (IOException e) {
				e.printStackTrace();
				logger.error("upload() Got an exception while uploading the files: " + filenames.toString());
			} finally {
				disconnect();
			}
		}
		return result;
	}
		
	/**
	 * Uploads the file with a given filename from a local path to a remote system.
	 * 
	 * @param filename
	 * @param localPathname
	 * @return true iff the upload is successful, false otherwise
	 * @throws Exception 
	 */
	private boolean upload(String filename, String localPathname) throws Exception {
		InputStream inputStream	= null;
		System.out.println("Filename --- " + filename);
		System.out.println("localPathname --- " + localPathname);
		try {
			inputStream 		= new FileInputStream(
					new File(getFilePath(filename, localPathname)));
			return storeFile(filename, inputStream);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			logger.error("upload() File: " + filename + " not found at: " + localPathname);			
		} catch (IOException e) {
			e.printStackTrace();
			logger.error("upload() File: " + filename + " upload failed. Got an exception.");
		} finally {
			if(inputStream != null)
				try {
					inputStream.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
		}
		logger.error("upload() File: " + filename + " upload failed.");
		return false;
	}
	
	/**
	 * If there's no File separator i.e. '/' for UNIX systems, '\\' for Windows systems
	 * provided at the end, then append the File separator at the end of the pathname.
	 * 
	 * @param filename
	 * @param pathname
	 * @return returns the full system independent file path.
	 */
	protected String getFilePath(String filename, String pathname) {
		if(pathname != null) {
			int length = pathname.length();
			return pathname.charAt(length - 1) == File.separatorChar ? pathname.concat(filename) :
				pathname.concat(File.separatorChar + filename);
		}
		return filename;
	}
	
	/**
	 * Stores a file on the remote system using the given filename 
	 * and taking input from the given InputStream.
	 * 
	 * @param filename
	 * @param inputStream
	 * @return true iff the store is complete, false otherwise
	 * @throws IOException
	 * @throws SftpException 
	 */
	protected abstract boolean storeFile(String filename, InputStream inputStream) throws Exception;
		
	public enum Type { ALL, DIR, FILE }

}
