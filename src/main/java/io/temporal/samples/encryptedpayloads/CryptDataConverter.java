/*
 *  Copyright (c) 2020 Temporal Technologies, Inc. All Rights Reserved
 *
 *  Copyright 2012-2016 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 *  Modifications copyright (C) 2017 Uber Technologies, Inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"). You may not
 *  use this file except in compliance with the License. A copy of the License is
 *  located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 *  or in the "license" file accompanying this file. This file is distributed on
 *  an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 *  express or implied. See the License for the specific language governing
 *  permissions and limitations under the License.
 */

package io.temporal.samples.encryptedpayloads;

import com.google.common.base.Defaults;
import com.google.protobuf.ByteString;
import io.temporal.api.common.v1.Payload;
import io.temporal.api.common.v1.Payloads;
import io.temporal.common.converter.DataConverter;
import io.temporal.common.converter.DataConverterException;
import java.lang.reflect.Type;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Optional;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class CryptDataConverter implements DataConverter {
  static final String METADATA_ENCODING_KEY = "encoding";
  static final ByteString METADATA_ENCODING =
      ByteString.copyFrom("binary/encrypted", StandardCharsets.UTF_8);

  private static final String CIPHER = "AES/GCM/NoPadding";

  static final String METADATA_ENCRYPTION_CIPHER_KEY = "encryption-cipher";
  static final ByteString METADATA_ENCRYPTION_CIPHER =
      ByteString.copyFrom(CIPHER, StandardCharsets.UTF_8);

  static final String METADATA_ENCRYPTION_KEY_ID_KEY = "encryption-key-id";

  private static final int GCM_NONCE_LENGTH_BYTE = 12;
  private static final int GCM_TAG_LENGTH_BIT = 128;
  private static final Charset UTF_8 = StandardCharsets.UTF_8;

  private final DataConverter converter;

  public CryptDataConverter(DataConverter converter) {
    this.converter = converter;
  }

  private String getKeyId() {
    // Currently there is no context available to vary which key is used.
    // Use a fixed key for all payloads.
    // This still supports key rotation as the key ID is recorded on payloads allowing
    // decryption to use a previous key.

    return "test-key-test-key-test-key-test!";
  }

  private SecretKey getKey(String keyId) {
    // Key must be fetched from KMS or other secure storage.
    // Hard coded here only for example purposes.
    return new SecretKeySpec(keyId.getBytes(UTF_8), "AES");
  }

  private static byte[] getNonce(int size) {
    byte[] nonce = new byte[size];
    new SecureRandom().nextBytes(nonce);
    return nonce;
  }

  private byte[] encrypt(byte[] plainData, SecretKey key) throws Exception {
    byte[] nonce = getNonce(GCM_NONCE_LENGTH_BYTE);

    Cipher cipher = Cipher.getInstance(CIPHER);
    cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_LENGTH_BIT, nonce));

    byte[] encryptedData = cipher.doFinal(plainData);
    return ByteBuffer.allocate(nonce.length + encryptedData.length)
        .put(nonce)
        .put(encryptedData)
        .array();
  }

  private byte[] decrypt(byte[] encryptedDataWithNonce, SecretKey key) throws Exception {
    ByteBuffer buffer = ByteBuffer.wrap(encryptedDataWithNonce);

    byte[] nonce = new byte[GCM_NONCE_LENGTH_BYTE];
    buffer.get(nonce);
    byte[] encryptedData = new byte[buffer.remaining()];
    buffer.get(encryptedData);

    Cipher cipher = Cipher.getInstance(CIPHER);
    cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_LENGTH_BIT, nonce));

    return cipher.doFinal(encryptedData);
  }

  @Override
  public <T> Optional<Payload> toPayload(T value) throws DataConverterException {
    return converter.toPayload(value);
  }

  public <T> Optional<Payload> toEncryptedPayload(T value) throws DataConverterException {
    Optional<Payload> optionalPayload = converter.toPayload(value);

    if (!optionalPayload.isPresent()) {
      return optionalPayload;
    }

    Payload innerPayload = optionalPayload.get();

    String keyId = getKeyId();
    SecretKey key = getKey(keyId);

    byte[] encryptedData;
    try {
      encryptedData = encrypt(innerPayload.toByteArray(), key);
    } catch (Throwable e) {
      throw new DataConverterException(e);
    }

    Payload encryptedPayload =
        Payload.newBuilder()
            .putMetadata(METADATA_ENCODING_KEY, METADATA_ENCODING)
            .putMetadata(METADATA_ENCRYPTION_CIPHER_KEY, METADATA_ENCRYPTION_CIPHER)
            .putMetadata(METADATA_ENCRYPTION_KEY_ID_KEY, ByteString.copyFromUtf8(keyId))
            .setData(ByteString.copyFrom(encryptedData))
            .build();

    return Optional.of(encryptedPayload);
  }

  @Override
  public <T> T fromPayload(Payload payload, Class<T> valueClass, Type valueType) {
    ByteString encoding = payload.getMetadataOrDefault(METADATA_ENCODING_KEY, null);
    if (!encoding.equals(METADATA_ENCODING)) {
      return converter.fromPayload(payload, valueClass, valueType);
    }

    String keyId;
    try {
      keyId = payload.getMetadataOrThrow(METADATA_ENCRYPTION_KEY_ID_KEY).toString(UTF_8);
    } catch (Exception e) {
      throw new DataConverterException(payload, valueClass, e);
    }
    SecretKey key = getKey(keyId);

    byte[] plainData;
    Payload decryptedPayload;

    try {
      plainData = decrypt(payload.getData().toByteArray(), key);
      decryptedPayload = Payload.parseFrom(plainData);
    } catch (Throwable e) {
      throw new DataConverterException(e);
    }

    return converter.fromPayload(decryptedPayload, valueClass, valueType);
  }

  @Override
  public Optional<Payloads> toPayloads(Object... values) throws DataConverterException {
    if (values == null || values.length == 0) {
      return Optional.empty();
    }
    try {
      Payloads.Builder result = Payloads.newBuilder();
      for (Object value : values) {
        Optional<Payload> payload = toEncryptedPayload(value);
        if (payload.isPresent()) {
          result.addPayloads(payload.get());
        } else {
          result.addPayloads(Payload.getDefaultInstance());
        }
      }
      return Optional.of(result.build());
    } catch (DataConverterException e) {
      throw e;
    } catch (Throwable e) {
      throw new DataConverterException(e);
    }
  }

  @Override
  public <T> T fromPayloads(
      int index, Optional<Payloads> content, Class<T> parameterType, Type genericParameterType)
      throws DataConverterException {
    if (!content.isPresent()) {
      return (T) Defaults.defaultValue((Class<?>) parameterType);
    }
    int count = content.get().getPayloadsCount();
    // To make adding arguments a backwards compatible change
    if (index >= count) {
      return (T) Defaults.defaultValue((Class<?>) parameterType);
    }
    return fromPayload(content.get().getPayloads(index), parameterType, genericParameterType);
  }
}
