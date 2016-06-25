package codedcaching;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;

public class CacheData 
{
	public static File theDir= new File("Users");
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
		ArrayList<String> ServerFilesNames = ServerData.getAllServerFileNames(); 
		
		for(int user = 1; user <= usersCount; user++)
		{
			File userdir = new File("Users/User"+Integer.toString(user));
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
			
			File dir = new File( userdir.getPath() + "/CacheFiles");
			if(!dir.exists() && ! dir.isFile())
			{
				try 
				{
					dir.mkdir();
				} 
				catch (Exception e) 
				{
					// handle exception
					System.out.println("Couldn't create \"CacheFiles\" Folder");
				}	
			}
			
			try 
			{
				for(int cacfile = 0; cacfile < serverFileCount; cacfile++)
				{
					File createFile = new File(dir.getPath()+"/"+ServerFilesNames.get(cacfile));
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
		File[] fileList = new File[usersCount]; 
		for(int user = 1; user <= usersCount; user++)
		{
			File file = new File("Users/User"+Integer.toString(user)+ "/CacheFiles");
			fileList[user-1] = file;
		}
		return fileList; // return fileArray
	}
	
	public File getNthUserCacheFile(int k)
	{
		if( k > 0 && k <= usersCount)
		{
			File folder = new File("Users/User"+Integer.toString(k)+ "/CacheFiles");
			return folder;
		}
		return null;
	}
	
	public boolean checkFileCached()
	{
		return true;
	}
}
