package id.co.realisasi.auth;

/** Atribut user hasil autentikasi ke direktori (AD). */
public record DirectoryUser(String username, String nama, String email, String departemen, String jabatan,
                            String userPrincipalName, String distinguishedName) {
}
