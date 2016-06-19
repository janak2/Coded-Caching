package codedcaching;
 
public class Initialize 
{
	int numberOfUsers;         // K 
	int cacheFileSize;         // M
	String[] usersFileRequest;	   // {d_1, d_2, ... ,d_k}
	
	public Initialize(int noOfUsers, int cacheSize, String[] filesRequest)
	{
		this.numberOfUsers = noOfUsers;
		this.cacheFileSize = cacheSize;
		this.usersFileRequest = filesRequest.clone();
	}

	int getNumberOfUsers()
	{
		return this.numberOfUsers;
	}
	
	int getCacheSize()
	{
		return this.cacheFileSize;
	}
	
	String[] getFileRequests()
	{
		return this.usersFileRequest ;
	}
}
