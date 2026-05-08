/*
 * Copyright 2013-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 */
package org.eulerframework.uc.oauth2.service.jwk.keygen;

import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jose.crypto.ECDSAVerifier;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSObject;
import com.nimbusds.jose.Payload;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.OctetKeyPair;
import com.nimbusds.jose.jwk.RSAKey;
import org.bouncycastle.asn1.DEROctetString;
import org.bouncycastle.asn1.edec.EdECObjectIdentifiers;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Security;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwkGeneratorTests {

    @BeforeAll
    static void registerBouncyCastle() {
        if (Security.getProvider("BC") == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    private final JwkGenerator generator = new JwkGenerator();

    @Test
    void generatesEs256ByDefaultShape() {
        JWK jwk = generator.generate(new JwkKeyGenSpec(JwkKeyGenAlgorithm.ES256, null));

        assertThat(jwk.isPrivate()).isTrue();
        assertThat(UUID.fromString(jwk.getKeyID())).isNotNull();
        assertThat(jwk).isInstanceOf(ECKey.class);
        assertThat(((ECKey) jwk).getCurve()).isEqualTo(Curve.P_256);
        assertThat(jwk.getAlgorithm().getName()).isEqualTo("ES256");
        assertThat(jwk.getKeyUse().identifier()).isEqualTo("sig");
        assertThat(jwk.getIssueTime()).isNotNull();
    }

    @Test
    void generatesUniqueKidsAcrossCalls() {
        JWK a = generator.generate(new JwkKeyGenSpec(JwkKeyGenAlgorithm.ES256, null));
        JWK b = generator.generate(new JwkKeyGenSpec(JwkKeyGenAlgorithm.ES256, null));
        assertThat(a.getKeyID()).isNotEqualTo(b.getKeyID());
    }

    @Test
    void generatesRsaWithDefaultKeySize() {
        JWK jwk = generator.generate(new JwkKeyGenSpec(JwkKeyGenAlgorithm.RS256, null));

        assertThat(jwk).isInstanceOf(RSAKey.class);
        assertThat(((RSAKey) jwk).size()).isEqualTo(JwkKeyGenSpec.DEFAULT_RSA_KEY_SIZE);
        assertThat(jwk.getAlgorithm().getName()).isEqualTo("RS256");
    }

    @Test
    void generatesRsaWithExplicitKeySize() {
        JWK jwk = generator.generate(new JwkKeyGenSpec(JwkKeyGenAlgorithm.RS384, 3072));

        assertThat(((RSAKey) jwk).size()).isEqualTo(3072);
        assertThat(jwk.getAlgorithm().getName()).isEqualTo("RS384");
    }

    @Test
    void generatesEs384AndEs512() {
        JWK g384 = generator.generate(new JwkKeyGenSpec(JwkKeyGenAlgorithm.ES384, null));
        assertThat(((ECKey) g384).getCurve()).isEqualTo(Curve.P_384);

        JWK g512 = generator.generate(new JwkKeyGenSpec(JwkKeyGenAlgorithm.ES512, null));
        assertThat(((ECKey) g512).getCurve()).isEqualTo(Curve.P_521);
    }

    // --- end-to-end sign + verify proves the JWK is materially complete (no PEM round-trip needed) ---

    @Test
    void rsaSignedJwsVerifiesWithPublicProjection() throws Exception {
        RSAKey rsa = (RSAKey) generator.generate(new JwkKeyGenSpec(JwkKeyGenAlgorithm.RS256, null));
        JWSObject jws = new JWSObject(new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(rsa.getKeyID()).build(),
                new Payload("hello-rsa"));
        jws.sign(new RSASSASigner(rsa.toRSAPrivateKey()));
        String compact = jws.serialize();

        JWSObject parsed = JWSObject.parse(compact);
        assertThat(parsed.verify(new RSASSAVerifier(rsa.toPublicJWK().toRSAPublicKey()))).isTrue();
    }

    @Test
    void ecSignedJwsVerifiesWithPublicProjection() throws Exception {
        ECKey ec = (ECKey) generator.generate(new JwkKeyGenSpec(JwkKeyGenAlgorithm.ES256, null));
        JWSObject jws = new JWSObject(new JWSHeader.Builder(JWSAlgorithm.ES256).keyID(ec.getKeyID()).build(),
                new Payload("hello-ec"));
        jws.sign(new ECDSASigner(ec.toECPrivateKey()));
        String compact = jws.serialize();

        JWSObject parsed = JWSObject.parse(compact);
        assertThat(parsed.verify(new ECDSAVerifier(ec.toPublicJWK().toECPublicKey()))).isTrue();
    }

    @Test
    void rejectsRsaKeySizeBelowFloor() {
        // JwkKeyGenSpec validates key size in constructor.
        assertThatThrownBy(() -> new JwkKeyGenSpec(JwkKeyGenAlgorithm.RS256, 1024))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
