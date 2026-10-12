package com.swipehire.web.servlet;

import com.swipehire.web.config.Storage;
import com.swipehire.web.security.AppUserPrincipal;
import com.swipehire.web.service.ProfileService;
import com.swipehire.web.support.NotFoundException;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;

/**
 * Profile photos.
 * <ul>
 *   <li>POST /files/photo (multipart, field "photo"): the logged-in applicant uploads a JPEG/PNG up to 2 MB</li>
 *   <li>GET  /files/photo?userId=101: streams that user's photo</li>
 * </ul>
 * {@code @MultipartConfig} tells the container to parse multipart/form-data so getPart() works.
 */
@WebServlet(name = "photo", urlPatterns = "/files/photo")
@MultipartConfig(maxFileSize = 2 * 1024 * 1024, maxRequestSize = 3 * 1024 * 1024)
public class PhotoServlet extends BaseServlet {

    private static final Map<String, String> TYPES = Map.of("image/jpeg", ".jpg", "image/png", ".png");

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {
        AppUserPrincipal me = currentUser(req);
        if (me == null) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        Part part;
        try {
            part = req.getPart("photo");
        } catch (IllegalStateException tooBig) {
            redirect(req, resp, "Photo must be smaller than 2 MB");
            return;
        }
        String ext = part == null ? null : TYPES.get(part.getContentType());
        if (ext == null || part.getSize() == 0) {
            redirect(req, resp, "Upload a JPEG or PNG image");
            return;
        }
        // The file name is derived from the user id, never from the uploaded name (no path tricks).
        Path target = bean(Storage.class).photos().resolve("user-" + me.getId() + ext);
        try (InputStream in = part.getInputStream()) {
            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
        }
        bean(ProfileService.class).setPhoto(me.getId(), target.toString());
        redirect(req, resp, null);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        long userId = longParam(req, "userId");
        String path;
        try {
            path = userId < 0 ? null : bean(ProfileService.class).get(userId).getPhotoPath();
        } catch (NotFoundException e) {
            path = null;
        }
        if (path == null || !Files.exists(Path.of(path))) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        Path file = Path.of(path);
        resp.setContentType(path.endsWith(".png") ? "image/png" : "image/jpeg");
        resp.setHeader("Cache-Control", "private, max-age=300");
        resp.setContentLengthLong(Files.size(file));
        Files.copy(file, resp.getOutputStream());
    }

    private void redirect(HttpServletRequest req, HttpServletResponse resp, String error) throws IOException {
        if (error != null) {
            req.getSession().setAttribute("servletError", error);
        }
        resp.sendRedirect(req.getContextPath() + "/applicant/profile");
    }
}
