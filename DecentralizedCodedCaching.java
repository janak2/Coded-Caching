import codedcaching.*;

public class DecentralizedCodedCaching 
{
	static int usersCount = 7;     // K users 
	static int cacheSize =  75000;     // cache in KB
	static String[] fileFetchRequest = {"1.flv","1.png","1.avi", "1.mp4", "1.pdf", "2.pdf", "3.pdf"}; // {d_1, d_2, ... , d_k} fetch requests in order -- user1, user2, ....userk

	public static void main(String[] args) 
	{
		Initialize cacheServerInit= new Initialize(usersCount, cacheSize, fileFetchRequest); 
		ServerData serverObj = new ServerData(cacheServerInit); //Build Server
		CacheData cacheObj = new CacheData(cacheServerInit); //Build ClientsCache
		FullReclaimedData recoveredData = new FullReclaimedData(cacheServerInit);// Get requested files	
	}
}
