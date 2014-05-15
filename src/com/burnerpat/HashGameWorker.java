package com.burnerpat;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class HashGameWorker extends Thread {
	private final String parent;
	private final String user;
	private final Listener listener;

	public HashGameWorker(String parent, String user, Listener listener) {
		this.parent = parent;
		this.user = user;
		this.listener = listener;
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
		
		while (!isInterrupted()) {
			try {
				calculate(digest);
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}
		}
	}
	
	public void calculate(MessageDigest digest) throws UnsupportedEncodingException{
		final String seed = Long.toHexString(new HighQualityRandom().nextLong());
		
		final String line = parent + " " + user + " " + seed;
		final byte[] hash = digest.digest(line.getBytes("UTF-8"));
		
		if (hash[0] == 0 &&
			hash[1] == 0 &&
			hash[2] == 0 &&
			(hash[3] & 0xF0) == 0) {
			
			listener.notifySuccess(javax.xml.bind.DatatypeConverter.printHexBinary(hash).toLowerCase(), seed, parent);
		}
	}
	
	public interface Listener {
		public void notifySuccess(String hash, String seed, String parent);
	}
	
	
	
}
