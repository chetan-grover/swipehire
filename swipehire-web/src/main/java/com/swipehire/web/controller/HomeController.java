package com.swipehire.web.controller;

import com.swipehire.web.dao.PlatformStatsDao;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.sql.SQLException;

@Controller
public class HomeController {

    private final PlatformStatsDao statsDao;

    public HomeController(PlatformStatsDao statsDao) {
        this.statsDao = statsDao;
    }

    @GetMapping("/")
    public String home(Model model) throws SQLException {
        model.addAttribute("stats", statsDao.loadStats());
        return "index";
    }
}
