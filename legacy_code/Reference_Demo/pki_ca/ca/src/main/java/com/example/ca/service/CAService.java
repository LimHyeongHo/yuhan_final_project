package com.example.ca.service;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.math.BigInteger;
import java.security.*;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class CAService {
    private KeyPair rootKeyPair;
    private X509Certificate rootCertificate;
    private final AtomicLong serialNumberGenerator = new AtomicLong(System.currentTimeMillis());

    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    @PostConstruct
    public void init() throws Exception {
        // Root CA 키 쌍 생성
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA", "BC");
        keyPairGenerator.initialize(2048);
        this.rootKeyPair = keyPairGenerator.generateKeyPair();

        // Root CA 자체 서명 인증서 생성
        long now = System.currentTimeMillis();
        Date startDate = new Date(now);
        Date endDate = new Date(now + 3650L * 24 * 60 * 60 * 1000); // 10년 유효

        X500Name dnName = new X500Name("CN=MyInternalRootCA, O=MyCompany, C=KR");
        BigInteger certSerialNumber = BigInteger.valueOf(serialNumberGenerator.getAndIncrement());

        X509v3CertificateBuilder certBuilder = new JcaX509v3CertificateBuilder(
                dnName, certSerialNumber, startDate, endDate, dnName, rootKeyPair.getPublic());

        ContentSigner contentSigner = new JcaContentSignerBuilder("SHA256WithRSAEncryption")
                .setProvider("BC").build(rootKeyPair.getPrivate());

        X509CertificateHolder certHolder = certBuilder.build(contentSigner);
        this.rootCertificate = new JcaX509CertificateConverter().setProvider("BC").getCertificate(certHolder);
        
        System.out.println("Root CA initialized successfully.");
    }

    public X509Certificate issueDeviceCertificate(PublicKey devicePublicKey, String deviceId) throws Exception {
        long now = System.currentTimeMillis();
        Date startDate = new Date(now);
        Date endDate = new Date(now + 365L * 24 * 60 * 60 * 1000); // 1년 유효

        X500Name subject = new X500Name("CN=" + deviceId + ", O=MyCompany, C=KR");
        BigInteger certSerialNumber = BigInteger.valueOf(serialNumberGenerator.getAndIncrement());

        X509v3CertificateBuilder certBuilder = new JcaX509v3CertificateBuilder(
                new X500Name(rootCertificate.getSubjectX500Principal().getName()),
                certSerialNumber, startDate, endDate, subject, devicePublicKey);

        ContentSigner contentSigner = new JcaContentSignerBuilder("SHA256WithRSAEncryption")
                .setProvider("BC").build(rootKeyPair.getPrivate());

        X509CertificateHolder certHolder = certBuilder.build(contentSigner);
        return new JcaX509CertificateConverter().setProvider("BC").getCertificate(certHolder);
    }

    public X509Certificate getRootCertificate() {
        return rootCertificate;
    }

    private final java.util.Set<BigInteger> revokedSerialNumbers = java.util.concurrent.ConcurrentHashMap.newKeySet();

    public void revokeCertificate(BigInteger serialNumber) {
        revokedSerialNumbers.add(serialNumber);
    }

    public boolean isRevoked(BigInteger serialNumber) {
        return revokedSerialNumbers.contains(serialNumber);
    }
}
