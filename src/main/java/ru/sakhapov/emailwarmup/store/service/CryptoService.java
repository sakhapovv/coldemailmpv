package ru.sakhapov.emailwarmup.store.service;

public interface CryptoService {

    String encrypt(String plainText);

    String decrypt(String encryptedText);
}
