package id.co.realisasi.auth;

import id.co.realisasi.domain.SumberUser;

import java.util.List;

/**
 * Pengganti AD untuk dev/test ({@code ldap.enabled=false}): username apa pun diterima dengan password
 * {@code ldap.dev-password} (default "dev"). JANGAN dipakai di production.
 */
public class DevDirectoryAuthenticator implements DirectoryAuthenticator {

    private final String devPassword;

    public DevDirectoryAuthenticator(String devPassword) {
        this.devPassword = devPassword;
    }

    @Override
    public SumberUser sumber() {
        return SumberUser.DEV;
    }

    @Override
    public DirectoryUser authenticate(String username, String password) {
        if (password == null || !password.equals(devPassword)) {
            throw new AuthException("Username atau password salah");
        }
        // Sama dengan isi direktori contoh agar login tidak mengosongkan departemen hasil sync.
        return listUsers().stream().filter(u -> u.username().equalsIgnoreCase(username)).findFirst()
                .orElse(new DirectoryUser(username, username, username + "@sarinah.net", null, null,
                        username + "@sarinah.net", null));
    }

    /** Contoh isi direktori untuk dev: beberapa user di beberapa departemen, satu tanpa departemen. */
    @Override
    public List<DirectoryUser> listUsers() {
        return List.of(
                user("agung.pribadi", "Agung Pribadi", "Technology Group", "IT Manager"),
                user("rina.wulandari", "Rina Wulandari", "Technology Group", "System Analyst"),
                user("dimas.saputra", "Dimas Saputra", "Technology Group", "Network Engineer"),
                user("sari.lestari", "Sari Lestari", "Finance & Accounting", "Accounting Officer"),
                user("budi.santoso", "Budi Santoso", "Finance & Accounting", "Budget Analyst"),
                user("maya.putri", "Maya Putri", "Retail Operation", "Store Manager"),
                user("svc.backup", "Service Backup", null, null));
    }

    private static DirectoryUser user(String username, String nama, String departemen, String jabatan) {
        return new DirectoryUser(username, nama, username + "@sarinah.net", departemen, jabatan,
                username + "@sarinah.net", null);
    }
}
