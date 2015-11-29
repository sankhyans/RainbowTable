import java.io.FileWriter;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Set;
import javax.xml.bind.annotation.adapters.HexBinaryAdapter;

public class RainbowTable {
	private MessageDigest SHA1; // 160 bits
	private int L_CHAIN = 300, N_CHAIN = 39000; // Length and number of chains
	static String rTableFileName = "RTABLE"; // Rainbow Table File Name

	// Function to generate Rainbow Table file
	private void genRTableFile() {
		try {
			String out = "Generating Rainbow Table!";
			SHA1 = MessageDigest.getInstance("SHA1");
			HexBinaryAdapter hexAdap = new HexBinaryAdapter();

			byte[] word;
			String key;
			int i = 0, success = 0, collisions = 0;

			// Map to store Digest(key) and word(value) 
			// Key = digest to avoid collisions
			HashMap<String, Integer> digestTextMap = new HashMap<String, Integer>();

			long startTime = System.currentTimeMillis();
			while (digestTextMap.size() < N_CHAIN) {
				byte[] digest = new byte[20];
				word = integerToBytes(i);

				// Generating chain
				for (int j = 0; j < L_CHAIN; j++) {
					digest = hash(word);
					word = reduce(digest, j);
				}
				key = hexAdap.marshal(word);

				// storing the final digest and word
				if (!digestTextMap.containsKey(key)) {
					digestTextMap.put(key, i);
					success++;
				} else {
					collisions++;
				}
				i++;
			}
			double T1 = (System.currentTimeMillis() - startTime) / 1000.0;

			out = "Rainbow Table generation time(T) : " + T1 + "\nN_CHAIN: "
					+ N_CHAIN + ", L_CHAIN: " + L_CHAIN + "\nSuccess: "
					+ success + " / " + N_CHAIN + "\nCollisions: " + collisions
					+ "/" + N_CHAIN;

			// Writing Rainbow Table to file
			FileWriter outputFile = new FileWriter(rTableFileName);
			Set<Entry<String, Integer>> entrySet = digestTextMap.entrySet();
			for (Entry<String, Integer> pair : entrySet) {
				outputFile.write(pair.getValue().toString() + " "
						+ pair.getKey() + "\n");
			}
			outputFile.close();
			System.out.println(out + "\nRainbow Table file: " + rTableFileName);
		} catch (Exception e) {
			System.out.println("Exception Found: " + e);
			e.printStackTrace();
		}
	}

	// Hash Function
	private byte[] hash(byte[] plaintext) {
		byte digest[] = new byte[20];
		try {
			digest = SHA1.digest(plaintext);
			SHA1.reset();
		} catch (Exception e) {
			System.out.println("Exception Encountered: " + e);
			e.printStackTrace();
		}
		return digest;
	}

	// Reduce Function
	private byte[] reduce(byte[] digest, int len) {
		byte last_byte = (byte) len;
		byte[] word = new byte[3];
		word[0] = (byte) ((digest[0] + last_byte) % 256);
		word[1] = (byte) ((digest[1]) % 256);
		word[2] = (byte) ((digest[2]) % 256);
		return word;
	}

	// Integer to Byte Conversion Functions
	private byte[] integerToBytes(int n) {
		byte plaintext[] = new byte[3];
		plaintext[0] = (byte) ((n >> 16) & 0xFF);
		plaintext[1] = (byte) ((n >> 8) & 0xFF);
		plaintext[2] = (byte) n;
		return plaintext;
	}

	public static void main(String[] args) {

		// Creating Rainbow Table
		RainbowTable Rtable = new RainbowTable();
		Rtable.genRTableFile();
		System.exit(0);
	}

}
