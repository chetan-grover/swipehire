package com.swipehire.web.controller;

import com.swipehire.web.security.AppUserPrincipal;
import com.swipehire.web.service.NotificationService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/** Adds the logged-in user and their unread-notification count to every page (used by the header). */
@ControllerAdvice
public class ViewModelAdvice {

    private final NotificationService notifications;

    public ViewModelAdvice(NotificationService notifications) {
        this.notifications = notifications;
    }

    @ModelAttribute("me")
    public AppUserPrincipal me(@AuthenticationPrincipal AppUserPrincipal me) {
        return me;
    }

    @ModelAttribute("unreadCount")
    public long unreadCount(@AuthenticationPrincipal AppUserPrincipal me) {
        return me == null ? 0 : notifications.unreadCount(me.getId());
    }
}
