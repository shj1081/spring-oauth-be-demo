package dev.hyzoon.oauth_test.user;

import dev.hyzoon.oauth_test.user.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    // email을 통해 이미 생성된 사용자인지 처음 가입하는 사용자인지 판단하기 위한 method
    Optional<User> findByEmail(String email);
}
