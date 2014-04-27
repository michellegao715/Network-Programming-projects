
public class test1 {
	public static String changeIntToHexString(int i) {
		char low = i / 16 < 10 ? (char)(i/16 + '0') : (char)(i/16-10+'a');
		char high = i% 16 < 10 ? (char)(i%16 +'0'): (char)(i%16 - 10 + 'a');
		return ""+ low + high;
	}
	
	
	public static String changeStringToHexString(String s) {
		String completeHexString = "";
		for(int i = 0 ; i<s.length(); i++){
			char c = s.charAt(i);
			String oneHexString = changeIntToHexString(c);
			completeHexString += oneHexString;
		}
		return completeHexString;
	}
	public static String ToHexString(String domain) {
		//Step 1. If domain has "." convert anything before "." to hexString
		String hexString = "";

		if(domain.contains(".")) {
			int indexOfDot = domain.indexOf(".");
			// TODO changeIntToHexString
			String hexStringOfSize = changeIntToHexString(indexOfDot);
			//TODO change string to hex string.
			
			String hexStringOfDomain = changeStringToHexString (domain.substring(0,indexOfDot));

			String restDomain = domain.substring(indexOfDot+1, domain.length());
			 hexString = hexStringOfSize+hexStringOfDomain + ToHexString(restDomain);
		}
		// If domain not contain "."
		else {
			//Step 2. If domain has not "."  change to size+hexString+"00"
			String hexStringOfSize = changeIntToHexString(domain.length());
			String hexStringOfDomain = changeStringToHexString (domain);
			 hexString = hexStringOfSize + hexStringOfDomain + "00";
		}
		return hexString;
	}
	
	public static void main(String [] args) {
		System.out.println(changeIntToHexString(3));
		System.out.println(changeIntToHexString(6));
		System.out.println(changeIntToHexString(3));
		System.out.println(changeIntToHexString(2));
		System.out.println(ToHexString("www.google.com.hk"));
	}
}
