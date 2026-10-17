package com.project.kfpcl_exports.service;

import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseToken;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class FirebaseAuthService {

    public FirebaseToken verifyIdToken(String idToken) throws Exception {
        if (idToken == null || idToken.isBlank()) {
            throw new IllegalArgumentException("Firebase ID token must not be null or blank");
        }
        if (FirebaseApp.getApps().isEmpty()) {
            throw new IllegalStateException("Firebase Admin SDK is not initialized on the server. Please verify the FIREBASE_CREDENTIALS environment variable.");
        }
        return FirebaseAuth.getInstance().verifyIdToken(idToken);
    }
}
