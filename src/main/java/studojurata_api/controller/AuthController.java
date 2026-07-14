package studojurata_api.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.bind.annotation.*;
import studojurata_api.dto.LoginRequest;
import studojurata_api.dto.LoginResponse;
import studojurata_api.security.CustomUserDetails;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final HttpSessionSecurityContextRepository securityContextRepository = new HttpSessionSecurityContextRepository();

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request,
                                                HttpServletRequest httpRequest) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getSenha()));

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, httpRequest, null);

        CustomUserDetails principal = (CustomUserDetails) authentication.getPrincipal();
        var usuario = principal.getUsuario();

        LoginResponse response = new LoginResponse(
                usuario.getId(),
                usuario.getUsername(),
                usuario.getTipoUsuario(),
                usuario.getPessoa() != null ? usuario.getPessoa().getId() : null,
                usuario.getPessoa() != null ? usuario.getPessoa().getNome() : null
        );

        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request) {
        request.getSession().invalidate();
        SecurityContextHolder.clearContext();
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    public ResponseEntity<LoginResponse> me() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof CustomUserDetails principal)) {
            return ResponseEntity.status(401).build();
        }

        var usuario = principal.getUsuario();
        LoginResponse response = new LoginResponse(
                usuario.getId(),
                usuario.getUsername(),
                usuario.getTipoUsuario(),
                usuario.getPessoa() != null ? usuario.getPessoa().getId() : null,
                usuario.getPessoa() != null ? usuario.getPessoa().getNome() : null
        );
        return ResponseEntity.ok(response);
    }
}
