package service;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.util.Base64;

public class AuthService {
    private static final String HASH_ADMIN = "qTT3k5tq6r56wTLec/QmvVSIGsaYMof9MltnvpJG78k=";
    private static final String HASH_TESTER = "tSd5GB9MtC/BV8pY5kMlLuAQHQDjU/17Z6Cmimd7ADU=";

    public enum Role { USER, TESTER, ADMIN }

    private Role currentRole = Role.USER;

    private final String rightsFilePath = util.AppPaths.RIGHTS_FILE;

    public void initializeRights() {
        File file = new File(rightsFilePath);
        if (!file.exists()) {
            currentRole = Role.USER;
            return;
        }

        try {
            String key = Files.readString(file.toPath(), StandardCharsets.UTF_8).trim();
            String inputHash = hash(key);

            if (HASH_ADMIN.equals(inputHash)) {
                currentRole = Role.ADMIN;
                System.out.println("System: Logged in as ADMIN");
            } else if (HASH_TESTER.equals(inputHash)) {
                currentRole = Role.TESTER;
                System.out.println("System: Logged in as TESTER");
            } else {
                currentRole = Role.USER;
            }
        } catch (Exception e) {
            currentRole = Role.USER;
        }
    }

    public Role getRole() {
        return currentRole;
    }

    public boolean isAdmin() {
        return currentRole == Role.ADMIN;
    }

    public boolean isTester() {
        return currentRole == Role.TESTER || currentRole == Role.ADMIN;
    }

    public String hash(String input) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(hash);
    }
}
