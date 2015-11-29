import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.HashMap;
import javax.xml.bind.annotation.adapters.HexBinaryAdapter;

public class Invert {
	private MessageDigest SHA1; 				// 160 bits
	static HexBinaryAdapter hexAdap; 			// for byte to hex conversion
	private int L_CHAIN = 300; 					// Length of chain
	static String rTableFileName = "RTABLE"; 	// Rainbow Table File Name
	private String inputFileName = "SAMPLE_INPUT.data"; // Input file name
	private String outputFileName = "Output.data"; // Output File Name

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

	// Function to invert
	private void invertFile() {
		try {
			String out = "\nInverting !";
			SHA1 = MessageDigest.getInstance("SHA1");
			hexAdap = new HexBinaryAdapter();

			int success = 0;
			byte[][] values = new byte[1000][3];

			// ----------- Reading RainbowTable and store it in Map
			BufferedReader rTableInput = new BufferedReader(new FileReader(
					rTableFileName));
			HashMap<String, Integer> digestTextMap = new HashMap<String, Integer>();
			String line;

			while ((line = rTableInput.readLine()) != null) {
				String[] result = line.split(" ");
				// key is the digest and value is word in Integer
				// written as word : digest in file hence stored in opposite way
				digestTextMap.put(result[1], Integer.parseInt(result[0]));
			}
			rTableInput.close();

			// ----------- Read Input file and save digests to array
			BufferedReader inputFile = new BufferedReader(new FileReader(
					inputFileName));
			int j = 0;
			byte[][] digests = new byte[1000][20];
			while ((line = inputFile.readLine()) != null) {

				// Not Reading space
				String hex = line.substring(2, 10) + line.substring(12, 20);
				hex += line.substring(22, 30) + line.substring(32, 40);
				hex += line.substring(42, 50);

				// Replacing space with 0 i.e. F86786 = 0F86786
				hex = hex.replaceAll("\\s", "0");
				digests[j] = hexAdap.unmarshal(hex);
				j++;
			}
			inputFile.close();

			// ----------- Find the digests from the file
			long startTime = System.currentTimeMillis();

			for (int i = 0; i < values.length; i++) {

				// read the digests read from input file
				byte[] d = digests[i];

				byte[] result = new byte[3];
				String key = "";

				// regenerate the chain for current digest d
				for (int k = L_CHAIN - 1; k >= 0; k--) {
					result = null;

					byte[] digest_to_match = d;
					byte[] val = new byte[3];

					// reduce then hash (opposite of rainbow table generation), get a digest for matching
					for (int k1 = k; k1 < L_CHAIN; k1++) {
						val = reduce(digest_to_match, k1);
						digest_to_match = hash(val);
					}
					key = hexAdap.marshal(val);

					// if the key is found i.e. digest is found, regenrate the
					// chain and get the plaintext corresponding to it
					if (digestTextMap.containsKey(key)) {
						byte[] word = integerToBytes(digestTextMap.get(key));
						byte[] digest;
						for (int l = 0; l < L_CHAIN; l++) {
							digest = hash(word);

							// break the loop when digest is found
							if (Arrays.equals(digest, d)) {
								result = word;
								break;
							}
							word = reduce(digest, l);
						}

						if (result != null) {
							break;
						}
					}
				}

				// If digest is found store the result else 0
				if (result != null) {
					success++;
					values[i] = result;
				} else {
					values[i] = integerToBytes(0);
				}
			}
			long endTime = System.currentTimeMillis();

			// ----------- Writing the values in Output File
			FileWriter outputFile = new FileWriter(outputFileName);
			outputFile.write("START\nTOTAL");
			outputFile.write(j);
			outputFile.write("\nREAD DONE\n");

			for (int i = 0; i < values.length; i++) {
				String temp = hexAdap.marshal(values[i]);

				// Instead of sending 00000, 0 is sent as required in the output
				if (temp.equals("000000")) {
					outputFile.write("     0\n");
				} else {
					outputFile.write(temp + "\n");
				}
			}
			outputFile.write("\n\nTotal number of words found: " + success
					+ "\n");
			outputFile.close();

			// ----------- Printing the Values Obtained after Inverting
			System.out
					.println(out + "\nTime taken(t): \t"
							+ ((endTime - startTime) / 1000.0)
							+ "\nAccuracy(C): \t" + (success / 1000.0 *100) + "%"
							+ "\nOutput File : " + outputFileName + "\n");

		} catch (Exception e) {
			System.out.println("Exception Encountered: " + e);
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {
		
		// Inverting
		Invert inv = new Invert();
		inv.invertFile();
		System.exit(0);
	}

}
