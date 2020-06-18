import java.beans.Statement;
import java.io.File;
import java.io.FileNotFoundException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLSyntaxErrorException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Properties;
import java.util.Scanner;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

public class Main {

	public static void main(String[] args) {

		DatabaseClass database = new DatabaseClass();

		String file = "top1mil";
		String name = "top1mil";
		//String name = "000webhost";

		
		dictionaryAnalysis(getFileContents("words_alpha"));
		//database.insert(getFileContents(file), name);
		
		//Anatomize anatomize = new Anatomize(name);
		//anatomize.analyzeThreadManager(database.get(name));
		//anatomize.sort();
		//anatomize.results();

	}
	static void dictionaryAnalysis(ArrayList<String> inputArr) {
		int lengthCalculated[] = new int[40];
		//initialize
		for(int x=0;x<lengthCalculated.length;x++) {
			lengthCalculated[x]=0;
		}
		
		for(int x=0;x<inputArr.size();x++) {
			lengthCalculated[inputArr.get(x).length()]++;
		}
		System.out.println("Analysis on Dictionary");
		for(int x=0;x<lengthCalculated.length;x++) {
			System.out.println("There are "+lengthCalculated[x]+" "+x+" letter words ");
		}
		
		
	}

	static ArrayList<String> getFileContents(String filename) {
		File file = new File("C:\\Users\\Tuesday\\Desktop\\Password Files\\" + filename + ".txt");

		//System.out.printf("%-60s", "Getting contents of " + filename);
		Pattern pattern = Pattern.compile("'");
		ArrayList<String> temps = new ArrayList<String>();
		try {
			Scanner in = new Scanner(file).useDelimiter(",\\s*");
			while (in.hasNext()) {
				String line = in.nextLine();
				if (!line.contains("'") && !line.contains("\\")) {
					temps.add(line);
				}
			}
			in.close();

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}

		//System.out.println("done");
		// System.out.println("Password Count: " + temps.size());
		return temps;
	}



}
////////////////////////////////////////////////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////////////////////////////////////////

class AnalyzeThread implements Runnable {
	private String[] input;
	private long localTotalEntires = 0;
	private long localTotalCharacters = 0;
	private ArrayList<CharCounter> localCharArray = new ArrayList<CharCounter>();
	private ArrayList<StringCounter> localMaskArray = new ArrayList<StringCounter>();
	private ArrayList<StringCounter> localWordArray = new ArrayList<StringCounter>();
	private ArrayList<String> dictionary;
	private int symbolCount=0;
	private int lowerCaseCount=0;
	private int upperCaseCount=0;
	private int digitCount=0;
	
////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	AnalyzeThread(String input[]) {
		this.input = input;
		dictionary = new ArrayList<String>();
	}

////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	@Override
	public void run() {
		dictionary = Main.getFileContents("words_alpha");
		localTotalEntires = input.length;
		for (int x = 0; x < input.length; x++) {
			localTotalCharacters += input[x].length();
			characterCounter(input[x]);
			maskCounter(input[x]);
		}

		
		Anatomize.UpdateOtherValues(localTotalCharacters,localTotalEntires,symbolCount,lowerCaseCount,upperCaseCount,digitCount);
		
		Anatomize.UpdateCharArray(localCharArray);
		Anatomize.UpdateMaskArray(localMaskArray);
		Anatomize.UpdateWordkArray(localWordArray);
	}
////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	void characterCounter(String input) {
		for (int y = 0; y < input.length(); y++) {
			boolean found = false;
			for (int z = 0; z < localCharArray.size(); z++) {
				if (localCharArray.get(z).getCharacter() == input.charAt(y)) {
					localCharArray.get(z).increaseCounter();
					found = true;
					break;
				}
			}
			if (!found)
				localCharArray.add(new CharCounter(input.charAt(y)));
		}
	}

////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	StringBuilder getMask(String input) {
		StringBuilder maskVersion = new StringBuilder();
		for (int y = 0; y < input.length(); y++) {
			char temp = input.charAt(y);
			if (Character.isLowerCase(temp)) {
				maskVersion.append("l");
			} else if (Character.isUpperCase(temp)) {
				maskVersion.append("L");
			} else if (Character.isDigit(temp)) {
				maskVersion.append("0");
			} else {
				maskVersion.append("#");
			}
		}
		return maskVersion;
	}
