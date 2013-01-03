package security;

import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import org.apache.log4j.Logger;
import javax.crypto.spec.SecretKeySpec;
import org.bouncycastle.util.encoders.Base64;

/**
 *
 * @author Bernhard
 */
public class AESChannel implements Channel {
	private Logger logger = Logger.getLogger(this.getClass());
	
	private final Base64Channel channel;
	
	private boolean encryptedRead = true;
	
	private Cipher cipher;
	private SecretKeySpec secretKey;
	private IvParameterSpec iv;
	
	public String generateBase64SecretKey() {
		try {
			KeyGenerator generator = KeyGenerator.getInstance("AES");
			// KEYSIZE is in bits
			generator.init(256);
			 SecretKey key = generator.generateKey(); 
			 return new String(Base64.encode(key.getEncoded()));
		} catch (NoSuchAlgorithmException ex) {
			ex.printStackTrace();
			return null;
		}
	}
	
	public void setSecretKey(String key) {
		secretKey =  new SecretKeySpec(Base64.decode(key.getBytes()), "AES");
		//new SecretKey(keySpec);
	}
	
	public String generateBase64IV() {
		// generates a 32 byte secure random number
		SecureRandom secureRandom = new SecureRandom();
		final byte[] number = new byte[16];
		secureRandom.nextBytes(number);
		logger.debug("IV byte[]: " + number);
		String ivString = new String(Base64.encode(number));
		logger.debug("Local authentication challenge in base64: " + ivString);
		return ivString;
	}
	
	public void setIV(String ivString) {
		iv = new IvParameterSpec(Base64.decode(ivString.getBytes()));
		logger.debug("IV getIV: " + new String(Base64.encode(iv.getIV())));
	}

	public AESChannel(Base64Channel channel) {
		this.channel = channel;
		try {
			cipher = Cipher.getInstance("AES/CTR/NoPadding");
		} catch (NoSuchAlgorithmException ex) {
			ex.printStackTrace();
		} catch (NoSuchPaddingException ex) {
			ex.printStackTrace();
		}
	}
	
	protected void encryptedRead(boolean encrypted) {
		this.encryptedRead = encrypted;
		channel.encodedRead(encrypted);
	}
	
	private byte[] encrypt(String message) {
		if (message == null) return null;
		byte[] encryptedMessage = null;
		try {
			cipher.init(Cipher.ENCRYPT_MODE, secretKey, iv);
			try {
				encryptedMessage = cipher.doFinal(message.getBytes());
			} catch (IllegalBlockSizeException ex) {
				ex.printStackTrace();
			} catch (BadPaddingException ex) {
				ex.printStackTrace();
			}
		} catch (InvalidAlgorithmParameterException ex) {
			ex.printStackTrace();
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
			cipher.init(Cipher.DECRYPT_MODE, secretKey, iv);
			try {
				decryptedMessage = new String(cipher.doFinal(message));
			} catch (IllegalBlockSizeException ex) {
				ex.printStackTrace();
			} catch (BadPaddingException ex) {
				ex.printStackTrace();
			}
		} catch (InvalidAlgorithmParameterException ex) {
			ex.printStackTrace();
		} catch (InvalidKeyException ex) {
			logger.error("RSA decryption: Key didn't match");
		}
		return decryptedMessage;
	}

	@Override
	public String readLine() throws IOException {
		byte[] line = channel.readBytes();
		if (!encryptedRead) {
			logger.debug("AES not encrypted read");
			return new String(line);
		}
		if (line == null) {
			return null;
		}
		logger.debug("Next message received via AES");
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
		logger.debug("Previous message sent via AES");
		channel.printBytes(encrypt(line));
	}
	
	@Override
	public void appendToInputStream(String line) {
		channel.appendToInputStream(encrypt(line));
	}
	
}
