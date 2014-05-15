package com.burnerpat;
import java.io.UnsupportedEncodingException;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
//import java.util.Random;
//import org.uncommons.maths.random.AESCounterRNG;
//import org.uncommons.maths.random.DefaultSeedGenerator;
//import org.uncommons.maths.random.SeedException;

public class HashGameWorker extends Thread {
	private final String parent;
	private final String user;
	private final Listener listener;
//	private static Random random = new Random();
//	private static AESCounterRNG random;
	
	private static long lastUpdate = 0;
	
	public HashGameWorker(String parent, String user, Listener listener) {
		this.parent = parent;
		this.user = user;
		this.listener = listener;
/*		try {
			random = new AESCounterRNG(DefaultSeedGenerator.getInstance());
		} catch (GeneralSecurityException e) {
			e.printStackTrace();
		} catch (SeedException e) {
			e.printStackTrace();
		}
*/	
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
/*			if (lastUpdate < System.currentTimeMillis()) {
				lastUpdate = System.currentTimeMillis() + 100;
				random.setSeed((lastUpdate));
			}
			*/
			try {
				calculate(digest);
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}
		}
	}
	
	public void calculate(MessageDigest digest) throws UnsupportedEncodingException{
//		final String seed = Long.toHexString(random.nextLong());
		final String seed = Long.toHexString(new HighQualityRandom().nextLong());
		
		final String line = parent + " " + user + " " + seed;
		final byte[] hash = digest.digest(line.getBytes("UTF-8"));
		
//		System.out.println(new Date().getTime());
//		System.out.println(seed);
		
		if (hash[0] == 0 &&
			hash[1] == 0 &&
			hash[2] == 0 &&
			(hash[3] & 0xF0) == 0) {
			
			new Thread() {
				public void run() {
					listener.notifySuccess(javax.xml.bind.DatatypeConverter.printHexBinary(hash).toLowerCase(), seed, parent);
				}
			}.start();
		}
	}
	
	public interface Listener {
		public void notifySuccess(String hash, String seed, String parent);
	}
	
	
	
}
