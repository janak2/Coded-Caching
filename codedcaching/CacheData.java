package codedcaching;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;

public class CacheData 
{
	public static File theDir= null;
	public static File thesubDir = null;
	private String[]  filerequest;
	private File[] serverfiles;
	private int usersCount;
	private int serverFileCount;
//	private int fileSize;
	public int skiplength;
	public  File[] cachefiles; 
	public static HashMap<HashMap<String, BitSet >, ArrayList<byte[]> > cacheDataTable = new HashMap< HashMap<String, BitSet >, ArrayList<byte[]> >();
	
	public CacheData(Initialize initObj)
	{
		this.filerequest = initObj.usersFileRequest;
		this.usersCount = initObj.numberOfUsers;
//		this.fileSize = ServerData.fileSize;
		this.serverFileCount = ServerData.servSize;
		this.skiplength  = ServerData.SKIP_LENGTH;
//		ServerData serverObj = new ServerData(initObj);
//		this.serverfiles = serverObj.getAllServerFiles();
		this.createCacheFiles();
	}
	
	private void createCacheFiles()
	{
		theDir = new File("CacheFiles");
		
		if(!theDir.exists() && ! theDir.isFile())
		{
			try 
			{
				theDir.mkdir();
			} 
			catch (Exception e) 
			{
				// handle exception
				System.out.println("Couldn't create \"CacheFiles\" Folder");
			}	
		}
		
		for(int user = 1; user <= usersCount; user++)
		{
			File userdir = new File("CacheFiles/User"+Integer.toString(user));
			if(!userdir.exists() && ! userdir.isFile())
			{
				try 
				{
					userdir.mkdir();
				} 
				catch (Exception e) 
				{
					// handle exception
					System.out.println("Couldn't create \"CacheFiles\" User's Folder");
				}	
			}
			for(File file: userdir.listFiles()) file.delete(); // delete all previous files in cache folder
		}

		ArrayList<String> ServerFilesNames = ServerData.getAllServerFileNames(); 
		
		for (int userid = 1; userid <= usersCount; ++userid) 
		{
			try 
			{
				for(int cacfile = 0; cacfile < serverFileCount; cacfile++)
				{
					File createFile = new File(theDir.getPath()+"/User"+Integer.toString(userid)+"/"+ServerFilesNames.get(cacfile));
					createFile.createNewFile();
				}
			}
			catch (FileNotFoundException e)
			{
				e.printStackTrace();
			} 
			catch (IOException e) 
			{
				e.printStackTrace();
			}
		}

		// start caching;
		ServerData.beginCaching();
	}

	public File[] getAllCacheFiles()
	{
		File folder = new File("CacheFiles");
		return folder.listFiles(); // return fileArray
	}
	
	public File getNthUserCacheFile(int k)
	{
		File folder = new File("CacheFiles");
		File[] fileList = folder.listFiles();
	
		if( k > 0 && k <= usersCount)
		{
			return fileList[k];
		}
	
		return fileList[k % (usersCount+1)];
	}
	
	public boolean checkFileCached()
	{
		return true;
	}
}
