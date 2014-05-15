package com.burnerpat;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Random;

public class HashGameWorker extends Thread {
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
