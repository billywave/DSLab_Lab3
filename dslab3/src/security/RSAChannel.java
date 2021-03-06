package security;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Arrays;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import org.apache.log4j.Logger;
import org.bouncycastle.openssl.EncryptionException;
import org.bouncycastle.openssl.PEMReader;
import org.bouncycastle.openssl.PasswordFinder;

public class RSAChannel implements Channel {
	private static Logger logger = Logger.getLogger(RSAChannel.class);
	
	private final Base64Channel channel;
	
	private boolean encryptedRead = true;
	
	private String user;
	
	private Cipher cipher;
	private static PrivateKey privateKey;
	private PublicKey publicKey;
	private PublicKey remotePublicKey;
	
	public RSAChannel(Base64Channel channel) {
		this.channel = channel;
		
		// Initializing cipher for RSA
		try {
			cipher = Cipher.getInstance("RSA/NONE/OAEPWithSHA256AndMGF1Padding");
		} catch (NoSuchAlgorithmException ex) {
			logger.error("RSA-Cipher: No such algorithm");
		} catch (NoSuchPaddingException ex) {
			logger.error("RSA-Cipher: No such padding");
		}
	}
	
	protected void setEncryptedRead(boolean encrypted) {
		this.encryptedRead = encrypted;
		channel.setEncodedRead(encrypted);
	}
	
	/**
	 * Loads the rsa key to the given user and stores them as the members
	 * @param user 
	 */
	public boolean loadUserKeys(String user, String password) {
		this.user = user;
		privateKey = readPrivateKey(user,password);
		publicKey = readPublicKey(user);
		return (privateKey != null && publicKey != null);
	}
	
	public boolean loadRemoteUserPublicKey(String user) {
		remotePublicKey = readPublicKey(user);
		return (remotePublicKey != null);
	}
	
	private byte[] encrypt(byte[] message) {
		if (message == null) return null;
		try {
			cipher.init(Cipher.ENCRYPT_MODE, remotePublicKey);
			try {
				return cipher.doFinal(message);
			} catch (IllegalBlockSizeException ex) {
				logger.error("RSA encryption failed");
			} catch (BadPaddingException ex) {
				logger.error("RSA encryption failed");
			}
		} catch (InvalidKeyException ex) {
			logger.error("RSA encryption failed: Key didn't match");
		}
		return null;
	}
	
	private byte[] decrypt(byte[] message) {
		if (message == null) return null;
		try {
			cipher.init(Cipher.DECRYPT_MODE, privateKey);
			try {
				return cipher.doFinal(message);
			} catch (IllegalBlockSizeException ex) {
				logger.error("RSA decryption failed");
			} catch (BadPaddingException ex) {
				logger.error("RSA decryption failed");
			}
		} catch (InvalidKeyException ex) {
			logger.error("RSA decryption: Key didn't match");
		}
		return null;
	}
	

	@Override
	public String readLine() throws IOException {
		return new String(this.readBytes());
	}
	
	//@Override
	public byte[] readBytes() throws IOException {
		byte[] line = channel.readBytes();
		if (line == null) return null;
	
		// pass unencrypted messages through the layer
		if (!encryptedRead) {
			logger.debug("RSA not encrypted read");
			return line;
		}
		
		// pass !list command undecrypted
		if (Arrays.equals(line, "!list".getBytes())) return line;
		
		logger.debug("Next message received via RSA");
		return decrypt(line);
	}

	@Override
	public void close() {
		channel.close();
	}

	@Override
	public void flush() {
		channel.flush();
	}

	@Override
	public void println(String line) {
		this.printBytes(line.getBytes());
	}
	
	//@Override
	public void printBytes(byte[] line) {
		logger.debug("Previous message sent via RSA");
		channel.printBytes(encrypt(line));
	}
	
	/**
	 * Reads the private key of the given user
	 */
	public static PrivateKey readPrivateKey(final String user, final String password) {
		try {
			PEMReader in = new PEMReader(new FileReader("keys/" + user + ".pem"), new PasswordFinder() {
				@Override
				public char[] getPassword() {
					return password.toCharArray();
				}
			});
			
			KeyPair keyPair = (KeyPair) in.readObject();
			return keyPair.getPrivate(); 
			
		} catch (FileNotFoundException e) {
			logger.error("Private Key File Not Found");
		} catch (EncryptionException e) {
			logger.error("Decryption failed, check password");
		} catch (IOException ex) {}
		return null;
	}
	
	/**
	 * Reads the public key of a given user
	 * @param user 
	 */
	public static PublicKey readPublicKey(String user) {
		try {
			PEMReader in = new PEMReader(new FileReader("keys/" + user + ".pub.pem"));
			return (PublicKey) in.readObject();
		} catch (FileNotFoundException e) {
			logger.error("Public Key File Not Found");
		} catch (IOException ex) {}
		return null;
	}
	
	public static PrivateKey getPrivateKey() {
		return privateKey;
	}
}
