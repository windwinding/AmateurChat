package com.coinomi.core.wallet;

import javax.annotation.Nullable;

import static com.coinomi.core.Preconditions.checkNotNull;

/**
 * @author John L. Jegutanis
 */
final public class SignedMessage {
    public enum Status {
        SignedOK, VerifiedOK, Unknown, AddressMalformed, KeyIsEncrypted, MissingPrivateKey,
        InvalidSigningAddress, InvalidMessageSignature
    }

    final String message;
    final String address;
    String signature;
    Status status = Status.Unknown;

    public SignedMessage(S