package com.mazlove.backend.util;

import java.util.Locale;

/** Normalisation des emails, partagée par l'inscription, la connexion et l'OTP. */
public final class Emails {

    private Emails() {
    }

    /**
     * " Camille@Exemple.FR " devient "camille@exemple.fr".
     * Sans cela, la même personne pourrait créer deux comptes, et la clé Redis de l'OTP
     * (otp:{email}) ne correspondrait plus entre /login et /verify-otp.
     */
    public static String normalize(String email) {
        return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }
}
