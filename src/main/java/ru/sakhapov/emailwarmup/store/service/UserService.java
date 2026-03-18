package ru.sakhapov.emailwarmup.store.service;

import ru.sakhapov.emailwarmup.api.dto.UserMeResponse;

public interface UserService {

    UserMeResponse getCurrentUser(String email);
}