////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	StringBuilder modifyMaskAndAddWord(String input, StringBuilder maskVersion) {
		// search for longest word in input
		String lowerCase = input.toLowerCase();
		String word = null;
		int wordLength = 0;
		for (int x = 0; x < dictionary.size(); x++) {
			String current = dictionary.get(x);
			if (lowerCase.contains(current) && current.length() >= wordLength) {
				word = current;
				wordLength = current.length();
			}
		}
		// if found word the change mask and add to wordArray
		if (word != null && wordLength>2) {
			int index = lowerCase.indexOf(word);
			for (int x = index; x < index + word.length(); x++) {
				if (maskVersion.charAt(x) == 'L') {
					maskVersion.setCharAt(x, 'W');
				} else if (maskVersion.charAt(x) == 'l') {
					maskVersion.setCharAt(x, 'w');
				} else {
					System.out.println("ERROR");
				}
			}
			// search for word to wordArray
			boolean wordFound = false;
			for (int x = 0; x < localWordArray.size(); x++) {
				if (localWordArray.get(x).getString().contentEquals(word)) {
					localWordArray.get(x).increaseCounter();
					wordFound = true;
					break;
				}
			}
			if (!wordFound) {
				localWordArray.add(new StringCounter(word));
			}
		}

		return maskVersion;
	}
////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	String maskCounter(String input) {
		StringBuilder maskVersion = getMask(input);

		if (maskVersion.indexOf("l") != -1) {
			lowerCaseCount++;
		}
		if (maskVersion.indexOf("L") != -1) {
			upperCaseCount++;
		}
		if (maskVersion.indexOf("0") != -1) {
			digitCount++;
		}
		if (maskVersion.indexOf("#") != -1) {
			symbolCount++;
		}
		if (maskVersion.indexOf("lll") != -1) {

		 maskVersion = modifyMaskAndAddWord(input, maskVersion);
		}
		boolean found = false;
		for (int x = 0; x < localMaskArray.size(); x++) {
			if (localMaskArray.get(x).getString().contentEquals(maskVersion.toString())) {
				localMaskArray.get(x).increaseCounter();
				found = true;
				break;
			}
		}
		if (!found)
			localMaskArray.add(new StringCounter(maskVersion.toString()));

		return maskVersion.toString();
	}

}
////////////////////////////////////////////////////////////////////////////////////////////////////////////////
class Anatomize {

	private static ArrayList<CharCounter> charArray = new ArrayList<CharCounter>();
	private static ArrayList<StringCounter> maskArray = new ArrayList<StringCounter>();
	private static ArrayList<StringCounter> wordArray = new ArrayList<StringCounter>();
	private static long totalCharacters = 0;
	private static long totalEntries = 0;
	private int THREADWORKSIZE;
	private int MAXTHREADS = 20;
	
	private static int symbolCount=0;
	private static int lowerCaseCount=0;
	private static int upperCaseCount=0;
	private static int digitCount=0;
	private String tableName;
	private static List<String> dictionary;

////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	Anatomize(String name) {
		tableName = name;
		

	}
///
	public synchronized static void UpdateOtherValues(long localTotalCharacters, long localTotalEntires,int symbolCountL,int lowerCaseCountL,int upperCaseCountL,int digitCountL) {
		symbolCount +=symbolCountL;
		lowerCaseCount +=lowerCaseCountL;
		upperCaseCount+=upperCaseCountL;
		digitCount+=digitCountL;
		totalCharacters += localTotalCharacters;
		totalEntries += localTotalEntires;
	}
////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	public synchronized static void UpdateCharArray(ArrayList<CharCounter> localCharArray) {
		// for each one of our local array
		for (int x = 0; x < localCharArray.size(); x++) {
			boolean found = false;
			for (int y = 0; y < charArray.size(); y++) {
				if (localCharArray.get(x).getCharacter() == charArray.get(y).getCharacter()) {
					charArray.get(y).increaseCounter(localCharArray.get(x).getCount());
					found = true;
					break;
				}
			}
			if (!found) {
				charArray.add(localCharArray.get(x));
			}
		}
	}

///////////////////////////////////////////////////////////////////
	public synchronized static void UpdateMaskArray(ArrayList<StringCounter> localMaskArray) {
	
		// for each one of our local array
		for (int x = 0; x < localMaskArray.size(); x++) {
			boolean found = false;
			for (int y = 0; y < maskArray.size(); y++) {
				if (localMaskArray.get(x).getString().contentEquals(maskArray.get(y).getString()) ) {//sometimes?
					maskArray.get(y).increaseCounter(localMaskArray.get(x).getCount());
					found = true;
					break;
				}
			}
			if (!found) {
				maskArray.add(localMaskArray.get(x));
			}
		}
	}

