package codedcaching;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.HashMap;
import java.util.Random;

public class ServerData 
{
	public static int NoOfusersConnected; // K
	public static int servSize; // N files 
	public static int cacSize;  // M bytes 
	public static int BYTE_IDENTITY_LENGTH = 4; 
	public static int BYTE_CACHEMAP_LENGTH ; // CacheMap of each block is (K + 7)/8  bytes long 
	private static String[] procureFileRequest; // {d_1, d_2, ... ,d_k}
	private File theDir = null;
	public static int BLOCK_SIZE = 7000; // Size of bytes to be considered 
	public static int SKIP_LENGTH;
	public static int[][] cacheBytesIndexes; // declare cacheBytesIndexes
	public static HashMap<HashMap<String, BitSet >, ArrayList<Byte> > serverDataTable = new HashMap< HashMap<String, BitSet >, ArrayList<Byte> >();
	public static HashMap<String, Long> serverFilesLenghtTable = new HashMap<String, Long>();  

	public ServerData(Initialize initObj)
	{
		ServerData.NoOfusersConnected = initObj.numberOfUsers;
		ServerData.cacSize = initObj.cacheFileSize;
		ServerData.procureFileRequest = initObj.usersFileRequest;
		this.bakeServerFiles();
	}
	
	private void bakeServerFiles()
	{
		File theTempDir = new File("TempServerFiles");
		if(!theTempDir.exists() && ! theTempDir.isFile())
		{
			try 
			{
				theTempDir.mkdir();
			} 
			catch (Exception e) 
			{
				// handle exception
				System.out.println("Couldn't create \"TempServerFiles\" Folder");
			}	 
		}
		
		for(File file: theTempDir.listFiles()) file.delete(); // delete all previous files in server folder
		
		File folder = new File("ServerFiles");
		ServerData.servSize = folder.listFiles().length;
		
		for (File file : folder.listFiles()) 
		{
			serverFilesLenghtTable.put(file.getName(), file.length());
			try 
			{
				int returnInt = 0;
				byte[] dataChar = new byte[BLOCK_SIZE];
				FileInputStream filein = new FileInputStream(file);
				File createTempServerFiles = new File(theTempDir.getPath()+"/"+file.getName());
				createTempServerFiles.createNewFile();
				FileOutputStream fileout = new FileOutputStream(createTempServerFiles);
				int pos = 0;
				
				while((returnInt = filein.read(dataChar, 0, BLOCK_SIZE)) != -1){
					fileout.write(dataChar);
					byte [] byteIdentity = ByteBuffer.allocate(BYTE_IDENTITY_LENGTH).putInt(pos).array(); // 4 bytes long Identity of each byte
					fileout.write(byteIdentity); 
					byte [] byteCacheMap = initializeByteCacheMap();
					fileout.write(byteCacheMap); // (K + 7)/8 byte long cacheMap of each byte
					pos = pos+1;
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
	}
	
	private byte[] initializeByteCacheMap()
	{
		BitSet b = new BitSet(NoOfusersConnected);
		b.set(0, NoOfusersConnected, true);// set all K bits as one, LSB is index  0.  
		BYTE_CACHEMAP_LENGTH = b.toByteArray().length;
		SKIP_LENGTH = BYTE_IDENTITY_LENGTH + BYTE_CACHEMAP_LENGTH;
		
		return b.toByteArray();
		//returns a byte array containing a little-endian representation of all the bits in this bit set
	}

	public int getByteCacheMapLength()
	{
		return BYTE_CACHEMAP_LENGTH;
	} 
	
	public String getNthUserFileRequest(int n)
	{
		return procureFileRequest[n];
	}
	
	public static File[] getAllServerFiles()
	{
		File folder = new File("ServerFiles");
		return folder.listFiles(); // return fileArray
	}

	public static ArrayList<String> getAllServerFileNames()
	{
		File folder = new File("ServerFiles");
		File[] fileList = folder.listFiles();
		ArrayList<String> file_names = new ArrayList<String>();
		for(File fl:fileList)
		{
			file_names.add(fl.getName());
		}
		return file_names;	
	}
	
	public static File getNthServerFile(String nme)
	{
		File folder = new File("TempServerFiles");
		boolean fileFound = false;
		File[] fileList = folder.listFiles();
		for(File file : fileList){
			if(file.getName().equals(nme))
			{
				fileFound = true;
				return file;
			} 
		}
		if(!fileFound) System.out.println("No requested file with such name found!");
		return null;
	}
	
	public static void beginCaching()
	{
		File folder = new File("TempServerFiles");
		File[] serverFiles = folder.listFiles();
		
		for (File file : serverFiles) 
		{			
			int file_block_counts  = (int) (file.length())/(SKIP_LENGTH + BLOCK_SIZE);
//			int fileSize =  BLOCK_SIZE*file_block_counts;
			long fileSize = file.length();
			cachingProbability caching_probability = new cachingProbability(fileSize);
			double uniform_caching_probability = caching_probability.uniform();
			System.out.println("Caching prob: "+ uniform_caching_probability);
			int cached_block_counts = 0;
			
			if(uniform_caching_probability >= 1)
			{
				cached_block_counts = (file_block_counts);
				System.out.println("Cache Size is large enough to store complete file during peak-off times");
			}
			else
			{
				cached_block_counts = (int) (uniform_caching_probability*file_block_counts);
			}
			System.out.println("Number of blocks cached: "+ cached_block_counts);
			
			// allocate memory for storing (NoOfusersConnected)*(cacSize*fileSize)/servSize elements
			cacheBytesIndexes = new int[NoOfusersConnected][cached_block_counts];
			
			// updating cacheMap starts
			for(int usr = 0; usr < NoOfusersConnected; ++usr)
			{
				int[] rn = generateRandomIndexes(cached_block_counts,  file_block_counts);
				// write the bytes in sorted index order form at cache 
				Arrays.sort(rn);
				System.arraycopy( rn, 0, cacheBytesIndexes[usr], 0, cached_block_counts );
				updateCacheMap(file.getPath(), rn, usr);
			}
			//update cacheMap finished
			
			//begin retrieval of the cached blocks from stored indexes
			for(int usr = 0; usr < NoOfusersConnected; ++usr)
			{
				FileOutputStream fileCacheOut = null;
				try 
				{
					// write cached blocks to the cache file of this user
					fileCacheOut = new FileOutputStream(CacheData.theDir.getPath()+"/User"+Integer.toString(usr+1)+"/"+file.getName(), true);
				} 
				catch (FileNotFoundException e1) 
				{
					e1.printStackTrace();
				}

				try 
				{
					RandomAccessFile thisServerfileIn = new RandomAccessFile(file, "rw");
					for(int i = 0; i < cacheBytesIndexes[usr].length; i++ )
					{
						byte[] buff = new byte[SKIP_LENGTH + BLOCK_SIZE];
						thisServerfileIn.seek(cacheBytesIndexes[usr][i]*(SKIP_LENGTH + BLOCK_SIZE));
						thisServerfileIn.read(buff, 0, SKIP_LENGTH + BLOCK_SIZE);
						fileCacheOut.write(buff);
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
			///////////////////////////////////////////////////////////
			// preparing the HashTable  
//			ServerData.prepareLookUpTable(file);
			///////////////////////////////////////////////////////////	
		}
//		System.out.println("HashTable finished");
	}
	
	public static void prepareLookUpTable(File file)
	{
		int returnInt = 0;
		RandomAccessFile fileIn = null;
		try {
			fileIn = new RandomAccessFile(file, "rw");
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		byte[] buff = new byte[SKIP_LENGTH + BLOCK_SIZE];
		HashMap<String, BitSet > key = new HashMap<String, BitSet >();
		try {
			while((returnInt = fileIn.read(buff, 0, buff.length))!=-1){
				byte[] buffCacheMap = new byte[ServerData.BYTE_CACHEMAP_LENGTH];
				System.arraycopy(buff, ServerData.BLOCK_SIZE + ServerData.BYTE_IDENTITY_LENGTH, buffCacheMap, 0, ServerData.BYTE_CACHEMAP_LENGTH);
				key.put(file.getName(), BitSet.valueOf(buffCacheMap));
				ArrayList<Byte> value = ServerData.serverDataTable.get(key);
				if(value == null){
					// mapping is not present for the key
					ArrayList<Byte> firstEntry = new ArrayList<Byte>();
					for(int idx = 0; idx < buff.length; idx++){
						firstEntry.add(buff[idx]);
					}
					ServerData.serverDataTable.put(key, firstEntry);
				}
				else{
					for(int idx = 0; idx < buff.length; idx++){
						value.add(buff[idx]);
					}
					ServerData.serverDataTable.put(key, value);
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}	 
	}
	
	public static void updateCacheMap(String updatefile, int[] pos, int id)
	{
		RandomAccessFile UpdateFile = null;
		try 
		{
			UpdateFile = new RandomAccessFile(updatefile,"rw");
		} 
		catch (FileNotFoundException e) 
		{
			e.printStackTrace();
		}
		
		byte[] cacheMapUpdateBytes = new byte[BYTE_CACHEMAP_LENGTH];
		
		for(int i = 0; i < pos.length; i++)
		{
			try 
			{
				UpdateFile.seek(pos[i]*(SKIP_LENGTH + BLOCK_SIZE) + (BLOCK_SIZE + BYTE_IDENTITY_LENGTH));
				UpdateFile.read(cacheMapUpdateBytes, 0, BYTE_CACHEMAP_LENGTH);
				
				// bytes array to BitSet
				BitSet	cacheMapUpdateBitSet = BitSet.valueOf(cacheMapUpdateBytes);
				cacheMapUpdateBitSet.flip(id);
				//The LSB is index 0(first user), BitSet indexing is from Left to Right
				UpdateFile.seek(pos[i]*(SKIP_LENGTH + BLOCK_SIZE) + (BLOCK_SIZE + BYTE_IDENTITY_LENGTH ));
				UpdateFile.write(cacheMapUpdateBitSet.toByteArray());
			} 
			catch (IOException e) 
			{
				e.printStackTrace();
			}
		}
	}

	public static int[] generateRandomIndexes(int Gn, int Rn )
	{
		Random rand = new Random();
		int[] randomArray = new int[Gn];
		ArrayList<Integer> RandomIntegerList = new ArrayList<Integer>();
		for (int i = 0; i < Rn; i++) 
		{
			RandomIntegerList.add(i);
		}
		
		for (int i = 0; i < Gn; i++) 
		{ 
			// FisherYates Shuffle method generate MF/N different random integers  
			//randomArray[i] = rand.nextInt(Rn);  // Random.nextInt((max - min) + 1) + min, generate a random number between [max, min]
			int rndIndex = rand.nextInt(RandomIntegerList.size());
			randomArray[i] = RandomIntegerList.get(rndIndex);
			RandomIntegerList.remove(rndIndex);
		}
		return randomArray;
	}
}

class cachingProbability
{
	private HashMap<String, Long> file_length_table = null;
	private long fileSize = 0;
	public static int IN_KB = 1000;
	
	public cachingProbability(long file_size)
	{
		 this.file_length_table = ServerData.serverFilesLenghtTable;
		 this.fileSize = file_size;
	}
	
	// Uniform caching irrespective of file size
	public double uniform()
	{
		double total_unifrom_storage = 0;
		double ql;
		for (String key : file_length_table.keySet()) 
		{
			total_unifrom_storage = total_unifrom_storage + (double)file_length_table.get(key)/IN_KB;	
		}
		ql = (ServerData.cacSize)/(total_unifrom_storage*(1 + ServerData.SKIP_LENGTH/ServerData.BLOCK_SIZE));
		return ql;
	}
	
	// Caching linear with file size 
	public double linear()
	{
		double total_linear_storage = 0;
		double ql;
		for (String key : file_length_table.keySet()) 
		{
			total_linear_storage = total_linear_storage + Math.pow(file_length_table.get(key)*(1 + ServerData.SKIP_LENGTH/ServerData.BLOCK_SIZE)/IN_KB, 2);	
		}
		ql = ServerData.cacSize*(fileSize/IN_KB)/total_linear_storage;
		return ql;
	}
}