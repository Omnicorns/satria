package id.co.realisasi.auth;

import id.co.realisasi.domain.SumberUser;
import lombok.extern.slf4j.Slf4j;

import javax.naming.AuthenticationException;
import javax.naming.CommunicationException;
import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.PartialResultException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.naming.ldap.Control;
import javax.naming.ldap.InitialLdapContext;
import javax.naming.ldap.LdapContext;
import javax.naming.ldap.PagedResultsControl;
import javax.naming.ldap.PagedResultsResponseControl;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

/**
 * Login ke Microsoft Active Directory: bind sebagai {@code username@domain} memakai password user
 * (password tidak pernah disimpan), lalu dengan koneksi yang sama membaca atribut user
 * (displayName, mail, department, title) berdasarkan sAMAccountName.
 */
@Slf4j
public class LdapDirectoryAuthenticator implements DirectoryAuthenticator {

    private static final String[] ATTRS = {"sAMAccountName", "displayName", "cn", "mail", "department", "title",
            "userPrincipalName", "distinguishedName"};

    private final LdapProperties props;

    public LdapDirectoryAuthenticator(LdapProperties props) {
        this.props = props;
    }

    @Override
    public SumberUser sumber() {
        return SumberUser.LDAP;
    }

    @Override
    public DirectoryUser authenticate(String username, String password) {
        // AD menerima bind tanpa password sebagai "unauthenticated bind" (sukses!), jadi wajib ditolak di sini.
        if (password == null || password.isEmpty()) {
            throw new AuthException("Username atau password salah");
        }
        String principal = username.contains("@") ? username : username + "@" + props.domain();
        DirContext ctx = null;
        try {
            ctx = new InitialDirContext(env(principal, password));
            DirectoryUser found = find(ctx, username);
            return found != null ? found : new DirectoryUser(username, username, null, null, null, principal, null);
        } catch (AuthenticationException e) {
            // Kode 49: password salah, akun terkunci/nonaktif/kedaluwarsa (lihat data 52e/775/533/532 di pesan)
            log.info("Login LDAP ditolak untuk {}: {}", username, e.getMessage());
            throw new AuthException("Username atau password salah");
        } catch (CommunicationException e) {
            log.error("Server LDAP {} tidak dapat dihubungi", props.url(), e);
            throw new DirectoryUnavailableException("Server LDAP tidak dapat dihubungi", e);
        } catch (NamingException e) {
            log.error("Error LDAP saat login {}", username, e);
            throw new DirectoryUnavailableException("Gagal membaca data user dari LDAP", e);
        } finally {
            if (ctx != null) {
                try {
                    ctx.close();
                } catch (NamingException ignored) {
                    // koneksi sudah ditutup
                }
            }
        }
    }

    private Hashtable<String, String> env(String principal, String password) {
        Hashtable<String, String> env = new Hashtable<>();
        env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
        env.put(Context.PROVIDER_URL, props.url());
        env.put(Context.SECURITY_AUTHENTICATION, "simple");
        env.put(Context.SECURITY_PRINCIPAL, principal);
        env.put(Context.SECURITY_CREDENTIALS, password);
        // Pencarian dari root domain AD menghasilkan referral ke partisi lain; abaikan.
        env.put(Context.REFERRAL, "ignore");
        env.put("com.sun.jndi.ldap.connect.timeout", String.valueOf(timeout(props.connectTimeoutMs(), 5000)));
        env.put("com.sun.jndi.ldap.read.timeout", String.valueOf(timeout(props.readTimeoutMs(), 10000)));
        return env;
    }

    private DirectoryUser find(DirContext ctx, String username) throws NamingException {
        String sam = username.contains("@") ? username.substring(0, username.indexOf('@')) : username;
        SearchControls sc = new SearchControls();
        sc.setSearchScope(SearchControls.SUBTREE_SCOPE);
        sc.setReturningAttributes(ATTRS);
        String filter = "(&(objectCategory=person)(objectClass=user)(sAMAccountName=" + escape(sam) + "))";
        NamingEnumeration<SearchResult> results = ctx.search(props.searchBase(), filter, sc);
        try {
            if (!results.hasMore()) {
                return null;
            }
            return toUser(results.next().getAttributes(), sam);
        } catch (PartialResultException e) {
            return null; // hanya referral yang tersisa
        } finally {
            results.close();
        }
    }

