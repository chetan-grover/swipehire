package com.swipehire.web.servlet;

import com.swipehire.common.enums.Role;
import com.swipehire.web.security.AppUserPrincipal;
import com.swipehire.web.service.PipelineService;
import com.swipehire.web.service.TailoringService;
import com.swipehire.web.support.NotFoundException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * GET /files/resume?applicationId=7 streams the tailored resume PDF.
 * A raw servlet is a natural fit for binary downloads: we set the content type and headers and
 * copy bytes to the response output stream ourselves.
 */
@WebServlet(name = "resumePdf", urlPatterns = "/files/resume")
public class ResumePdfServlet extends BaseServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        AppUserPrincipal me = currentUser(req);
        long appId = longParam(req, "applicationId");
        if (me == null || appId < 0) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        PipelineService pipeline = bean(PipelineService.class);
        Path pdf;
        try {
            // Only the applicant themself, a recruiter of the job's company, or an admin may download.
            if (me.getRole() == Role.APPLICANT) {
                pipeline.forApplicant(appId, me.getId());
            } else if (me.getRole() == Role.HIRER) {
                pipeline.forHirer(appId, me.getId());
            }
            pdf = bean(TailoringService.class).ensurePdf(appId);
        } catch (NotFoundException e) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        resp.setContentType("application/pdf");
        resp.setHeader("Content-Disposition", "inline; filename=\"tailored-resume-" + appId + ".pdf\"");
        resp.setContentLengthLong(Files.size(pdf));
        Files.copy(pdf, resp.getOutputStream());
    }
}
