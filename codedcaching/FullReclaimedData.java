package codedcaching;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class FullReclaimedData 
{
	private String[] filesrequest;	   // {d_1, d_2, ... ,d_k}
	private int userNo;
	public static File recoveredDir = null;
	List<Integer> superSet = new ArrayList<Integer>(); // {1, 2, ... , K}
	
	public FullReclaimedData(Initialize initObj) 
	{
		this.filesrequest = initObj.usersFileRequest;
		this.userNo = initObj.numberOfUsers;
		this.createUsersSuperSet();
		this.getCodedData(filesrequest);
		this.generateFile();
		PostProcessing pp = new PostProcessing();
		pp.truncteFiles();
	}
	
	private void createUsersSuperSet()
	{
		for (int i = 0; i < userNo; ++i) 
		{
			this.superSet.add(i);
		}
	}
	
	private static void getSubsets(List<Integer> superSet, int k, int idx, Set<Integer> current,List<Set<Integer>> solution) 
	{
	    //successful stop clause
	    if (current.size() == k) 
	    {
	        solution.add(new HashSet<Integer>(current));
	        return;
	    }
	    
	    //unsuccessful stop clause
	    if (idx == superSet.size()) return;
	    Integer x = superSet.get(idx);
	    current.add(x);
	    
	    //x is in the subset
	    getSubsets(superSet, k, idx+1, current, solution);
	    current.remove(x);
	    
	    // x is not in the subset
	    getSubsets(superSet, k, idx+1, current, solution);
	}
	
	public static List<Set<Integer>> getSubsets(List<Integer> superSet, int k) 
	{
	    List<Set<Integer>> res = new ArrayList<Set<Integer>>();
	    getSubsets(superSet, k, 0, new HashSet<Integer>(), res);
	    return res;
	}
	
	public void getCodedData(String[] fileFetchRequest)
	{
		GetCodedData beginTransmission = new GetCodedData();
		for (int s = 1; s <= userNo; s++) 
		{    
			// s = |S| 
			List<Set<Integer>>userSubsets = getSubsets(superSet, s);
			for (int i = 0; i < userSubsets.size(); i++) 
			{
				// requesting server to send coded data
				CodedTransmission.transmitCodedData(userSubsets.get(i), fileFetchRequest, fileFetchRequest.length ); 
			}
//			System.out.println(getSubsets(superSet, s));
		}
		System.out.println("Transmission End!");
	}
	
	private void generateFile()
	{
		recoveredDir = new File("RecoveredFiles");
		int returnInt = 0;
		if(!recoveredDir.exists() && ! recoveredDir.isFile())
		{
			try 
			{
				recoveredDir.mkdir();
			} 
			catch (Exception e) 
			{
				// handle exception
				System.out.println("Couldn't create \"RecoveredFiles\" Folder");
			}	
		}
		for(File file: recoveredDir.listFiles()) file.delete(); // delete all previous files in recovered folder
		
		File getCodedFile = new File(GetCodedData.thedir.getPath()+"/codedTransmittedfile.txt");
		System.out.println("Total Data transmitted: "+ getCodedFile.length()/1000000.0+ " MB");
		
		for (int user = 0; user < userNo; user++) 
		{
			try 
			{
				File recoveredFile = new File(recoveredDir.getPath()+"/"+filesrequest[user]);
				recoveredFile.createNewFile();
				FileInputStream getCodedFileIn = new FileInputStream(getCodedFile);
				byte[] receivers = new byte[ServerData.BYTE_CACHEMAP_LENGTH];
				byte[] bytesToRead = new byte[ServerData.BYTE_IDENTITY_LENGTH]; // number of bytes to be read
				while((returnInt = getCodedFileIn.read(receivers, 0, ServerData.BYTE_CACHEMAP_LENGTH))!=-1)
				{
					Set<Integer> userSet = BitsetToSet(receivers);
					getCodedFileIn.read(bytesToRead);
					int bytesCount = java.nio.ByteBuffer.wrap(bytesToRead).getInt();
					byte[] codedBytebuffer = new byte[bytesCount];
					getCodedFileIn.read(codedBytebuffer, 0, bytesCount); // in little-endian format
					if(userSet.contains(user))
					{
						DecodeData(recoveredFile, codedBytebuffer, user, userSet);
					}		
				} 
				File fl_seek = new File(CacheData.theDir.getPath()+"/User"+Integer.toString(user+1)+"/"+filesrequest[user]);
				InputStream cacheStream = new FileInputStream(fl_seek);
				WriteDataBlocksFromStream(cacheStream, recoveredFile);
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
	}
	
	private Set<Integer> BitsetToSet(byte[] b)
	{
		BitSet userBitSet = BitSet.valueOf(b);
		Set<Integer> userSet = new HashSet<Integer>();
		// iterate over the true bits in a BitSet
		for(int i = userBitSet.length(); (i = userBitSet.previousSetBit(i-1)) >= 0;)
		{
			userSet.add(i);
		}	
		return userSet;
	}

	public void DecodeData( File fileRecovered, byte[] nonCachedCodedBytes, int userid, Set<Integer>S)
	{
		// CacheFile is the cache file of "the user" itself
		BitSet CachedBits = BitSet.valueOf(nonCachedCodedBytes);  // nonCachedCodedBytes is already in little-endian format 
		for (int u : S) 
		{
			if(u!= (userid))
			{
				BitSet cachemap = CodedTransmission.createthisCacheMap(userNo, u, S);
				//user looking for the cache bytes, bytes required by "u" cached at "user" with this cacheMap "cachemap"
				File fl_seek = new File(CacheData.theDir.getPath()+"/User"+Integer.toString(userid+1)+"/"+filesrequest[u]);
				CachedBits.xor(CodedTransmission.getAssociatedBitsToThisCacheMap(fl_seek, cachemap)); 
			}
		}
		
		// Now writing the decoded bytes to the recovered file
		ByteBuffer bb = ByteBuffer.wrap(CachedBits.toByteArray());
		bb.order( ByteOrder.BIG_ENDIAN);
		byte[] meBuffer = bb.array();
		InputStream codedBytesStream = new ByteArrayInputStream(meBuffer);
		WriteDataBlocksFromStream(codedBytesStream, fileRecovered);
	}

	public void WriteDataBlocksFromStream( InputStream FromThisStream, File ToThisFile)
	{
		try 
		{
			int returnInt = 0;
			RandomAccessFile UpdateFile = new RandomAccessFile(ToThisFile,"rw");
			byte[] readbuff = new byte[ServerData.BLOCK_SIZE + ServerData.SKIP_LENGTH];
			
			//  "data byte", next four bytes represent its identity and remaining bytes represent CacheMap of the data byte
			byte[] dataBlock = new byte[ServerData.BLOCK_SIZE];
			byte[] blockId = new byte[ServerData.BYTE_IDENTITY_LENGTH];
			while((returnInt = FromThisStream.read(readbuff, 0, readbuff.length))!=-1)
			{
				for (int i = 0; i < (ServerData.BLOCK_SIZE + ServerData.BYTE_IDENTITY_LENGTH); i++) 
				{  
					if(i < ServerData.BLOCK_SIZE)
					{
						dataBlock[i] = readbuff[i];
					} 
					else
					{
						blockId[i-ServerData.BLOCK_SIZE] = readbuff[i];
					}
				}

				String str = new String(dataBlock, StandardCharsets.UTF_8);
//				System.out.println("-> String at pos "+ java.nio.ByteBuffer.wrap(blockId).getInt()+": "+str);
				UpdateFile.seek(java.nio.ByteBuffer.wrap(blockId).getInt()*ServerData.BLOCK_SIZE);
				UpdateFile.write(dataBlock); // write data
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
	
	class PostProcessing
	{
		private void truncteFiles()
		{
			for(File file: recoveredDir.listFiles())
			{
				try 
				{ 
					RandomAccessFile fn = new RandomAccessFile(file, "rw");
					long fileLength = ServerData.serverFilesLenghtTable.get(file.getName());
					fn.setLength(fileLength);
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
		}
		
		private boolean ComapreTwoFiles(File one, File two) throws IOException
		{
			//character Input stream class
	        FileReader fileOne = new FileReader(one);
	        FileReader fileTwo = new FileReader(two);

	        BufferedReader readerOne = new BufferedReader(fileOne);
	        BufferedReader readerTwo = new BufferedReader(fileTwo);

	        String lineOne = null;
	        String lineTwo = null;
	        
	        List<String> listOne = new ArrayList<String>();
	        List<String> listTwo = new ArrayList<String>();

	        while ((lineOne = readerOne.readLine()) != null) 
	        {
	            listOne.add(lineOne);
	        }

	        while ((lineTwo = readerTwo.readLine()) != null) 
	        {
	            listTwo.add(lineTwo);
	        }

	        readerOne.close();
	        readerTwo.close();
	        return listOne.equals(listTwo);
		}
	}		
}
