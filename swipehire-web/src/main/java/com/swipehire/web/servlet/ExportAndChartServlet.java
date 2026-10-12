package com.swipehire.web.servlet;

import com.swipehire.common.enums.Role;
import com.swipehire.web.dao.AnalyticsDao;
import com.swipehire.web.security.AppUserPrincipal;
import com.swipehire.web.service.ChartService;
import com.swipehire.web.service.JobService;
import com.swipehire.web.support.NotFoundException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Generated files:
 * <ul>
 *   <li>/files/chart?type=funnel|skillgap&amp;jobId=..  (hirer)  - PNG drawn by JFreeChart</li>
 *   <li>/files/chart?type=missing                       (applicant)</li>
 *   <li>/files/chart?type=activity                      (admin)</li>
 *   <li>/files/export/pipeline.csv?jobId=..             (hirer)  - CSV written with a PrintWriter</li>
 * </ul>
 */
@WebServlet(name = "exportAndChart", urlPatterns = {"/files/chart", "/files/export/pipeline.csv"})
public class ExportAndChartServlet extends BaseServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        AppUserPrincipal me = currentUser(req);
        if (me == null) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        try {
            if (req.getServletPath().endsWith(".csv")) {
                csv(me, longParam(req, "jobId"), resp);
            } else {
                chart(me, req.getParameter("type"), longParam(req, "jobId"), resp);
            }
        } catch (NotFoundException e) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    private void chart(AppUserPrincipal me, String type, long jobId, HttpServletResponse resp) throws IOException {
        ChartService charts = bean(ChartService.class);
        byte[] png;
        if (("funnel".equals(type) || "skillgap".equals(type)) && me.getRole() == Role.HIRER) {
            bean(JobService.class).requireOwned(jobId, me.getId());
            png = "funnel".equals(type) ? charts.jobFunnel(jobId) : charts.skillGap(jobId);
        } else if ("missing".equals(type) && me.getRole() == Role.APPLICANT) {
            png = charts.missingSkills(me.getId());
        } else if ("activity".equals(type) && me.getRole() == Role.ADMIN) {
            png = charts.dailyActivity(14);
        } else {
            throw new NotFoundException("Unknown chart");
        }
        resp.setContentType("image/png");
        resp.setHeader("Cache-Control", "no-store");
        resp.setContentLength(png.length);
        resp.getOutputStream().write(png);
    }

    private void csv(AppUserPrincipal me, long jobId, HttpServletResponse resp) throws IOException {
        if (me.getRole() != Role.HIRER) {
            throw new NotFoundException("Not allowed");
        }
        bean(JobService.class).requireOwned(jobId, me.getId());
        List<String[]> rows = bean(AnalyticsDao.class).pipelineRows(jobId);
        resp.setContentType("text/csv");
        resp.setCharacterEncoding(StandardCharsets.UTF_8.name());
        resp.setHeader("Content-Disposition", "attachment; filename=\"job-" + jobId + "-pipeline.csv\"");
        PrintWriter out = resp.getWriter();
        out.println("Name,Email,Verified college,Graduation year,Status,Score %,Matched at,Applied at,Last change");
        for (String[] row : rows) {
            StringBuilder line = new StringBuilder();
            for (int i = 0; i < row.length; i++) {
                if (i > 0) {
                    line.append(',');
                }
                line.append(escape(row[i]));
            }
            out.println(line);
        }
        out.flush();
    }

    /** CSV quoting: wrap in quotes and double any quote inside, so commas in names don't break columns. */
    static String escape(String v) {
        if (v == null) {
            return "";
        }
        return "\"" + v.replace("\"", "\"\"") + "\"";
    }
}
