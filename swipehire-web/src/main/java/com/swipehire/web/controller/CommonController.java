package com.swipehire.web.controller;

import com.swipehire.common.enums.Role;
import com.swipehire.web.entity.Notification;
import com.swipehire.web.repository.ApplicationRepository;
import com.swipehire.web.security.AppUserPrincipal;
import com.swipehire.web.service.ChatService;
import com.swipehire.web.service.NotificationService;
import com.swipehire.web.support.NotFoundException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/** Pages shared by all roles: notifications and sending chat messages. */
@Controller
public class CommonController {

    private final NotificationService notifications;
    private final ChatService chat;
    private final ApplicationRepository applications;

    public CommonController(NotificationService notifications, ChatService chat, ApplicationRepository applications) {
        this.notifications = notifications;
        this.chat = chat;
        this.applications = applications;
    }

    @GetMapping("/notifications")
    public String notifications(@AuthenticationPrincipal AppUserPrincipal me, Model model) {
        List<Notification> list = notifications.recent(me.getId());
        model.addAttribute("notifications", list);
        return "notifications";
    }

    @PostMapping("/notifications/read-all")
    public String readAll(@AuthenticationPrincipal AppUserPrincipal me) {
        notifications.markAllRead(me.getId());
        return "redirect:/notifications";
    }

    /** Plain form POST; the other side receives it live over the WebSocket. */
    @PostMapping("/chat/{matchId}")
    public String send(@AuthenticationPrincipal AppUserPrincipal me, @PathVariable long matchId, @RequestParam String body) {
        chat.send(matchId, me.getId(), body);
        long appId = applications.findByMatchId(matchId).orElseThrow(() -> new NotFoundException("Not found")).getId();
        String page = me.getRole() == Role.APPLICANT ? "/applicant/matches/" : "/hirer/applications/";
        return "redirect:" + page + appId + "#chat";
    }
}
