package org.kirya343.core.exceptions;

public class UserNotFoundException extends RuntimeException {
    public UserNotFoundException(String userParam) {
        super("Пользователь не найден по параметру: \"" + userParam + "\"");
    }
}
