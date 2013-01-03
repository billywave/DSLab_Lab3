package security;

import java.io.*;
import java.security.*;
import java.util.Arrays;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import org.apache.log4j.Logger;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.EncryptionException;
import org.bouncycastle.openssl.PEMReader;
import org.bouncycastle.openssl.PasswordFinder; 

public class RSAChannel implements Channel {
	private Logger logger = Logger.getLogger(this.getClass());
	
	private final Base64Channel channel;
	
	private boolean encryptedRead = true;
	
	private String user;
	
	private Cipher cipher;
	private PrivateKey privateKey;
	private PublicKey publicKey;
	private PublicKey remotePublicKey;
	
	public RSAChannel(Base64Channel channel) {
		this.channel = channel;
		
		// Initializing cipher for RSA
		try {
			cipher = Cipher.getInstance("RSA/NONE/OAEPWithSHA256AndMGF1Padding");
		} catch (NoSuchAlgorithmException ex) {
			ex.printStackTrace();
		} catch (NoSuchPaddingException ex) {
			ex.printStackTrace();
		}
	}
	
	protected void encryptedRead(boolean encrypted) {
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
		return (privateKey != null & publicKey != null);
	}
	
	public boolean loadRemoteUserPublicKey(String user) {
		remotePublicKey = readPublicKey(user);
		return (remotePublicKey != null);
	}
	
	private byte[] encrypt(String message) {
		if (message == null) return null;
		byte[] encryptedMessage = null;
		try {
			cipher.init(Cipher.ENCRYPT_MODE, remotePublicKey);
			try {
				encryptedMessage = cipher.doFinal(message.getBytes());
			} catch (IllegalBlockSizeException ex) {
				ex.printStackTrace();
			} catch (BadPaddingException ex) {
				ex.printStackTrace();
			}
		} catch (InvalidKeyException ex) {
			//ex.printStackTrace();
			logger.error("RSA Encryption: Key didn't match");
		}
		return encryptedMessage;
	}
	
	private String decrypt(byte[] message) {
		if (message == null) return null;
		String decryptedMessage = null;
		try {
			cipher.init(Cipher.DECRYPT_MODE, privateKey);
			try {
				decryptedMessage = new String(cipher.doFinal(message));
			} catch (IllegalBlockSizeException ex) {
				ex.printStackTrace();
			} catch (BadPaddingException ex) {
				ex.printStackTrace();
			}
		} catch (InvalidKeyException ex) {
			logger.error("RSA decryption: Key didn't match");
		}
		return decryptedMessage;
	}
	

	@Override
	public String readLine() throws IOException {
		byte[] line = channel.readBytes();
		if (line == null) return null;
	
		// pass unencrypted messages through the layer
		if (!encryptedRead) {
			logger.debug("RSA not encrypted read");
			return new String(line);
		}
		
		// pass !list command undecrypted
		if (Arrays.equals(line, "!list".getBytes())) return "!list";
		
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
		logger.debug("Previouse message sent via RSA");
		channel.printBytes(encrypt(line));
	}
	
	/**
	 * Reads the private key of the given user
	 */
	private PrivateKey readPrivateKey(final String user, final String password) {
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
		} catch (IOException ex) {
			logger.error("Two");
			ex.printStackTrace();
		}
		return null;
	}
	
	/**
	 * Reads the public key of a given user
	 * @param user 
	 */
	private PublicKey readPublicKey(String user) {
		try {
			PEMReader in = new PEMReader(new FileReader("keys/" + user + ".pub.pem"));
			return (PublicKey) in.readObject();
		} catch (FileNotFoundException e) {
			logger.error("Public Key File Not Found");
		} catch (IOException ex) {
			ex.printStackTrace();
		}
		return null;
	}
}
