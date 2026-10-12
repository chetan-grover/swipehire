package com.swipehire.web.service;

import com.swipehire.common.enums.Role;
import com.swipehire.web.entity.ApplicantProfile;
import com.swipehire.web.entity.Company;
import com.swipehire.web.entity.Hirer;
import com.swipehire.web.entity.User;
import com.swipehire.web.repository.ApplicantProfileRepository;
import com.swipehire.web.repository.CompanyRepository;
import com.swipehire.web.repository.HirerRepository;
import com.swipehire.web.repository.UserRepository;
import com.swipehire.web.support.BusinessException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

/** Sign-up for applicants and hirers. Passwords are stored only as BCrypt hashes. */
@Service
public class AccountService {

    private final UserRepository users;
    private final ApplicantProfileRepository profiles;
    private final CompanyRepository companies;
    private final HirerRepository hirers;
    private final PasswordEncoder encoder;

    public AccountService(UserRepository users, ApplicantProfileRepository profiles, CompanyRepository companies,
                          HirerRepository hirers, PasswordEncoder encoder) {
        this.users = users;
        this.profiles = profiles;
        this.companies = companies;
        this.hirers = hirers;
        this.encoder = encoder;
    }

    @Transactional
    public User registerApplicant(String fullName, String email, String password) {
        User user = createUser(fullName, email, password, Role.APPLICANT);
        profiles.save(new ApplicantProfile(user));
        return user;
    }

    /**
     * A hirer either joins an existing company or creates a new one. New companies start as
     * PENDING until an admin verifies them.
     */
    @Transactional
    public User registerHirer(String fullName, String email, String password, Long existingCompanyId,
                              String newCompanyName, String companyCity, String companyWebsite, String designation) {
        Company company;
        if (existingCompanyId != null) {
            company = companies.findById(existingCompanyId).orElseThrow(() -> new BusinessException("Company not found"));
        } else {
            String name = trimToNull(newCompanyName);
            if (name == null) {
                throw new BusinessException("Pick your company or enter a new company name");
            }
            if (companies.findByNameIgnoreCase(name).isPresent()) {
                throw new BusinessException("'" + name + "' already exists: select it from the list instead");
            }
            company = companies.save(new Company(name, trimToNull(companyWebsite), trimToNull(companyCity)));
        }
        User user = createUser(fullName, email, password, Role.HIRER);
        hirers.save(new Hirer(user, company, trimToNull(designation)));
        return user;
    }

    private User createUser(String fullName, String email, String password, Role role) {
        String cleanEmail = email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
        if (!cleanEmail.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
            throw new BusinessException("Enter a valid email address");
        }
        if (fullName == null || fullName.isBlank()) {
            throw new BusinessException("Enter your name");
        }
        if (password == null || password.length() < 8) {
            throw new BusinessException("Password must be at least 8 characters");
        }
        if (users.existsByEmailIgnoreCase(cleanEmail)) {
            throw new BusinessException("An account with this email already exists");
        }
        return users.save(new User(cleanEmail, encoder.encode(password), fullName.trim(), role));
    }

    static String trimToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