///////////////////////////////////////////////////////////////////
	public synchronized static void UpdateWordkArray(ArrayList<StringCounter> localWordArray) {
		// for each one of our local array
		for (int x = 0; x < localWordArray.size(); x++) {
			boolean found = false;
			for (int y = 0; y < wordArray.size(); y++) {
				if(localWordArray.get(x).getString()==null) {
					System.out.println("1");
				}else if(wordArray.get(y).getString()==null) {
					System.out.println("2");
				}
				if (localWordArray.get(x).getString().contentEquals(wordArray.get(y).getString()) ) {
					wordArray.get(y).increaseCounter(localWordArray.get(x).getCount());
					found = true;
					break;
				}
			}
			if (!found) {
				wordArray.add(localWordArray.get(x));
			}
		}
	}

	
	
	
	///////////////////////////////////////////////////////////////////////////////
	void analyzeThreadManager(String array[]) {
		System.out.printf("%s", "Analyzing Table \n");
		long startTime = System.currentTimeMillis();
		ThreadPoolExecutor pool = (ThreadPoolExecutor) Executors.newFixedThreadPool(MAXTHREADS);
		THREADWORKSIZE = array.length/20;

		for (int x = 0; x < array.length; x+= THREADWORKSIZE) {
			//System.out.println("x="+x);
			
			if((x+THREADWORKSIZE)>array.length) {
				//System.out.println("shorty ["+x+" "+ array.length+"]");
				String temp[]  = Arrays.copyOfRange(array, x,  array.length);
				pool.execute(new AnalyzeThread(temp));
				break;
			}
			//System.out.println("normal ["+x+" "+(x + THREADWORKSIZE)+"]");
			String temp[]  = Arrays.copyOfRange(array, x, x + THREADWORKSIZE);
			pool.execute(new AnalyzeThread(temp));
		}

		while (pool.getActiveCount() > 0) {
			System.out.printf("[%d/%d] %.2f%%\n", pool.getCompletedTaskCount(), pool.getTaskCount(),
					(((double) pool.getCompletedTaskCount() / (double) pool.getTaskCount()) * 100.0));
			try {
				Thread.sleep(10000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		pool.shutdown();
		try {
			pool.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
		} catch (InterruptedException e) {
			System.out.println(e);
		}

		System.out.printf("%59s done in %ss\n", "",((System.currentTimeMillis() - startTime) / 1000.0) );
	}

////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	void sort() {
		System.out.printf("%-60s", "Sorting data");
		long startTime = System.currentTimeMillis();
		//
		Collections.sort(charArray, new Comparator<CharCounter>() {
			@Override
			public int compare(CharCounter lhs, CharCounter rhs) {
				return lhs.getCount() > rhs.getCount() ? -1 : (lhs.getCount() < rhs.getCount()) ? 1 : 0;
			}
		});
		Collections.sort(maskArray, new Comparator<StringCounter>() {
			@Override
			public int compare(StringCounter lhs, StringCounter rhs) {
				return lhs.getCount() > rhs.getCount() ? -1 : (lhs.getCount() < rhs.getCount()) ? 1 : 0;
			}
		});
		Collections.sort(wordArray, new Comparator<StringCounter>() {
			@Override
			public int compare(StringCounter lhs, StringCounter rhs) {
				return lhs.getCount() > rhs.getCount() ? -1 : (lhs.getCount() < rhs.getCount()) ? 1 : 0;
			}
		});

		System.out.println("done in " + (System.currentTimeMillis() - startTime) / 1000.0 + "s");
	}

////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	void results() {
		
		System.out.println("========================================== ANALYTICS ==========================================");
		//System.out.println("Total Entries: " + totalEntries);
		System.out.printf("%-20s%6s\n","Database Name:",tableName);
		System.out.printf("%-20s%6d\n","Total Entries:",totalEntries);
		System.out.printf("%-20s%6d\n","Total characters:",totalCharacters);
		//System.out.println("Total characters : " + totalCharacters);
		System.out.printf("%-20s%6.2f\n","Average Length:",((double) totalCharacters / (double) totalEntries));
		//System.out.println("Avg length: " + ((double) totalCharacters / (double) totalEntries));
		
		System.out.printf("%-20s%6.2f%%\n","Contain Lowercase:",(((double)lowerCaseCount/(double)totalEntries)*100.0));
		System.out.printf("%-20s%6.2f%%\n","Contain Uppercase:",(((double)upperCaseCount/(double)totalEntries)*100.0));
		System.out.printf("%-20s%6.2f%%\n","Contain Digits:",(((double)digitCount/(double)totalEntries)*100.0));
		System.out.printf("%-20s%6.2f%%\n","Contain Symbols:",(((double)symbolCount/(double)totalEntries)*100.0));
		
		
		//System.out.println("Entries that contain lowercase : "+ );
		//System.out.println("Entries that contain uppercase : " + ((double)upperCaseCount/(double)totalEntries)*100.0);
		//System.out.println("Entries that digits : " + ((double)digitCount/(double)totalEntries)*100.0);
		//System.out.println("Entries that symbols : " + ((double)symbolCount/(double)totalEntries)*100.0);
		System.out.println("========================================== CHAR DATA ==========================================");
		for (int x = 0; x < charArray.size(); x++) {
			System.out.printf("%3d.[%c %7d] ",(x+1),charArray.get(x).getCharacter(),charArray.get(x).getCount());
			if (x % 6 == 5) {
				System.out.println();
			}
		}
		System.out.println();
		System.out.println("========================================== MASK DATA ==========================================");
		for (int x = 0; x < 100; x++) {
		
			if(x==maskArray.size()) {
				break;
			}
			System.out.printf("%3d.[%18s %6d] ",(x+1),maskArray.get(x).getString(),maskArray.get(x).getCount());
			
			if (x % 3 == 2) {
				System.out.println();
			}
			
		}
		System.out.println();
		System.out.println("========================================== WORD DATA ==========================================");
		for (int x = 0; x < 100; x++) {
			if(x==wordArray.size()) {
				break;
			}
			System.out.printf("%3d.[%10s %6d] ",(x+1),wordArray.get(x).getString(),wordArray.get(x).getCount());
	
			if (x % 4 == 3) {
				System.out.println();
			}
		}
	}
////////////////////////////////////////////////////////////////////////////////////////////////////////////////

////////////////////////////////////////////////////////////////////////////////////////////////////////////////

}

