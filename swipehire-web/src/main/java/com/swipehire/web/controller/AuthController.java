package com.swipehire.web.controller;

import com.swipehire.common.enums.Role;
import com.swipehire.web.repository.CompanyRepository;
import com.swipehire.web.service.AccountService;
import com.swipehire.web.support.BusinessException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AuthController {

    private final AccountService accounts;
    private final CompanyRepository companies;

    public AuthController(AccountService accounts, CompanyRepository companies) {
        this.accounts = accounts;
        this.companies = companies;
    }

    /** The POST /login itself is handled by Spring Security's form-login filter. */
    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/signup")
    public String signupForm(@RequestParam(defaultValue = "APPLICANT") Role role, Model model) {
        model.addAttribute("role", role == Role.HIRER ? Role.HIRER : Role.APPLICANT);
        model.addAttribute("companies", companies.findAllByOrderByNameAsc());
        return "signup";
    }

    @PostMapping("/signup")
    public String signup(@RequestParam Role role, @RequestParam String fullName, @RequestParam String email,
                         @RequestParam String password,
                         @RequestParam(required = false) Long companyId,
                         @RequestParam(required = false) String newCompanyName,
                         @RequestParam(required = false) String companyCity,
                         @RequestParam(required = false) String companyWebsite,
                         @RequestParam(required = false) String designation,
                         Model model, RedirectAttributes flash) {
        try {
            if (role == Role.HIRER) {
                accounts.registerHirer(fullName, email, password, companyId, newCompanyName, companyCity, companyWebsite, designation);
            } else {
                accounts.registerApplicant(fullName, email, password);
            }
        } catch (BusinessException e) {
            // Re-show the form with what the user typed (except the password).
            model.addAttribute("error", e.getMessage());
            model.addAttribute("role", role);
            model.addAttribute("companies", companies.findAllByOrderByNameAsc());
            model.addAttribute("fullName", fullName);
            model.addAttribute("email", email);
            model.addAttribute("newCompanyName", newCompanyName);
            return "signup";
        }
        flash.addFlashAttribute("flash", "Account created. Log in to continue.");
        return "redirect:/login";
    }
}
