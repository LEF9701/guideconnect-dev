package com.guideconnect.service;

import com.guideconnect.model.AccountStatus;
import com.guideconnect.model.Role;
import com.guideconnect.model.User;
import com.guideconnect.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Optional;

/**
 * Service layer for user management and Spring Security authentication.
 * Implements {@link UserDetailsService} to integrate with Spring Security's
 * authentication mechanism, loading users by email address.
 */
@Service
public class UserService implements UserDetailsService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Constructs a UserService with required dependencies.
     *
     * @param userRepository  the user repository for data access
     * @param passwordEncoder the BCrypt encoder for password hashing
     */
    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Loads a user by email address for Spring Security authentication.
     * The granted authority is derived from the user's role with a "ROLE_" prefix.
     *
     * @param email the email address used as the username
     * @return a Spring Security {@link UserDetails} object
     * @throws UsernameNotFoundException if no user exists with the given email
     */
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));

        return new org.springframework.security.core.userdetails.User(
                user.getEmail(),
                user.getPassword(),
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()))
        );
    }

    /**
     * Registers a new user with the given details. The password is BCrypt-encoded
     * before persistence. Throws an exception if the email is already in use.
     *
     * @param displayName the user's display name
     * @param email       the user's unique email address
     * @param password    the plaintext password to be encoded
     * @param role        the user's role (TOURIST, GUIDE, or ADMIN)
     * @return the persisted User entity
     * @throws IllegalArgumentException if the email is already registered
     */
    @Transactional
    public User register(String displayName, String email, String password, Role role) {
        if (userRepository.findByEmail(email).isPresent()) {
            throw new IllegalArgumentException("Email already registered: " + email);
        }

        String encodedPassword = passwordEncoder.encode(password);
        User user = new User(displayName, email, encodedPassword, role);
        return userRepository.save(user);
    }

    /**
     * Finds a user by their unique identifier.
     *
     * @param id the user ID
     * @return an Optional containing the user if found
     */
    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }

    /**
     * Finds a user by their email address.
     *
     * @param email the email address to search for
     * @return an Optional containing the user if found
     */
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    /**
     * Updates the profile of a tourist user, including display name,
     * contact information, and language preferences.
     *
     * @param userId              the ID of the user to update
     * @param displayName         the new display name
     * @param contactInfo         the new contact information
     * @param languagePreferences the new language preferences
     * @return the updated User entity
     * @throws IllegalArgumentException if the user is not found
     */
    @Transactional
    public User updateTouristProfile(Long userId, String displayName, String contactInfo, String languagePreferences) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + userId));

        user.setDisplayName(displayName);
        user.setContactInfo(contactInfo);
        user.setLanguagePreferences(languagePreferences);
        return userRepository.save(user);
    }

    /**
     * Updates the profile of a guide user, including display name,
     * biography, languages spoken, and profile pricing.
     *
     * @param userId         the ID of the user to update
     * @param displayName    the new display name
     * @param biography      the new biography text
     * @param languagesSpoken the new languages spoken
     * @param guidePricing    the guide's profile pricing
     * @return the updated User entity
     * @throws IllegalArgumentException if the user is not found
     */
    @Transactional
    public User updateGuideProfile(Long userId, String displayName, String biography,
                                   String languagesSpoken, BigDecimal guidePricing) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + userId));

        user.setDisplayName(displayName);
        user.setBiography(biography);
        user.setLanguagesSpoken(languagesSpoken);
        user.setGuidePricing(guidePricing);
        return userRepository.save(user);
    }

    /**
     * Updates the account status of a user. Intended for admin use to
     * activate, suspend, or ban user accounts.
     *
     * @param userId the ID of the user to update
     * @param status the new account status
     * @return the updated User entity
     * @throws IllegalArgumentException if the user is not found
     */
    @Transactional
    public User updateUserStatus(Long userId, AccountStatus status) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + userId));

        user.setStatus(status);
        return userRepository.save(user);
    }

    /**
     * Retrieves all users with pagination support.
     *
     * @param pageable pagination and sorting parameters
     * @return a page of all users
     */
    public Page<User> findAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable);
    }

    /**
     * Searches users by display name or email using a partial, case-insensitive match.
     *
     * @param query    the search keyword
     * @param pageable pagination and sorting parameters
     * @return a page of matching users
     */
    public Page<User> searchUsers(String query, Pageable pageable) {
        return userRepository.searchByDisplayNameOrEmail(query, pageable);
    }

    /**
     * Counts the number of users with the specified role.
     *
     * @param role the role to count
     * @return the count of users with that role
     */
    public long countByRole(Role role) {
        return userRepository.countByRole(role);
    }

    /**
     * Counts the total number of registered users.
     *
     * @return the total user count
     */
    public long countTotal() {
        return userRepository.count();
    }
}
