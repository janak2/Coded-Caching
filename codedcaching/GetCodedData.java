package codedcaching;

import java.io.File;	
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

// This file resides on client side, containing the single coded transmitted message 
public class GetCodedData 
{
	public static File thedir = null;
	
	public GetCodedData()
	{
		this.createCodedDataFile();
	}
	
	private void createCodedDataFile()
	{
		thedir = new File("NonCachedCodedData");
		if(!thedir.exists() && ! thedir.isFile())
		{
			try 
			{
				thedir.mkdir();
			} 
			catch (Exception e) 
			{
				// handle exception
				System.out.println("Couldn't create \"NonCachedCodedData\" Folder");
			}	
		}
		
		for(File file: thedir.listFiles()) file.delete(); // delete the previous coded data file
		
		try 
		{
			File createFile = new File(thedir.getPath()+"/codedTransmittedfile.txt");
			createFile.createNewFile(); 		
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
	
	public static void sendCodedbytes(byte[] meSmilingData)
	{
		FileOutputStream codedDatafileout = null;
		try 
		{
			//the "true" argument appends the new content in the end of the file, without overwriting on previous content
			codedDatafileout = new FileOutputStream(thedir.getPath()+"/codedTransmittedfile.txt", true); 
		} 
		catch (FileNotFoundException e) 
		{
			e.printStackTrace();
		}
		try 
		{
			codedDatafileout.write(meSmilingData);
		} 
		catch (IOException e) 
		{
			e.printStackTrace();
		}
	}
}
