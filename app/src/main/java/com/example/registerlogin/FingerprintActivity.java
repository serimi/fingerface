package com.example.registerlogin;

import android.annotation.TargetApi;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.hardware.biometrics.BiometricPrompt;
import android.hardware.fingerprint.FingerprintManager;
import android.os.Build;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyPermanentlyInvalidatedException;
import android.security.keystore.KeyProperties;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;


public class FingerprintActivity extends AppCompatActivity {

    // create a CancellationSignal
    // variable and assign a
    // value null to it
    private CancellationSignal cancellationSignal = null;

    // create an authenticationCallback
    private BiometricPrompt.AuthenticationCallback authenticationCallback;
    private Button start_authentication;

    @TargetApi(Build.VERSION_CODES.P)
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fingerprint);

        authenticationCallback = new BiometricPrompt.AuthenticationCallback() {
            // here we need to implement two methods
            // onAuthenticationError and
            // onAuthenticationSucceeded If the
            // fingerprint is not recognized by the
            // app it will call onAuthenticationError
            // and show a toast
            @Override
            public void onAuthenticationError(
                    int errorCode, CharSequence errString)
            {
                super.onAuthenticationError(errorCode, errString);
                notifyUser("Authentication Error : " + errString);
            }
            // If the fingerprint is recognized by the
            // app then it will call
            // onAuthenticationSucceeded and show a
            // toast that Authentication has Succeed
            // Here you can also start a new activity
            // after that
            @Override
            public void onAuthenticationSucceeded(BiometricPrompt.AuthenticationResult result)
            {
                super.onAuthenticationSucceeded(result);
                notifyUser("Authentication Succeeded");
                // or start a new Activity
            }
        };

        checkBiometricSupport();
        // create a biometric dialog on Click of button

        start_authentication=findViewById(R.id.start_authentication);
        start_authentication.setOnClickListener(new View.OnClickListener() {
            @TargetApi(Build.VERSION_CODES.P)
            @Override
            public void onClick(View view)
            {
                // This creates a dialog of biometric
                // auth and it requires title , subtitle
                // , and description In our case there
                // is a cancel button by clicking it, it
                // will cancel the process of
                // fingerprint authentication
                BiometricPrompt biometricPrompt = new BiometricPrompt
                        .Builder(getApplicationContext())
                        .setTitle("Title of Prompt")
                        .setSubtitle("Subtitle")
                        .setDescription("Uses FP")
                        .setNegativeButton("Cancel", getMainExecutor(), new DialogInterface.OnClickListener() {
                            @Override
                            public void
                            onClick(DialogInterface dialogInterface, int i)
                            {
                                notifyUser("Authentication Cancelled");
                            }
                        }).build();

                // start the authenticationCallback in
                // mainExecutor
                biometricPrompt.authenticate(
                        getCancellationSignal(),
                        getMainExecutor(),
                        authenticationCallback);
            }
        });
    }

    // it will be called when
    // authentication is cancelled by
    // the user
    private CancellationSignal getCancellationSignal()
    {
        cancellationSignal = new CancellationSignal();
        cancellationSignal.setOnCancelListener(
                new CancellationSignal.OnCancelListener() {
                    @Override public void onCancel()
                    {
                        notifyUser("Authentication was Cancelled by the user");
                    }
                });
        return cancellationSignal;
    }

    // it checks whether the
    // app the app has fingerprint
    // permission
    @TargetApi(Build.VERSION_CODES.P)
    private Boolean checkBiometricSupport() {
        KeyguardManager keyguardManager = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
        if (!keyguardManager.isDeviceSecure()) {
            notifyUser("Fingerprint authentication has not been enabled in settings");
            return false;
        }
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.USE_BIOMETRIC) != PackageManager.PERMISSION_GRANTED) {
            notifyUser("Fingerprint Authentication Permission is not enabled");
            return false;
        }
        if (getPackageManager().hasSystemFeature(PackageManager.FEATURE_FINGERPRINT)) {
            return true;
        } else
            return true;

        generateKey();
        if (cipherInit()) {
            cryptoObject = new FingerprintManager.CryptoObject(cipher);
            FingerprintHandler fingerprintHandler = new FingerprintHandler(this);
            fingerprintHandler.startAutho(fingerprintManager, cryptoObject);
        }
    }

        public boolean cipherInit(){
        try {
            cipher = Cipher.getInstance(
                    KeyProperties.KEY_ALGORITHM_AES + "/"
                            + KeyProperties.BLOCK_MODE_CBC + "/"
                            + KeyProperties.ENCRYPTION_PADDING_PKCS7);
        } catch (NoSuchAlgorithmException |
                 NoSuchPaddingException e) {
            throw new RuntimeException("Failed to get Cipher", e);
        }
        try {
            keyStore.load(null);
            SecretKey key = (SecretKey) keyStore.getKey(KEY_NAME,
                    null);
            cipher.init(Cipher.ENCRYPT_MODE, key);
            return true;
        } catch (KeyPermanentlyInvalidatedException e) {
            return false;
        } catch (KeyStoreException | CertificateException
                 | UnrecoverableKeyException | IOException
                 | NoSuchAlgorithmException | InvalidKeyException e) {
            throw new RuntimeException("Failed to init Cipher", e);
        }
    }

        //Key Generator
        protected void generateKey() {
        try {
            keyStore = KeyStore.getInstance("AndroidKeyStore");
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");
        } catch (NoSuchAlgorithmException | NoSuchProviderException e) {
            throw new RuntimeException("Failed to get KeyGenerator instance", e);
        }

        try {
            keyStore.load(null);
            keyGenerator.init(new KeyGenParameterSpec.Builder(KEY_NAME,
                    KeyProperties.PURPOSE_ENCRYPT |
                            KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_CBC)
                    .setUserAuthenticationRequired(true)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_PKCS7)
                    .build());
            keyGenerator.generateKey();
        } catch (NoSuchAlgorithmException | InvalidAlgorithmParameterException | CertificateException | IOException e){
            throw new RuntimeException(e);
        }
    }


    private class

    // this is a toast method which is responsible for
    // showing toast it takes a string as parameter
    private void notifyUser(String message)
    {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}
