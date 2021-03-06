/*
 * Copyright 2018-2019 Ping Identity Corporation
 * All Rights Reserved.
 */
/*
 * Copyright (C) 2018-2019 Ping Identity Corporation
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPLv2 only)
 * or the terms of the GNU Lesser General Public License (LGPLv2.1 only)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see <http://www.gnu.org/licenses>.
 */
package com.unboundid.util;



import java.io.IOException;
import java.io.OutputStream;
import java.security.SecureRandom;
import java.security.GeneralSecurityException;
import java.util.concurrent.atomic.AtomicReference;
import javax.crypto.Cipher;
import javax.crypto.CipherOutputStream;



/**
 * This class provides an {@code OutputStream} implementation that will encrypt
 * all data written to it with a key generated from a passphrase.  Details about
 * the encryption will be encapsulated in a
 * {@link PassphraseEncryptedStreamHeader}, which will typically be written to
 * the underlying stream before any of the encrypted data, so that the
 * {@link PassphraseEncryptedInputStream} can read it to determine how to
 * decrypt that data when provided with the same passphrase.  However, it is
 * also possible to store the encryption header elsewhere and provide it to the
 * {@code PassphraseEncryptedInputStream} constructor so that that the
 * underlying stream will only include encrypted data.
 * <BR><BR>
 * The specific details of the encryption performed may change over time, but
 * the information in the header should ensure that data encrypted with
 * different settings can still be decrypted (as long as the JVM provides the
 * necessary support for that encryption).  The current implementation uses a
 * baseline of 128-bit AES/CBC/PKCS5Padding using a key generated from the
 * provided passphrase using the PBKDF2WithHmacSHA1 key factory algorithm
 * (unfortunately, PBKDF2WithHmacSHA256 isn't available on Java 7, which is
 * still a supported Java version for the LDAP SDK) with 16,384 iterations and a
 * 128-bit (16-byte) salt.  However, if the  output stream is configured to use
 * strong encryption, then it will attempt to use 256-bit AES/CBC/PKCS5Padding
 * with a PBKDF2WithHmacSHA512 key factory algorithm with 131,072 iterations and
 * a 128-bit salt.  If the JVM does not support this level of encryption, then
 * it will fall back to a key size of 128 bits and a key factory algorithm of
 * PBKDF2WithHmacSHA1.
 * <BR><BR>
 * Note that the use of strong encryption may require special configuration for
 * some versions of the JVM (for example, installation of JCE unlimited strength
 * jurisdiction policy files).  If data encrypted on one system may need to be
 * decrypted on another system, then you should make sure that all systems will
 * support the stronger encryption option before choosing to use it over the
 * baseline encryption option.
 */
