package id.co.realisasi.auth;

import id.co.realisasi.domain.SumberUser;

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
        return new DirectoryUser(username, username, username + "@sarinah.net", null, null,
                username + "@sarinah.net", null);
    }
}