    /** User AD yang aktif (bukan disabled) dan punya sAMAccountName. */
    private static final String ALL_USERS_FILTER = "(&(objectCategory=person)(objectClass=user)(sAMAccountName=*)"
            + "(!(userAccountControl:1.2.840.113556.1.4.803:=2)))";
    private static final int PAGE_SIZE = 500;

    /**
     * Baca semua user aktif memakai service account. Pakai paged results karena AD membatasi
     * satu pencarian maksimal 1000 entri.
     */
    @Override
    public List<DirectoryUser> listUsers() {
        if (props.managerDn() == null || props.managerDn().isBlank()
                || props.managerPassword() == null || props.managerPassword().isEmpty()) {
            throw new DirectoryUnavailableException(
                    "Service account LDAP belum diatur (ldap.manager-dn dan env LDAP_BIND_PASSWORD)", null);
        }
        String principal = props.managerDn().contains("@") || props.managerDn().contains("=")
                ? props.managerDn() : props.managerDn() + "@" + props.domain();
        LdapContext ctx = null;
        try {
            ctx = new InitialLdapContext(env(principal, props.managerPassword()), null);
            SearchControls sc = new SearchControls();
            sc.setSearchScope(SearchControls.SUBTREE_SCOPE);
            sc.setReturningAttributes(ATTRS);
            List<DirectoryUser> users = new ArrayList<>();
            byte[] cookie = null;
            do {
                ctx.setRequestControls(new Control[]{new PagedResultsControl(PAGE_SIZE, cookie, Control.CRITICAL)});
                NamingEnumeration<SearchResult> results = ctx.search(props.searchBase(), ALL_USERS_FILTER, sc);
                try {
                    while (results.hasMore()) {
                        users.add(toUser(results.next().getAttributes(), null));
                    }
                } catch (PartialResultException e) {
                    // sisa hasil hanya referral
                } finally {
                    results.close();
                }
                cookie = null;
                Control[] controls = ctx.getResponseControls();
                if (controls != null) {
                    for (Control c : controls) {
                        if (c instanceof PagedResultsResponseControl prc) {
                            cookie = prc.getCookie();
                        }
                    }
                }
            } while (cookie != null && cookie.length > 0);
            log.info("Sync LDAP: {} user aktif dibaca dari {}", users.size(), props.searchBase());
            return users;
        } catch (AuthenticationException e) {
            log.error("Service account LDAP {} ditolak AD: {}", principal, e.getMessage());
            throw new DirectoryUnavailableException("Service account LDAP ditolak AD (cek LDAP_BIND_PASSWORD)", e);
        } catch (CommunicationException e) {
            log.error("Server LDAP {} tidak dapat dihubungi", props.url(), e);
            throw new DirectoryUnavailableException("Server LDAP tidak dapat dihubungi", e);
        } catch (NamingException | IOException e) {
            log.error("Error LDAP saat membaca daftar user", e);
            throw new DirectoryUnavailableException("Gagal membaca daftar user dari LDAP", e);
        } finally {
            if (ctx != null) {
                try {
                    ctx.close();
                } catch (NamingException ignored) {
                    // koneksi sudah ditutup
                }
            }
        }
    }

    private static DirectoryUser toUser(Attributes a, String fallbackUsername) throws NamingException {
        String sam = attr(a, "sAMAccountName");
        String nama = attr(a, "displayName");
        return new DirectoryUser(
                sam != null ? sam : fallbackUsername,
                nama != null ? nama : attr(a, "cn"),
                attr(a, "mail"), attr(a, "department"), attr(a, "title"),
                attr(a, "userPrincipalName"), attr(a, "distinguishedName"));
    }

    private static String attr(Attributes a, String name) throws NamingException {
        Attribute at = a.get(name);
        return at == null || at.get() == null ? null : at.get().toString();
    }

    private static int timeout(Integer value, int fallback) {
        return value != null && value > 0 ? value : fallback;
    }

    /** Escape karakter khusus filter LDAP (RFC 4515) agar username tidak bisa menyuntik filter. */
    static String escape(String s) {
        StringBuilder sb = new StringBuilder();
        for (char c : s.toCharArray()) {
            switch (c) {
                case '\\' -> sb.append("\\5c");
                case '*' -> sb.append("\\2a");
                case '(' -> sb.append("\\28");
                case ')' -> sb.append("\\29");
                case '\0' -> sb.append("\\00");
                default -> sb.append(c);
            }
        }
        return sb.toString();
    }
}
