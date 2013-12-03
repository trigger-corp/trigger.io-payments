package io.trigger.forge.android.modules.payments;

import io.trigger.forge.android.util.Base64;
import io.trigger.forge.android.util.Base64DecoderException;

import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.HashSet;

public class Security {
	private static final SecureRandom random = new SecureRandom();

	private static HashSet<Long> knownNonces = new HashSet<Long>();

	public static long generateNonce() {
		long nonce = random.nextLong();
		knownNonces.add(nonce);
		return nonce;
	}

	public static boolean goodNonce(long nonce) {
		boolean good = knownNonces.contains(nonce);
		if (good) {
			knownNonces.remove(nonce);
		}
		return good;
	}

	public static boolean checkSignature(String publicKeyStr, String input, String signature) {
		try {
			byte[] decodedKey = Base64.decode(publicKeyStr);
			KeyFactory keyFactory = KeyFactory.getInstance("RSA");
			PublicKey publicKey = keyFactory.generatePublic(new X509EncodedKeySpec(decodedKey));
			Signature sig = Signature.getInstance("SHA1withRSA");
			sig.initVerify(publicKey);
			sig.update(input.getBytes());
			if (sig.verify(Base64.decode(signature))) {
				return true;
			}
		} catch (NoSuchAlgorithmException e) {
		} catch (InvalidKeySpecException e) {
		} catch (InvalidKeyException e) {
		} catch (SignatureException e) {
		} catch (Base64DecoderException e) {
		}
		return false;
	}

}
