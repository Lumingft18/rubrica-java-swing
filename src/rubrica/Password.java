package rubrica;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * Le password non si salvano in chiaro: salvo sale + hash PBKDF2.
 * Formato: pbkdf2$giri$sale$hash (sale e hash in Base64).
 *
 * Una riga scritta a mano in chiaro (es. "rene;ciao") viene ancora accettata;
 * al primo login giusto la riscrivo con l'hash.
 */
public final class Password {

    private static final String PREFISSO = "pbkdf2$";
    private static final int GIRI = 65536;
    private static final int BIT = 256;

    private Password() {
    }

    public static String hash(String password) {
        byte[] sale = new byte[16];
        new SecureRandom().nextBytes(sale);
        byte[] calcolato = pbkdf2(password, sale, GIRI);
        Base64.Encoder b64 = Base64.getEncoder();
        return PREFISSO + GIRI + "$" + b64.encodeToString(sale) + "$" + b64.encodeToString(calcolato);
    }

    public static boolean inChiaro(String salvata) {
        return salvata == null || !salvata.startsWith(PREFISSO);
    }

    public static boolean verifica(String inserita, String salvata) {
        if (inserita == null || salvata == null) {
            return false;
        }
        if (inChiaro(salvata)) {
            return MessageDigest.isEqual(utf8(inserita), utf8(salvata));
        }
        String[] parti = salvata.split("\\$");
        if (parti.length != 4) {
            return false;
        }
        try {
            int giri = Integer.parseInt(parti[1]);
            byte[] sale = Base64.getDecoder().decode(parti[2]);
            byte[] atteso = Base64.getDecoder().decode(parti[3]);
            // confronto a tempo costante
            return MessageDigest.isEqual(atteso, pbkdf2(inserita, sale, giri));
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private static byte[] pbkdf2(String password, byte[] sale, int giri) {
        PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), sale, giri, BIT);
        try {
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
        } catch (GeneralSecurityException e) {
            // PBKDF2WithHmacSHA256 c'è in ogni JVM da Java 8
            throw new IllegalStateException(e);
        } finally {
            spec.clearPassword();
        }
    }

    private static byte[] utf8(String testo) {
        return testo.getBytes(StandardCharsets.UTF_8);
    }
}
