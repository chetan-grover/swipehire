package com.swipehire.web.controller;

import com.swipehire.common.enums.JobStatus;
import com.swipehire.common.enums.Role;
import com.swipehire.common.enums.VerificationStatus;
import com.swipehire.web.dao.PlatformStatsDao;
import com.swipehire.web.repository.ApplicationStatusHistoryRepository;
import com.swipehire.web.repository.CollegeRepository;
import com.swipehire.web.repository.CompanyRepository;
import com.swipehire.web.repository.SkillSynonymRepository;
import com.swipehire.web.security.AppUserPrincipal;
import com.swipehire.web.service.AdminService;
import com.swipehire.web.service.BackgroundJobs;
import com.swipehire.web.service.DemoDataService;
import com.swipehire.web.service.EngineClient;
import com.swipehire.web.service.SkillCatalog;
import com.swipehire.web.websocket.NotificationHub;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.sql.SQLException;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private final AdminService admin;
    private final PlatformStatsDao stats;
    private final EngineClient engine;
    private final CompanyRepository companies;
    private final CollegeRepository colleges;
    private final SkillSynonymRepository synonyms;
    private final ApplicationStatusHistoryRepository history;
    private final SkillCatalog catalog;
    private final BackgroundJobs jobs;
    private final DemoDataService demoData;

    public AdminController(AdminService admin, PlatformStatsDao stats, EngineClient engine, CompanyRepository companies,
                           CollegeRepository colleges, SkillSynonymRepository synonyms,
                           ApplicationStatusHistoryRepository history, SkillCatalog catalog, BackgroundJobs jobs,
                           DemoDataService demoData) {
        this.demoData = demoData;
        this.admin = admin;
        this.stats = stats;
        this.engine = engine;
        this.companies = companies;
        this.colleges = colleges;
        this.synonyms = synonyms;
        this.history = history;
        this.catalog = catalog;
        this.jobs = jobs;
    }

    @GetMapping
    public String dashboard(Model model) throws SQLException {
        model.addAttribute("stats", stats.loadStats());
        model.addAttribute("enginePing", engine.ping());
        model.addAttribute("engine", engine);
        model.addAttribute("onlineUsers", NotificationHub.onlineUsers());
        model.addAttribute("pendingCompanies", companies.findByStatusOrderByCreatedAtAsc(VerificationStatus.PENDING).size());
        model.addAttribute("pendingColleges", colleges.findByStatusOrderByNameAsc(VerificationStatus.PENDING).size());
        return "admin/dashboard";
    }

    @PostMapping("/run-maintenance")
    public String runMaintenance(RedirectAttributes flash) {
        flash.addFlashAttribute("flash", "Maintenance ran: " + jobs.hourly());
        return "redirect:/admin";
    }

    @PostMapping("/demo-data")
    public String demoData(RedirectAttributes flash) {
        flash.addFlashAttribute("flash", "Demo data: " + demoData.generate(100, 30));
        return "redirect:/admin";
    }

    // ------------------------------------------------------------- verification queues

    @GetMapping("/companies")
    public String companies(Model model) {
        model.addAttribute("pending", companies.findByStatusOrderByCreatedAtAsc(VerificationStatus.PENDING));
        model.addAttribute("all", companies.findAllByOrderByNameAsc());
        return "admin/companies";
    }

    @PostMapping("/companies/{id}")
    public String companyStatus(@AuthenticationPrincipal AppUserPrincipal me, @PathVariable long id,
                                @RequestParam VerificationStatus status, RedirectAttributes flash) {
        admin.setCompanyStatus(id, status, me.getId());
        flash.addFlashAttribute("flash", "Company marked " + status);
        return "redirect:/admin/companies";
    }

    @GetMapping("/colleges")
    public String colleges(Model model) {
        model.addAttribute("pending", colleges.findByStatusOrderByNameAsc(VerificationStatus.PENDING));
        model.addAttribute("verified", colleges.findByStatusOrderByNameAsc(VerificationStatus.VERIFIED));
        return "admin/colleges";
    }

    @PostMapping("/colleges/{id}")
    public String collegeStatus(@PathVariable long id, @RequestParam VerificationStatus status, RedirectAttributes flash) {
        admin.setCollegeStatus(id, status);
        flash.addFlashAttribute("flash", "College marked " + status);
        return "redirect:/admin/colleges";
    }

    @PostMapping("/colleges")
    public String addCollege(@RequestParam String name, @RequestParam String domain,
                             @RequestParam(required = false) String city, RedirectAttributes flash) {
        admin.addCollege(name, domain, city);
        flash.addFlashAttribute("flash", name + " added");
        return "redirect:/admin/colleges";
    }

    // ------------------------------------------------------------- users & jobs (Criteria API search)

    @GetMapping("/users")
    public String users(@RequestParam(required = false) String q, @RequestParam(required = false) Role role,
                        @RequestParam(required = false) Boolean enabled, Model model) {
        model.addAttribute("users", admin.searchUsers(q, role, enabled));
        model.addAttribute("roles", Role.values());
        model.addAttribute("q", q);
        model.addAttribute("role", role);
        model.addAttribute("enabled", enabled);
        return "admin/users";
    }

    @PostMapping("/users/{id}/toggle")
    public String toggleUser(@AuthenticationPrincipal AppUserPrincipal me, @PathVariable long id, RedirectAttributes flash) {
        admin.toggleUser(id, me.getId());
        flash.addFlashAttribute("flash", "User updated");
        return "redirect:/admin/users";
    }

    @GetMapping("/jobs")
    public String jobs(@RequestParam(required = false) String q, @RequestParam(required = false) JobStatus status, Model model) {
        model.addAttribute("jobs", admin.searchJobs(q, status));
        model.addAttribute("statuses", JobStatus.values());
        model.addAttribute("q", q);
        model.addAttribute("status", status);
        return "admin/jobs";
    }

    @PostMapping("/jobs/{id}/close")
    public String closeJob(@PathVariable long id, RedirectAttributes flash) {
        admin.closeJob(id);
        flash.addFlashAttribute("flash", "Job closed");
        return "redirect:/admin/jobs";
    }

    // ------------------------------------------------------------- skills & audit

    @GetMapping("/skills")
    public String skills(Model model) {
        model.addAttribute("skillsByCategory", catalog.byCategory());
        model.addAttribute("skills", catalog.all());
        model.addAttribute("synonyms", synonyms.findAllByOrderByAliasAsc());
        return "admin/skills";
    }

    @PostMapping("/skills")
    public String addSkill(@RequestParam String name, @RequestParam String category, RedirectAttributes flash) {
        admin.addSkill(name, category);
        flash.addFlashAttribute("flash", "Skill added");
        return "redirect:/admin/skills";
    }

    @PostMapping("/synonyms")
    public String addSynonym(@RequestParam String alias, @RequestParam long skillId, RedirectAttributes flash) {
        admin.addSynonym(alias, skillId);
        flash.addFlashAttribute("flash", "Synonym added");
        return "redirect:/admin/skills";
    }

    @PostMapping("/synonyms/delete")
    public String deleteSynonym(@RequestParam String alias) {
        admin.deleteSynonym(alias);
        return "redirect:/admin/skills";
    }

    @GetMapping("/audit")
    public String audit(Model model) {
        model.addAttribute("rows", history.findRecent(PageRequest.of(0, 100)));
        return "admin/audit";
    }
}
