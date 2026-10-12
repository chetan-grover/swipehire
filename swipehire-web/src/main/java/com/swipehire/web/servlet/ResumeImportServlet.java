package com.swipehire.web.servlet;

import com.swipehire.web.config.Storage;
import com.swipehire.web.controller.ApplicantProfileController;
import com.swipehire.web.security.AppUserPrincipal;
import com.swipehire.web.service.ProfileService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

/**
 * POST /files/resume-import: the applicant uploads an existing PDF resume. Apache PDFBox extracts
 * its text, we detect dictionary skills in it, and the applicant reviews the suggestions on the
 * next page. Nothing is added to the profile until they confirm.
 */
@WebServlet(name = "resumeImport", urlPatterns = "/files/resume-import")
@MultipartConfig(maxFileSize = 5 * 1024 * 1024, maxRequestSize = 6 * 1024 * 1024)
public class ResumeImportServlet extends BaseServlet {

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {
        AppUserPrincipal me = currentUser(req);
        if (me == null) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        Part part;
        try {
            part = req.getPart("resume");
        } catch (IllegalStateException tooBig) {
            fail(req, resp, "The PDF must be smaller than 5 MB");
            return;
        }
        if (part == null || part.getSize() == 0) {
            fail(req, resp, "Choose a PDF file first");
            return;
        }
        byte[] bytes;
        try (InputStream in = part.getInputStream()) {
            bytes = in.readAllBytes();
        }
        // Check the PDF magic number instead of trusting the browser's content type.
        if (bytes.length < 5 || !new String(bytes, 0, 5).equals("%PDF-")) {
            fail(req, resp, "That file is not a PDF");
            return;
        }
        String text;
        try (PDDocument doc = Loader.loadPDF(bytes)) {
            text = new PDFTextStripper().getText(doc);
        } catch (IOException e) {
            fail(req, resp, "Could not read the PDF (is it password-protected or scanned?)");
            return;
        }
        Path target = bean(Storage.class).uploads().resolve("user-" + me.getId() + "-" + System.currentTimeMillis() + ".pdf");
        Files.write(target, bytes);

        String original = part.getSubmittedFileName() == null ? "resume.pdf" : Path.of(part.getSubmittedFileName()).getFileName().toString();
        Set<Long> suggestions = bean(ProfileService.class).recordUploadAndSuggest(me.getId(),
                original.length() > 200 ? original.substring(0, 200) : original, target.toString(), text);
        req.getSession().setAttribute(ApplicantProfileController.IMPORT_SUGGESTIONS, suggestions);
        resp.sendRedirect(req.getContextPath() + "/applicant/skills/import-review");
    }

    private void fail(HttpServletRequest req, HttpServletResponse resp, String message) throws IOException {
        req.getSession().setAttribute("servletError", message);
        resp.sendRedirect(req.getContextPath() + "/applicant/skills");
    }
}
