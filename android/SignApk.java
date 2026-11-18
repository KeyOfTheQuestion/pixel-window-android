import com.android.apksig.ApkSigner;
import com.android.apksig.ApkVerifier;
import java.io.File;
import java.nio.file.Path;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.List;

class SignApk {
    public static void main(String[] args) throws Exception {
        KeyStore store = KeyStore.getInstance(Path.of(args[0]).toFile(), "android".toCharArray());
        PrivateKey key = (PrivateKey) store.getKey("dashboard", "android".toCharArray());
        X509Certificate certificate = (X509Certificate) store.getCertificate("dashboard");
        ApkSigner.SignerConfig signer = new ApkSigner.SignerConfig.Builder("dashboard", key, List.of(certificate)).build();
        File output = new File(args[2]);
        new ApkSigner.Builder(List.of(signer))
            .setInputApk(new File(args[1]))
            .setOutputApk(output)
            .setV1SigningEnabled(false)
            .setV2SigningEnabled(true)
            .setV3SigningEnabled(true)
            .build().sign();
        ApkVerifier.Result result = new ApkVerifier.Builder(output).build().verify();
        if (!result.isVerified()) {
            throw new IllegalStateException(result.getAllErrors().toString());
        }
        System.out.println("APK 签名验证通过。");
    }
}
