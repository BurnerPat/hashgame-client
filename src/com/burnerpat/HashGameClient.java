package com.burnerpat;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class HashGameClient {
	
	private static final String SERVER_URL = "http://hash.h10a.de/";
	private static String USERNAME = "wwi11b2_ampjbw";
	private static int THREAD_COUNT = 2;
	private static String HASH = "";
	
	private static HashGameWorker[] threads = null;
	
	public static void main(String[] args) {			
		try {
			for (int i = 0; i < args.length; i += 2) {
				switch (args[i]) {
					case "-u": {
						USERNAME = args[i + 1];
						break;
					}
					case "-t": {
						try {
							THREAD_COUNT = Integer.parseInt(args[i + 1]);
						}
						catch (NumberFormatException ex) {
							System.out.println("Please provide a valid nunber of threads");
							return;
						}
						break;
					}
					default: {
						usage();
					}
				}
			}
		}
		catch (ArrayIndexOutOfBoundsException ex) {
			usage();
		}
		
		System.out.println("Username: " + USERNAME);
		
		start();
	}
	
	private static void usage() {
		System.out.println("Usage: java -jar HashGameClient.jar [-t thread_count] [-u username]\n"
						 + "\t-t thread_count: Number of worker threads to use\n"
						 + "\t-u username: The username to use when commiting\n");
		System.exit(1);
	}
	
	private static String retrieveHash() {
		List<String> hashes = new ArrayList<String>();
		List<Integer> chains = new ArrayList<Integer>();
		
		try {
			InputStream in = new URL(SERVER_URL + "?raw").openStream();
			
			while (true) {
				byte[] buffer = new byte[64];
				
				if (in.read(buffer) < 0) {
					break;
				}
				
				hashes.add(new String(buffer));
				
				in.read();
				
				int len = 0;
				int c = 0;
				while ((c = in.read()) >= 0 && (!Character.isWhitespace(c))) {
					len *= 10;
					len += (char)c - '0';
				}
				
				chains.add(len);
			}
			
			in.close();
		}
		catch (IOException ex) {
			System.err.println("Failed to request current hashes");
			ex.printStackTrace(System.err);
			return null;
		}
		
		int max = 0;
		String hash = "";
		for (int i = 0; i < hashes.size(); i++) {
			if (chains.get(i) > max) {
				max = chains.get(i);
				hash = hashes.get(i);
			}
		}
		
		return hash;
	}
	
	private static void start() {
		HASH = retrieveHash();
		
		if (HASH == null) {
			System.err.println("Failed to fetch longest hash. Shutting down.");
			System.exit(1);
			return;
		}
		
		System.out.println("Using hash: " + HASH);
		
		startWorkers(HASH);
		
		if (!UPDATER.isAlive()) {
			UPDATER.start();
		}
	}
	
	private static void startWorkers(String hash) {
		System.out.println("Starting " + THREAD_COUNT + " worker threads...");
		threads = new HashGameWorker[THREAD_COUNT];
		
		for (int i = 0; i < THREAD_COUNT; i++) {
			threads[i] = new HashGameWorker(hash, USERNAME, listener);
			threads[i].start();
		}
	}

	private static final HashGameWorker.Listener listener = new HashGameWorker.Listener() {
		@Override
		public synchronized void notifySuccess(String hash, String seed, String parent) {
			if (Thread.currentThread().isInterrupted()) {
				System.out.println("Worker found hash but another worker was faster, ignoring.");
				return;
			}
			
			System.out.println("Found hash: " + hash);
			System.out.println("Seed: " + seed);
			System.out.println("Stopping workers...");

			for (HashGameWorker thread : threads) {
				thread.interrupt();
			}
			
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				
			}
			
			try {
				InputStream in = new URL(SERVER_URL + "?Z=" + parent + "&P=" + USERNAME + "&R=" + seed).openStream();
				while (in.read() >= 0) {
					
				}
				in.close();
			}
			catch (IOException ex) {
				System.err.println("Failed to post new hash");
				ex.printStackTrace(System.err);
			}

			start();
		}
	};
	
	private static Thread UPDATER = new Thread(){
		public void run() {
			setName("Updater");
			
			try {
				while (true) {
					sleep(3000);
					
					String hash = retrieveHash();
					
					if (hash == null) {
						System.err.println("Failed to retrieve update! Shutting down.");
						System.exit(1);
						return;
					}
					
					if (hash.equals(HASH)) {
						continue;
					}
					
					System.out.println("Found update, stopping workers");
					HASH = hash;
					
					for (HashGameWorker thread : threads) {
						thread.interrupt();
					}
					
					sleep(1000);
					
					startWorkers(HASH);
				}
			}
			catch (InterruptedException ex) {
				
			}
		};
	};
	
	private static class HashGameWorker extends Thread {
		private final String parent;
		private final String user;
		private final Listener listener;
		
		private static Random random = new Random();
		private static long lastUpdate = 0;

		public HashGameWorker(String parent, String user, Listener listener) {
			this.parent = parent;
			this.user = user;
			this.listener = listener;
			
			setName("Worker-" + parent);
		}
		
		@Override
		public void run() {
			MessageDigest digest = null;
			
			try {
				digest = MessageDigest.getInstance("SHA-256");
			}
			catch (NoSuchAlgorithmException ex) {
				System.err.println("Failed to retrieve digest");
				ex.printStackTrace(System.err);
				return;
			}
			
			random = new Random(System.currentTimeMillis() + random.nextLong());
			
			while (!isInterrupted()) {
				if (lastUpdate < System.currentTimeMillis()) {
					lastUpdate = System.currentTimeMillis() + 50;
					random.setSeed(lastUpdate + random.nextLong());
				}
				
				try {
					if (calculate(digest)) {
						return;
					}
				} catch (UnsupportedEncodingException e) {
					e.printStackTrace();
				}
			}
		}
		
		public boolean calculate(MessageDigest digest) throws UnsupportedEncodingException{
			final String seed = Long.toHexString(random.nextLong());
			
			final String line = parent + " " + user + " " + seed;
			final byte[] hash = digest.digest(line.getBytes("UTF-8"));
			
			if (hash[0] == 0 &&
				hash[1] == 0 &&
				hash[2] == 0 &&
				(hash[3] & 0xF0) == 0) {
				
				listener.notifySuccess(javax.xml.bind.DatatypeConverter.printHexBinary(hash).toLowerCase(), seed, parent);
				return true;
			}
			else {
				return false;
			}
		}
		
		public interface Listener {
			public void notifySuccess(String hash, String seed, String parent);
		}
	}
}
