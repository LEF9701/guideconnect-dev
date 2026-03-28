package com.guideconnect.repository;

import com.guideconnect.model.AccountStatus;
import com.guideconnect.model.Role;
import com.guideconnect.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for {@link User} entities.
 * Provides query methods for user lookup, search, and role/status-based filtering.
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Finds a user by their unique email address.
     *
     * @param email the email address to search for
     * @return an Optional containing the user if found
     */
    Optional<User> findByEmail(String email);

    /**
     * Finds all users with the specified role.
     *
     * @param role the role to filter by (TOURIST, GUIDE, or ADMIN)
     * @return list of users with the given role
     */
    List<User> findByRole(Role role);

    /**
     * Finds all users with the specified account status.
     *
     * @param status the account status to filter by (ACTIVE, SUSPENDED, or BANNED)
     * @return list of users with the given status
     */
    List<User> findByStatus(AccountStatus status);

    /**
     * Searches users by partial match on display name or email (case-insensitive).
     * Supports pagination for large result sets.
     *
     * @param keyword  the search keyword to match against display name or email
     * @param pageable pagination and sorting parameters
     * @return a page of matching users
     */
    @Query("SELECT u FROM User u WHERE LOWER(u.displayName) LIKE LOWER(CONCAT('%', :keyword, '%')) "
         + "OR LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    Page<User> searchByDisplayNameOrEmail(@Param("keyword") String keyword, Pageable pageable);

    /**
     * Counts the number of users with the specified role.
     *
     * @param role the role to count
     * @return the count of users with that role
     */
    long countByRole(Role role);

    /**
     * Counts the number of users with the specified account status.
     *
     * @param status the account status to count
     * @return the count of users with that status
     */
    long countByStatus(AccountStatus status);
}