//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
class CharCounter {
	private char character;
	private int count;

	CharCounter(char character) {
		this.character = character;
		this.count = 1;
	}

	void increaseCounter() {
		count++;
	}

	void increaseCounter(int x) {
		count += x;
	}

	char getCharacter() {
		return character;
	}

	int getCount() {
		return count;
	}
}

//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
class StringCounter {
	private String input;
	private int count;

	StringCounter(String input) {
		this.input = input;
		this.count = 1;
	}

	void increaseCounter() {
		count++;
	}
	void increaseCounter(int x) {
		count += x;
	}

	String getString() {
		return input;
	}

	int getCount() {
		return count;
	}
}

//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
class DatabaseClass {

	Connection conn = null;

	DatabaseClass() {
		Properties info = new Properties();
		info.put("user", "root");
		info.put("password", "no");
		info.put("useUnicode", "true");
		info.put("useJDBCCompliantTimezoneShift", "true");
		info.put("useLegacyDatetimeCode", "false");
		info.put("serverTimezone", "UTC");
		String myUrl = "jdbc:mysql://192.168.1.18/data";
		try {
			System.out.printf("%-60s", "Connecting to Database");
			conn = DriverManager.getConnection(myUrl, info);
			System.out.println("done");
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	String[] get(String name) {
		System.out.printf("%-60s", "Retriving Table");
		long startTime = System.currentTimeMillis();
		ArrayList<String> array = new ArrayList<String>();
		try {
			java.sql.Statement stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery("Select * from " + name);
			while (rs.next()) {
				array.add(rs.getString(1));
			}

		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		String[] array2 = new String[array.size()];
		array2 = array.toArray(array2);

		System.out.println("done in " + (System.currentTimeMillis() - startTime) / 1000.0 + "s");
		return array2;

	}

	void insert(List<String> content, String name) {
		long startTime;
		int maxPasswordCount = 1000000;

		try {
			// creating table
			System.out.printf("%-60s", "Building table");
			startTime = System.currentTimeMillis();

			java.sql.Statement stmt = conn.createStatement();

			stmt.executeUpdate("DROP TABLE IF EXISTS " + name + "; ");
			stmt.executeUpdate("CREATE TABLE IF NOT EXISTS " + name + " (pass VARCHAR(255)); ");

			System.out.println("done in " + (System.currentTimeMillis() - startTime) / 1000.0 + "s");
			System.out.printf("%-60s", "Executing Update");
			////////////////////////////////////////////////////////////////////////////////////
			startTime = System.currentTimeMillis();
			StringBuilder query = new StringBuilder();
			query.append("INSERT INTO " + name + " ( pass )" + " VALUES");
			for (int x = 0; x < content.size() - 1; x++) {
				query.append("('" + content.get(x) + "'),");
				if (x % maxPasswordCount + 1 == 1) {
					query.setLength(query.length() - 1);
					query.append(";");
					try {
						stmt.executeUpdate(query.toString());
					} catch (SQLSyntaxErrorException e) {
						System.out.println("");
					}

					query.setLength(0);
					query.append("INSERT INTO " + name + " ( pass )" + " VALUES");
				}

			}
			query.append("('" + content.get(content.size() - 1) + "');");
			stmt.executeUpdate(query.toString());
			System.out.println("done in " + (System.currentTimeMillis() - startTime) / 1000.0 + "s");
			////////////////////////////////////////////////////////////////////////////////////
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

}
