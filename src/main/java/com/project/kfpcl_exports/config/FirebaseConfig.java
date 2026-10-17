package com.project.kfpcl_exports.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

@Configuration
@Slf4j
public class FirebaseConfig {

    private static String lastError = null;

    public static String getLastError() {
        return lastError;
    }

    @PostConstruct
    public void initializeFirebase() {
        try {
            if (FirebaseApp.getApps().isEmpty()) {
                String envCredentials = System.getenv("FIREBASE_CREDENTIALS");
                InputStream serviceAccountStream;

                if (envCredentials != null && !envCredentials.isBlank()) {
                    log.info("[FIREBASE CONFIG] Loading Firebase credentials from environment variable FIREBASE_CREDENTIALS");
                    serviceAccountStream = new ByteArrayInputStream(envCredentials.getBytes(StandardCharsets.UTF_8));
                } else {
                    ClassPathResource resource = new ClassPathResource("serviceAccountKey.json");
                    if (resource.exists()) {
                        log.info("[FIREBASE CONFIG] Loading Firebase credentials from classpath: serviceAccountKey.json");
                        serviceAccountStream = resource.getInputStream();
                    } else {
                        lastError = "Firebase credentials not configured (neither FIREBASE_CREDENTIALS env var nor serviceAccountKey.json on classpath)";
                        log.info("[FIREBASE CONFIG] Firebase credentials not configured. Firebase Admin SDK will not be initialized on startup.");
                        return;
                    }
                }

                try (InputStream is = serviceAccountStream) {
                    byte[] bytes = is.readAllBytes();
                    GoogleCredentials credentials;
                    try {
                        credentials = GoogleCredentials.fromStream(new ByteArrayInputStream(bytes));
                    } catch (Exception e1) {
                        String jsonStr = new String(bytes, StandardCharsets.UTF_8);
                        if (jsonStr.contains("\\\\n")) {
                            jsonStr = jsonStr.replace("\\\\n", "\n");
                        } else if (jsonStr.contains("\\n") && !jsonStr.contains("\n")) {
                            jsonStr = jsonStr.replace("\\n", "\n");
                        }
                        credentials = GoogleCredentials.fromStream(new ByteArrayInputStream(jsonStr.getBytes(StandardCharsets.UTF_8)));
                    }

                    FirebaseOptions options = FirebaseOptions.builder()
                            .setCredentials(credentials)
                            .setDatabaseUrl("https://anushabazaar-2288e-default-rtdb.firebaseio.com")
                            .build();

                    FirebaseApp app = FirebaseApp.initializeApp(options);
                    lastError = null;
                    log.info("[FIREBASE CONFIG] FirebaseApp successfully initialized for project: {}", options.getProjectId() != null ? options.getProjectId() : app.getName());
                }
            }
        } catch (Exception e) {
            lastError = e.getMessage();
            log.error("[FIREBASE CONFIG ERROR] Failed to initialize FirebaseApp: {}", e.getMessage());
        }
    }
}
