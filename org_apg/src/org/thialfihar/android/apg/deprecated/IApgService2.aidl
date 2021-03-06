package org.thialfihar.android.apg.deprecated;

interface IApgService2 {

    /* All functions fill the returnVals Bundle with the following keys:
     *
     * ArrayList<String> "WARNINGS"  = Warnings, if any
     * ArrayList<String> "ERRORS"    = Human readable error descriptions, if any
     * int "ERROR"                   = Numeric representation of error, if any
     *                                 starting with 100:
     *                                  100: Required argument missing
     *                                  101: Generic failure of APG
     *                                  102: No matching private key found
     *                                  103: Private key's passphrase wrong
     *                                  104: Private key's passphrase missing
     */

    /* ********************************************************
     * Encryption 
     * ********************************************************/

    /* All encryption function's arguments
     *
     * Bundle params' keys:
     *  (optional/required) 
     *      TYPE "STRING KEY" = EXPLANATION / VALUES
     *
     *  (required)
     *      String  "MESSAGE"                   = Message to encrypt
     *		OR
     *		String 	"BLOB"						= ContentUri to a file handle
     *											  with binary data to encrypt
     *											  (Attention: file will be overwritten
     *                                            with encrypted content!)
     *      
     *  (optional)
     *      int     "ENCRYPTION_ALGORYTHM"      = Encryption Algorithm
     *                                              7: AES-128, 8: AES-192, 9: AES-256, 
     *                                              4: Blowfish, 10: Twofish, 3: CAST5,
     *                                              6: DES, 2: Triple DES, 1: IDEA
     *  (optional)
     *      int     "HASH_ALGORYTHM"            = Hash Algorithm
     *                                              1: MD5, 3: RIPEMD-160, 2: SHA-1,
     *                                              11: SHA-224, 8: SHA-256, 9: SHA-384,
     *                                              10: SHA-512
     *  (optional)
     *      Boolean "ARMORED_OUTPUT"            = Armor output
     *      
     *  (optional)
     *      Boolean "FORCE_V3_SIGNATURE"        = Force V3 Signatures
     *      
     *  (optional)
     *      int     "COMPRESSION"               = Compression to use
     *                                              0x21070001: none, 1: Zip, 2: Zlib,
     *                                              3: BZip2
     *  (optional)
     *      String  "SIGNATURE_KEY"             = Key to sign with
     *      
     *  (optional)
     *      String  "PRIVATE_KEY_PASSPHRASE"    = Passphrase for signing key
     *
     * Bundle returnVals (in addition to the ERRORS/WARNINGS above):
	 *      If "MESSAGE" was set:
     *      	String  "RESULT"                    = Encrypted message
     */

     /* Additional argument for function below:
     *  (required)
     *      String  "SYMMETRIC_PASSPHRASE"      = Symmetric passphrase to use
     */
    boolean encryptWithPassphrase(in Bundle params, out Bundle returnVals);

    /* Additional argument:
     *  (required)
     *      ArrayList<String>   "PUBLIC_KEYS"   = Public keys (8char fingerprint "123ABC12" OR 
     *                                              complete id "Alice Meyer <ab@email.com>")
     */
    boolean encryptWithPublicKey(in Bundle params, out Bundle returnVals);

    /* ********************************************************
     * Decryption
     * ********************************************************/

    /* Bundle params:
     *  (required) 
     *      String  "MESSAGE"                   = Message to dencrypt
     *		OR
     *		String 	"BLOB"						= ContentUri to a file handle
     *											  with binary data to dencrypt
     *											  (Attention: file will be overwritten
     *                                            with dencrypted content!)
     *      
     *  (optional)
     *      String  "SYMMETRIC_PASSPHRASE"      = Symmetric passphrase for decryption
     *      
     *  (optional)
     *      String  "PRIVATE_KEY_PASSPHRASE"    = Private keys's passphrase on asymmetric encryption
     *
     * Bundle return_vals:
	 *      If "MESSAGE" was set:
     *          String     "RESULT"             = Decrypted message
     */
    boolean decrypt(in Bundle params, out Bundle returnVals);
 
    /* ********************************************************
     * Get key information 
     * ********************************************************/

    /* Get info about all available keys
     * 
     * Bundle params:
     *  (required)
     *      int "KEY_TYPE"                      = info about what type of keys to return
     *                                              0: public keys
     *                                              1: private keys
     *      
     *  Returns:
     *      StringArrayList "FINGERPRINTS"      = Short fingerprints of keys
     *      
     *      StringArrayList "USER_IDS"          = User ids of corresponding fingerprints
     *                                            (order is the same as in FINGERPRINTS)
     */
    boolean getKeys(in Bundle params, out Bundle returnVals);

}