package com.example.blog.user.service;

import com.example.blog.user.entity.CustomUserPrincipal;
import com.example.blog.user.entity.User;
import com.example.blog.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다."));

        // User 엔티티가 바로 UserDetails를 구현해도 되고,
        // 별도의 CustomUserDetails 클래스를 만들어서 감싸도 됩니다.
        return new CustomUserPrincipal(user);
    }
}

