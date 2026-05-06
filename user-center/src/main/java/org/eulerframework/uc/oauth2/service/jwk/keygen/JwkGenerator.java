/*
 * Copyright 2013-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.eulerframework.uc.oauth2.service.jwk.keygen;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.OctetKeyPair;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.util.Base64URL;
import org.bouncycastle.asn1.ASN1OctetString;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.SecureRandom;
import java.security.Security;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.ECGenParameterSpec;
import java.time.Instant;
import java.util.Arrays;
import java.util.Date;
import java.util.Objects;
import java.util.UUID;

/**
 * Server-side JWK generator. Produces fresh asymmetric key material entirely inside the
 * JVM process and assembles the {@link JWK} directly from the {@link KeyPair} via Nimbus
 * builders — no PEM round-trip, no intermediate {@code char[]} buffer escapes the method.
 * <p>
 * Algorithm coverage:
 * <ul>
 *   <li>RSA — {@code RS256/RS384/RS512} with {@code keySize} ∈ {2048, 3072, 4096}.</li>
 *   <li>EC — {@code ES256} (P-256), {@code ES384} (P-384), {@code ES512} (P-521).</li>
 *   <li>EdDSA — {@code Ed25519}, requires BouncyCastle (provider {@code BC}) for key-pair
 *       generation. The raw 32-byte public/private scalars are extracted from the
 *       RFC 8410 X.509 SubjectPublicKeyInfo / PKCS#8 envelopes, so this class does not
 *       depend on any BC-internal API.</li>
 * </ul>
 * The returned {@link JWK} carries {@code kid} (random UUID), {@code alg}, {@code use=sig}
 * and {@code iat = now()}; lifecycle status is intentionally not modeled here — the caller
 * (typically the admin controller) wraps the JWK into a {@code JwkEntry} and assigns
 * the initial {@code JwkStatus} via {@code JwkManageService.createKey(...)}.
 * <p>
 * The class is stateless and thread-safe; a single instance can be shared across requests.
 */
public final class JwkGenerator {

    private static final Logger log = LoggerFactory.getLogger(JwkGenerator.class);

    private static final String BC_PROVIDER_NAME = "BC";

    private final SecureRandom secureRandom;

    public JwkGenerator() {
        this(new SecureRandom());
    }

    public JwkGenerator(SecureRandom secureRandom) {
        this.secureRandom = Objects.requireNonNull(secureRandom, "secureRandom");
    }

    /**
     * Generate a fresh key pair according to {@code spec} and assemble it into a fully
     * populated {@link JWK} (private + public, with {@code kid}/{@code alg}/{@code use=sig}/{@code iat}).
     *
     * @throws IllegalStateException when the requested algorithm is not supported by the
     *                               current JCA providers (e.g. EdDSA without BouncyCastle),
     *                               or when an Ed25519 key encoding violates RFC 8410.
     */
    public JWK generate(JwkKeyGenSpec spec) {
        Objects.requireNonNull(spec, "spec");
        JwkKeyGenAlgorithm alg = spec.algorithm();
        String kid = UUID.randomUUID().toString();
        JWSAlgorithm jwsAlg = JWSAlgorithm.parse(alg.joseName());
        Date iat = Date.from(Instant.now());

        try {
            JWK jwk = switch (alg) {
                case RS256, RS384, RS512 -> buildRsa(generateRsa(spec.effectiveRsaKeySize()), kid, jwsAlg, iat);
                case ES256 -> buildEc(generateEc("secp256r1"), Curve.P_256, kid, jwsAlg, iat);
                case ES384 -> buildEc(generateEc("secp384r1"), Curve.P_384, kid, jwsAlg, iat);
                case ES512 -> buildEc(generateEc("secp521r1"), Curve.P_521, kid, jwsAlg, iat);
            };
            log.info("Generated JWK kid={} alg={} use=sig", kid, jwsAlg.getName());
            return jwk;
        }
        catch (GeneralSecurityException ex) {
            throw new IllegalStateException("Failed to generate key for alg=" + alg.joseName(), ex);
        }
    }

    // --- key-pair generation (raw JCA) ---

    private KeyPair generateRsa(int keySize) throws GeneralSecurityException {
        KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
        gen.initialize(keySize, this.secureRandom);
        return gen.generateKeyPair();
    }

