package com.project.kfpcl_exports.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

@Configuration
public class FirebaseConfig {

    @PostConstruct
    public void initializeFirebase() {
        try {
            if (FirebaseApp.getApps().isEmpty()) {
                String envCredentials = System.getenv("FIREBASE_CREDENTIALS");
                InputStream serviceAccountStream;

                if (envCredentials != null && !envCredentials.isBlank()) {
                    System.out.println("[FIREBASE CONFIG] Loading Firebase credentials from environment variable FIREBASE_CREDENTIALS");
                    serviceAccountStream = new ByteArrayInputStream(envCredentials.getBytes(StandardCharsets.UTF_8));
                } else {
                    System.out.println("[FIREBASE CONFIG] Loading Firebase credentials from classpath: serviceAccountKey.json");
                    ClassPathResource resource = new ClassPathResource("serviceAccountKey.json");
                    serviceAccountStream = resource.getInputStream();
                }

                try (InputStream is = serviceAccountStream) {
                    ObjectMapper mapper = new ObjectMapper();
                    ObjectNode node = (ObjectNode) mapper.readTree(is);
                    if (node.has("private_key")) {
                        String rawKey = node.get("private_key").asText();
                        String formattedKey = rawKey.replace("\\n", "\n");
                        node.put("private_key", formattedKey);
                    }

                    byte[] jsonBytes = mapper.writeValueAsBytes(node);
                    ByteArrayInputStream bais = new ByteArrayInputStream(jsonBytes);

                    FirebaseOptions options = FirebaseOptions.builder()
                            .setCredentials(GoogleCredentials.fromStream(bais))
                            .setDatabaseUrl("https://anushabazaar-2288e-default-rtdb.firebaseio.com")
                            .build();

                    FirebaseApp.initializeApp(options);
                    System.out.println("[FIREBASE CONFIG] FirebaseApp initialized successfully for project: " + node.path("project_id").asText());
                }
            }
        } catch (Exception e) {
            System.err.println("[FIREBASE CONFIG ERROR] Failed to initialize FirebaseApp: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
