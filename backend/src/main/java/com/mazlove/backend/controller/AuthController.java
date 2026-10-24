package com.mazlove.backend.controller;

import com.mazlove.backend.dto.LoginDTO;
import com.mazlove.backend.dto.OtpDTO;
import com.mazlove.backend.dto.RegisterDTO;
import com.mazlove.backend.dto.TokenDTO;
import com.mazlove.backend.exception.UnauthorizedException;
import com.mazlove.backend.service.AuthService;
import com.mazlove.backend.service.MfaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Parcours de connexion en deux étapes :
 * <ol>
 *   <li>POST /login : vérifie email + mot de passe puis envoie un code à 6 chiffres ;</li>
 *   <li>POST /verify-otp : échange ce code contre le JWT à utiliser ensuite.</li>
 * </ol>
 * Le code n'est jamais renvoyé dans la réponse HTTP : il doit arriver par un autre canal
 * (email/SMS), sinon la double authentification ne protège de rien.
 */
@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentification")
public class AuthController {

    private final AuthService authService;
    private final MfaService mfaService;

    public AuthController(AuthService authService, MfaService mfaService) {
        this.authService = authService;
        this.mfaService = mfaService;
    }

    @Operation(summary = "Créer un compte client ou praticien")
    @PostMapping("/register")
    public ResponseEntity<Map<String, String>> register(@Valid @RequestBody RegisterDTO dto) {
        authService.register(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("message", "Compte créé"));
    }

    @Operation(summary = "Étape 1 : vérifier les identifiants et envoyer le code de connexion")
    @PostMapping("/login")
    public ResponseEntity<Map<String, String>> login(@Valid @RequestBody LoginDTO loginDTO) {
        authService.validateCredentials(loginDTO.email(), loginDTO.password());
        // Génère ET stocke le code dans Redis en un seul appel (plus de second code qui écrase le premier).
        mfaService.sendOtp(loginDTO.email());
        return ResponseEntity.ok(Map.of("message", "Code de connexion envoyé à " + loginDTO.email().trim()));
    }

    @Operation(summary = "Étape 2 : échanger le code reçu contre un JWT")
    @PostMapping("/verify-otp")
    public ResponseEntity<TokenDTO> verifyOtp(@Valid @RequestBody OtpDTO otpDTO) {
        if (!mfaService.verifyOtp(otpDTO)) {
            // 401 au format d'erreur commun ({ "message": … }) plutôt qu'un faux jeton "error:…"
            // qu'un client distrait pourrait stocker comme s'il s'agissait d'un vrai JWT.
            throw new UnauthorizedException("Code invalide ou expiré. Demandez un nouveau code.");
        }
        return ResponseEntity.ok(new TokenDTO(authService.generateJwt(otpDTO.email())));
    }
}