    private KeyPair generateEc(String curveName) throws GeneralSecurityException {
        KeyPairGenerator gen = KeyPairGenerator.getInstance("EC");
        gen.initialize(new ECGenParameterSpec(curveName), this.secureRandom);
        return gen.generateKeyPair();
    }

    private KeyPair generateEd25519() throws GeneralSecurityException {
        if (Security.getProvider(BC_PROVIDER_NAME) == null) {
            throw new IllegalStateException(
                    "EdDSA generation requires BouncyCastle on the classpath; provider 'BC' is not registered");
        }
        KeyPairGenerator gen = KeyPairGenerator.getInstance("Ed25519", BC_PROVIDER_NAME);
        gen.initialize(255, this.secureRandom);
        return gen.generateKeyPair();
    }

    // --- KeyPair -> JWK (no PEM round-trip) ---

    private static JWK buildRsa(KeyPair kp, String kid, JWSAlgorithm jwsAlg, Date iat) {
        return new RSAKey.Builder((RSAPublicKey) kp.getPublic())
                .privateKey((RSAPrivateKey) kp.getPrivate())
                .keyID(kid)
                .keyUse(KeyUse.SIGNATURE)
                .algorithm(jwsAlg)
                .issueTime(iat)
                .build();
    }

    private static JWK buildEc(KeyPair kp, Curve curve, String kid, JWSAlgorithm jwsAlg, Date iat) {
        return new ECKey.Builder(curve, (ECPublicKey) kp.getPublic())
                .privateKey((ECPrivateKey) kp.getPrivate())
                .keyID(kid)
                .keyUse(KeyUse.SIGNATURE)
                .algorithm(jwsAlg)
                .issueTime(iat)
                .build();
    }

    /**
     * Assemble an Ed25519 {@link OctetKeyPair} from the JCA key pair without depending on
     * any BC-internal class. The 32-byte raw scalars are extracted via the standard ASN.1
     * envelopes (RFC 8410), tolerating both PKCS#8 v1 and v2 (the latter optionally
     * embedding the public key, which BouncyCastle emits by default).
     */
    private static JWK buildEd25519(KeyPair kp, String kid, JWSAlgorithm jwsAlg, Date iat) {
        byte[] rawPub = extractEd25519PublicScalar(kp.getPublic().getEncoded());
        byte[] rawPriv = extractEd25519PrivateScalar(kp.getPrivate().getEncoded());
        try {
            return new OctetKeyPair.Builder(Curve.Ed25519, Base64URL.encode(rawPub))
                    .d(Base64URL.encode(rawPriv))
                    .keyID(kid)
                    .keyUse(KeyUse.SIGNATURE)
                    .algorithm(jwsAlg)
                    .issueTime(iat)
                    .build();
        }
        finally {
            // Wipe the raw private scalar; the public bytes are not sensitive.
            Arrays.fill(rawPriv, (byte) 0);
        }
    }

    private static byte[] extractEd25519PublicScalar(byte[] x509Spki) {
        SubjectPublicKeyInfo spki = SubjectPublicKeyInfo.getInstance(x509Spki);
        byte[] raw = spki.getPublicKeyData().getOctets();
        ensureLength(raw, "Ed25519 public scalar");
        return raw;
    }

    private static byte[] extractEd25519PrivateScalar(byte[] pkcs8) {
        PrivateKeyInfo pki = PrivateKeyInfo.getInstance(pkcs8);
        try {
            // RFC 8410 §7: PrivateKey is encoded as CurvePrivateKey ::= OCTET STRING
            // which is itself wrapped in the outer OCTET STRING of PKCS#8.
            ASN1OctetString inner = ASN1OctetString.getInstance(pki.parsePrivateKey());
            byte[] raw = inner.getOctets();
            ensureLength(raw, "Ed25519 private scalar");
            return raw;
        }
        catch (IOException ex) {
            throw new IllegalStateException("Failed to parse Ed25519 PKCS#8 PrivateKeyInfo", ex);
        }
    }

    private static void ensureLength(byte[] raw, String what) {
        if (raw == null || raw.length != 32) {
            throw new IllegalStateException("Unexpected " + what + " length: "
                    + (raw == null ? "null" : String.valueOf(raw.length)) + " (expected 32)");
        }
    }
}
