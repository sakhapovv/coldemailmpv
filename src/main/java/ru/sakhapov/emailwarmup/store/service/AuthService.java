package ru.sakhapov.emailwarmup.store.service;

import ru.sakhapov.emailwarmup.api.dto.AuthResponse;
import ru.sakhapov.emailwarmup.api.dto.LoginRequest;
import ru.sakhapov.emailwarmup.api.dto.RegisterRequest;

public interface AuthService {

    AuthResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);
}
