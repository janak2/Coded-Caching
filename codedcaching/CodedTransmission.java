package codedcaching;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.HashMap;
import java.util.Set;

public class CodedTransmission 
{
	public static void transmitCodedData(Set<Integer>S, String[] userRequest, int userCount )
	{ 	
	// arguments( S, user, requestFile )
		BitSet codedbits = new BitSet(); // all BitSet values are initially set false
//		String userString = "";
		BitSet usersBitset = new BitSet(userCount);
		usersBitset.set(0, S.size(), false);
		for (int user : S) 
		{
			// just send a BitSet of K bits, set and unsets, depending on whom it concern
			usersBitset.flip(user); // setting 0
			BitSet cachemap  = new BitSet(userCount);
			cachemap = createthisCacheMap(userCount, user, S);
			File thisRequestFile = ServerData.getNthServerFile(userRequest[user]);
			codedbits.xor(getAssociatedBitsToThisCacheMap(thisRequestFile, cachemap));
//			codedbits.xor(getAssociatedHashBitsToThisCacheMap(thisRequestFile, cachemap));
		}
		
		byte[] receivers = usersBitset.toByteArray();
		GetCodedData.sendCodedbytes(receivers);
		byte [] NoBytesToRead = ByteBuffer.allocate(ServerData.BYTE_IDENTITY_LENGTH).putInt(codedbits.toByteArray().length).array();
		GetCodedData.sendCodedbytes(NoBytesToRead);
		GetCodedData.sendCodedbytes(codedbits.toByteArray()); // give little-endian
		System.out.println("Transmitting... Coded Data!");
	}
	
	public static BitSet createthisCacheMap(int usercount, int user, Set<Integer>subset )
	{
		BitSet b = new BitSet(usercount);
		b.set(0, usercount, true);// LSB is index  0
		for(int u:subset)
		{
			b.flip(u);  // these cacheMap bits represents: bytes present at all other users except the user itself; S - {user}  
		}
		b.flip(user); // byte not present at the user itself
		return b;
	}
	
	public static BitSet getAssociatedHashBitsToThisCacheMap(File seekFile, BitSet findthisBitset){
		HashMap<String, BitSet > key = new HashMap<String, BitSet >();
		key.put(seekFile.getName(), findthisBitset);
		ArrayList<Byte> nonCachedBytesList = new ArrayList<Byte>();
		nonCachedBytesList = ServerData.serverDataTable.get(key);
		if (nonCachedBytesList == null){
			BitSet tod = new BitSet();
			tod.clear();
			return tod;
		}
		else{
			int n = nonCachedBytesList.size();
			byte[] out = new byte[n];
			for (int i = 0; i < n; i++) {
			  out[i] = nonCachedBytesList.get(i);
			  //System.out.println((char)out[i]);
			}
			// out should be little-endian form
			ByteBuffer bb = ByteBuffer.wrap(out);
			bb.order( ByteOrder.LITTLE_ENDIAN);
			out = bb.array();
			return BitSet.valueOf(out); // converting bytes array to BitSet; returning BitSet
		}	
	}
	
	public static BitSet getAssociatedBitsToThisCacheMap(File seekFile, BitSet findthisBitset)
	{
		int returnInt = 0;
		ArrayList<Byte> nonCachedBytesList = new ArrayList<Byte>();
		FileInputStream nonCachedataIn = null;
		
		try 
		{
			nonCachedataIn = new FileInputStream(seekFile);
		} 
		catch (FileNotFoundException e) 
		{
			e.printStackTrace();
		}
		
		boolean found = false;
		byte[] readbuff = new byte[ServerData.BLOCK_SIZE + ServerData.SKIP_LENGTH];
		
		try 
		{
			while((returnInt = nonCachedataIn.read(readbuff, 0, readbuff.length))!=-1)
			{
				byte[] readbuffCaheMap = new byte[findthisBitset.toByteArray().length];
				System.arraycopy(readbuff, ServerData.BLOCK_SIZE + ServerData.BYTE_IDENTITY_LENGTH, readbuffCaheMap, 0, ServerData.BYTE_CACHEMAP_LENGTH);
				if(Arrays.equals(readbuffCaheMap, findthisBitset.toByteArray()))
				{
					found = true;
					// use addAll
					for (int i = 0; i < readbuff.length; i++) 
					{
						nonCachedBytesList.add(readbuff[i]);
					}
				}
			}
		} 
		catch (IOException e) 
		{
			e.printStackTrace();
		}
		
		finally
		{
	        if(nonCachedataIn!=null)
	        {
				try 
				{
					nonCachedataIn.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
	    }
		
		if(found)
		{
			int n = nonCachedBytesList.size();
			byte[] out = new byte[n];
			for (int i = 0; i < n; i++) 
			{
			  out[i] = nonCachedBytesList.get(i);
			  //System.out.println((char)out[i]);
			}
			
			// out should be little-endian form
			ByteBuffer bb = ByteBuffer.wrap(out);
			bb.order( ByteOrder.LITTLE_ENDIAN);
			out = bb.array();
			return BitSet.valueOf(out); // converting bytes array to BitSet; returning BitSet
		}
		else
		{
			BitSet tod = new BitSet();
			tod.clear();
			return tod; 
		}
	}
}