@ThreadSafety(level=ThreadSafetyLevel.NOT_THREADSAFE)
public final class PassphraseEncryptedOutputStream
     extends OutputStream
{
  /**
   * An atomic reference that indicates whether the JVM supports the stronger
   * encryption settings.  It will be {@code null} until an attempt is made to
   * use stronger encryption, at which point the determination will be made and
   * a value assigned.  The cached value will be used for subsequent attempts to
   * use the strong encryption.
   */
  private static final AtomicReference<Boolean> SUPPORTS_STRONG_ENCRYPTION =
       new AtomicReference<>();



  /**
   * The length (in bytes) of the initialization vector that will be generated
   * for the cipher.
   */
  private static final int CIPHER_INITIALIZATION_VECTOR_LENGTH_BYTES = 16;



  /**
   * The length (in bits) for the encryption key to generate from the password
   * when using the baseline encryption strength.
   */
  private static final int BASELINE_KEY_FACTORY_KEY_LENGTH_BITS = 128;



  /**
   * The length (in bits) for the encryption key to generate from the password
   * when using strong encryption.
   */
  private static final int STRONG_KEY_FACTORY_KEY_LENGTH_BITS = 256;



  /**
   * The key factory iteration count that will be used when generating the
   * encryption key from the passphrase when using the baseline encryption
   * strength.
   */
  private static final int BASELINE_KEY_FACTORY_ITERATION_COUNT = 16_384;



  /**
   * The key factory iteration count that will be used when generating the
   * encryption key from the passphrase when using the strong encryption.
   */
  private static final int STRONG_KEY_FACTORY_ITERATION_COUNT = 131_072;



  /**
   * The length (in bytes) of the key factory salt that will be used when
   * generating the encryption key from the passphrase.
   */
  private static final int KEY_FACTORY_SALT_LENGTH_BYTES = 16;



  /**
   * The cipher transformation that will be used for the encryption.
   */
  private static final String CIPHER_TRANSFORMATION = "AES/CBC/PKCS5Padding";



  /**
   * The key factory algorithm that will be used when generating the encryption
   * key from the passphrase when using the baseline encryption strength.
   */
  private static final String BASELINE_KEY_FACTORY_ALGORITHM =
       "PBKDF2WithHmacSHA1";



  /**
   * The key factory algorithm that will be used when generating the encryption
   * key from the passphrase when using strong encryption.
   */
  private static final String STRONG_KEY_FACTORY_ALGORITHM =
       "PBKDF2WithHmacSHA512";



  /**
   * The algorithm that will be used when generating a MAC of the header
   * contents when using the baseline encryption strength.
   */
  private static final String BASELINE_MAC_ALGORITHM = "HmacSHA256";



  /**
   * The algorithm that will be used when generating a MAC of the header
   * contents when using strong encryption.
   */
  private static final String STRONG_MAC_ALGORITHM = "HmacSHA512";



  // The cipher output stream that will be used to actually write the
  // encrypted output.
  private final CipherOutputStream cipherOutputStream;

  // A header containing the encoded encryption details.
  private final PassphraseEncryptedStreamHeader encryptionHeader;



  /**
   * Creates a new passphrase-encrypted output stream with the provided
   * information.  It will not use a key identifier, will use the baseline
   * encryption strength rather than attempting to use strong encryption, and it
   * will write the generated {@link PassphraseEncryptedStreamHeader} to the
   * underlying stream before writing any encrypted data.
   *
   * @param  passphrase           The passphrase that will be used to generate
   *                              the encryption key.  It must not be
   *                              {@code null}.
   * @param  wrappedOutputStream  The output stream to which the encrypted data
   *                              (optionally preceded by a header with
   *                              details about the encryption) will be written.
   *                              It must not be {@code null}.
   *
   * @throws  GeneralSecurityException  If a problem is encountered while
   *                                    initializing the encryption.
   *
   * @throws  IOException  If a problem is encountered while writing the
   *                       encryption header to the underlying output stream.
   */
  public PassphraseEncryptedOutputStream(final String passphrase,
                                         final OutputStream wrappedOutputStream)
         throws GeneralSecurityException, IOException
  {
    this(passphrase.toCharArray(), wrappedOutputStream);
  }



  /**
   * Creates a new passphrase-encrypted output stream with the provided
   * information.  It will not use a key identifier, will use the baseline
   * encryption strength rather than attempting to use strong encryption, and it
   * will write the generated {@link PassphraseEncryptedStreamHeader} to the
   * underlying stream before writing any encrypted data.
   *
   * @param  passphrase           The passphrase that will be used to generate
   *                              the encryption key.  It must not be
   *                              {@code null}.
   * @param  wrappedOutputStream  The output stream to which the encrypted data
   *                              (optionally preceded by a header with
   *                              details about the encryption) will be written.
   *                              It must not be {@code null}.
   *
   * @throws  GeneralSecurityException  If a problem is encountered while
   *                                    initializing the encryption.
   *
   * @throws  IOException  If a problem is encountered while writing the
   *                       encryption header to the underlying output stream.
   */
  public PassphraseEncryptedOutputStream(final char[] passphrase,
                                         final OutputStream wrappedOutputStream)
         throws GeneralSecurityException, IOException
  {
    this(passphrase, wrappedOutputStream, null, false, true);
  }



  /**
   * Creates a new passphrase-encrypted output stream with the provided
   * information.
   *
   * @param  passphrase           The passphrase that will be used to generate
   *                              the encryption key.  It must not be
   *                              {@code null}.
   * @param  wrappedOutputStream  The output stream to which the encrypted data
   *                              (optionally preceded by a header with
   *                              details about the encryption) will be written.
   *                              It must not be {@code null}.
   * @param  keyIdentifier        An optional identifier that may be used to
   *                              associate the encryption details with
   *                              information in another system.  This is
   *                              primarily intended for use in conjunction with
   *                              UnboundID/Ping Identity products, but may be
   *                              useful in other systems.  It may be
   *                              {@code null} if no key identifier is needed.
   * @param  useStrongEncryption  Indicates whether to attempt to use strong
   *                              encryption, if it is available.  If this is
   *                              {@code true} and the JVM supports the stronger
   *                              level of encryption, then that encryption will
   *                              be used.  If this is {@code false}, or if the
   *                              JVM does not support the attempted stronger
   *                              level of encryption, then the baseline
   *                              configuration will be used.
   * @param  writeHeaderToStream  Indicates whether to write the generated
   *                              {@link PassphraseEncryptedStreamHeader} to the
   *                              provided {@code wrappedOutputStream} before
   *                              any encrypted data so that a
   *                              {@link PassphraseEncryptedInputStream} can
   *                              read it to obtain information necessary for
   *                              decrypting the data.  If this is
   *                              {@code false}, then the
   *                              {@link #getEncryptionHeader()} method must be
   *                              used to obtain the encryption header so that
   *                              it can be stored elsewhere and provided to the
   *                              {@code PassphraseEncryptedInputStream}
   *                              constructor.
   *
   * @throws  GeneralSecurityException  If a problem is encountered while
   *                                    initializing the encryption.
   *
   * @throws  IOException  If a problem is encountered while writing the
   *                       encryption header to the underlying output stream.
   */
  public PassphraseEncryptedOutputStream(final String passphrase,
                                         final OutputStream wrappedOutputStream,
                                         final String keyIdentifier,
                                         final boolean useStrongEncryption,
                                         final boolean writeHeaderToStream)
         throws GeneralSecurityException, IOException
  {
    this(passphrase.toCharArray(), wrappedOutputStream, keyIdentifier,
         useStrongEncryption, writeHeaderToStream);
  }



  /**
   * Creates a new passphrase-encrypted output stream with the provided
   * information.
   *
   * @param  passphrase           The passphrase that will be used to generate
   *                              the encryption key.  It must not be
   *                              {@code null}.
   * @param  wrappedOutputStream  The output stream to which the encrypted data
   *                              (optionally preceded by a header with
   *                              details about the encryption) will be written.
   *                              It must not be {@code null}.
   * @param  keyIdentifier        An optional identifier that may be used to
   *                              associate the encryption details with
   *                              information in another system.  This is
   *                              primarily intended for use in conjunction with
   *                              UnboundID/Ping Identity products, but may be
   *                              useful in other systems.  It may be
   *                              {@code null} if no key identifier is needed.
   * @param  useStrongEncryption  Indicates whether to attempt to use strong
   *                              encryption, if it is available.  If this is
   *                              {@code true} and the JVM supports the stronger
   *                              level of encryption, then that encryption will
   *                              be used.  If this is {@code false}, or if the
   *                              JVM does not support the attempted stronger
   *                              level of encryption, then the baseline
   *                              configuration will be used.
   * @param  writeHeaderToStream  Indicates whether to write the generated
   *                              {@link PassphraseEncryptedStreamHeader} to the
   *                              provided {@code wrappedOutputStream} before
   *                              any encrypted data so that a
   *                              {@link PassphraseEncryptedInputStream} can
   *                              read it to obtain information necessary for
   *                              decrypting the data.  If this is
   *                              {@code false}, then the
   *                              {@link #getEncryptionHeader()} method must be
   *                              used to obtain the encryption header so that
   *                              it can be stored elsewhere and provided to the
   *                              {@code PassphraseEncryptedInputStream}
   *                              constructor.
   *
   * @throws  GeneralSecurityException  If a problem is encountered while
   *                                    initializing the encryption.
   *
   * @throws  IOException  If a problem is encountered while writing the
   *                       encryption header to the underlying output stream.
   */
  public PassphraseEncryptedOutputStream(final char[] passphrase,
                                         final OutputStream wrappedOutputStream,
                                         final String keyIdentifier,
                                         final boolean useStrongEncryption,
                                         final boolean writeHeaderToStream)
         throws GeneralSecurityException, IOException
  {
    final SecureRandom random = new SecureRandom();

    final byte[] keyFactorySalt = new byte[KEY_FACTORY_SALT_LENGTH_BYTES];
    random.nextBytes(keyFactorySalt);

    final byte[] cipherInitializationVector =
         new byte[CIPHER_INITIALIZATION_VECTOR_LENGTH_BYTES];
    random.nextBytes(cipherInitializationVector);

    final int keyFactoryIterationCount;
    final String macAlgorithm;
    PassphraseEncryptedStreamHeader header = null;
    CipherOutputStream cipherStream = null;
    if (useStrongEncryption)
    {
      keyFactoryIterationCount = STRONG_KEY_FACTORY_ITERATION_COUNT;
      macAlgorithm = STRONG_MAC_ALGORITHM;

      final Boolean supportsStrongEncryption = SUPPORTS_STRONG_ENCRYPTION.get();
      if ((supportsStrongEncryption == null) ||
           Boolean.TRUE.equals(supportsStrongEncryption))
      {
        try
        {
          header = new PassphraseEncryptedStreamHeader(passphrase,
               STRONG_KEY_FACTORY_ALGORITHM, keyFactoryIterationCount,
               keyFactorySalt, STRONG_KEY_FACTORY_KEY_LENGTH_BITS,
               CIPHER_TRANSFORMATION, cipherInitializationVector,
               keyIdentifier, macAlgorithm);

          final Cipher cipher = header.createCipher(Cipher.ENCRYPT_MODE);
          if (writeHeaderToStream)
          {
            header.writeTo(wrappedOutputStream);
          }

          cipherStream = new CipherOutputStream(wrappedOutputStream, cipher);
          SUPPORTS_STRONG_ENCRYPTION.compareAndSet(null, Boolean.TRUE);
        }
        catch (final Exception e)
        {
          Debug.debugException(e);
          SUPPORTS_STRONG_ENCRYPTION.set(Boolean.FALSE);
        }
      }
    }
    else
    {
      keyFactoryIterationCount = BASELINE_KEY_FACTORY_ITERATION_COUNT;
      macAlgorithm = BASELINE_MAC_ALGORITHM;
    }

    if (cipherStream == null)
    {
      header = new PassphraseEncryptedStreamHeader(passphrase,
           BASELINE_KEY_FACTORY_ALGORITHM, keyFactoryIterationCount,
           keyFactorySalt, BASELINE_KEY_FACTORY_KEY_LENGTH_BITS,
           CIPHER_TRANSFORMATION, cipherInitializationVector, keyIdentifier,
           macAlgorithm);

      final Cipher cipher = header.createCipher(Cipher.ENCRYPT_MODE);
      if (writeHeaderToStream)
      {
        header.writeTo(wrappedOutputStream);
      }

      cipherStream = new CipherOutputStream(wrappedOutputStream, cipher);
    }

    encryptionHeader = header;
    cipherOutputStream = cipherStream;
  }



  /**
   * Writes an encrypted representation of the provided byte to the underlying
   * output stream.
   *
   * @param  b  The byte of data to be written.  Only the least significant 8
   *            bits of the value will be used, and the most significant 24 bits
   *            will be ignored.
   *
   * @throws  IOException  If a problem is encountered while encrypting the data
   *                       or writing to the underlying output stream.
   */
  @Override()
  public void write(final int b)
         throws IOException
  {
    cipherOutputStream.write(b);
  }



  /**
   * Writes an encrypted representation of the contents of the provided byte
   * array to the underlying output stream.
   *
   * @param  b  The array containing the data to be written.  It must not be
   *            {@code null}.  All bytes in the array will be written.
   *
   * @throws  IOException  If a problem is encountered while encrypting the data
   *                       or writing to the underlying output stream.
   */
  @Override()
  public void write(final byte[] b)
         throws IOException
  {
    cipherOutputStream.write(b);
  }



  /**
   * Writes an encrypted representation of the specified portion of the provided
   * byte array to the underlying output stream.
   *
   * @param  b       The array containing the data to be written.  It must not
   *                 be {@code null}.
   * @param  offset  The index in the array of the first byte to be written.
   *                 It must be greater than or equal to zero, and less than the
   *                 length of the provided array.
   * @param  length  The number of bytes to be written.  It must be greater than
   *                 or equal to zero, and the sum of the {@code offset} and
   *                 {@code length} values must be less than or equal to the
   *                 length of the provided array.
   *
   * @throws  IOException  If a problem is encountered while encrypting the data
   *                       or writing to the underlying output stream.
   */
  @Override()
  public void write(final byte[] b, final int offset, final int length)
         throws IOException
  {
    cipherOutputStream.write(b, offset, length);
  }



  /**
   * Flushes the underlying output stream so that any buffered encrypted output
   * will be written to the underlying output stream, and also flushes the
   * underlying output stream.  Note that this call may not flush any data that
   * has yet to be encrypted (for example, because the encryption uses a block
   * cipher and the associated block is not yet full).
   *
   * @throws  IOException  If a problem is encountered while flushing data to
   *                       the underlying output stream.
   */
  @Override()
  public void flush()
         throws IOException
  {
    cipherOutputStream.flush();
  }



  /**
   * Closes this output stream, along with the underlying output stream.  Any
   * remaining buffered data will be processed (including generating any
   * necessary padding) and flushed to the underlying output stream before the
   * streams are closed.
   *
   * @throws  IOException  If a problem is encountered while closing the stream.
   */
  @Override()
  public void close()
         throws IOException
  {
    cipherOutputStream.close();
  }



  /**
   * Retrieves an encryption header with details about the encryption being
   * used.  If this header was not automatically written to the beginning of the
   * underlying output stream before any encrypted data, then it must be stored
   * somewhere else so that it can be provided to the
   * {@link PassphraseEncryptedInputStream} constructor.
   *
   * @return  An encryption header with details about the encryption being used.
   */
  public PassphraseEncryptedStreamHeader getEncryptionHeader()
  {
    return encryptionHeader;
  }
}
