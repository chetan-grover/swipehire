package com.swipehire.web.service;

import com.swipehire.web.dao.AnalyticsDao;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryLabelPositions;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.chart.renderer.category.StandardBarPainter;
import org.jfree.data.category.DefaultCategoryDataset;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Map;

/** Server-side charts: JFreeChart draws a PNG in Java, so the browser needs no charting JavaScript. */
@Service
public class ChartService {

    // Same palette as app.css: sage, terracotta clay, forest for text.
    private static final Color SAGE = new Color(0x55, 0x73, 0x52);
    private static final Color CLAY = new Color(0xB8, 0x61, 0x3F);
    private static final Color FOREST = new Color(0x2F, 0x4A, 0x35);
    private static final Color CARD = new Color(0xFF, 0xFE, 0xFB);

    private final AnalyticsDao analytics;

    public ChartService(AnalyticsDao analytics) {
        this.analytics = analytics;
    }

    public byte[] jobFunnel(long jobId) {
        return bars("Hiring funnel", "Count", analytics.jobFunnel(jobId), SAGE, true);
    }

    public byte[] skillGap(long jobId) {
        return bars("Interested applicants who have each skill", "% of applicants", analytics.skillGap(jobId), CLAY, true);
    }

    public byte[] missingSkills(long applicantId) {
        return bars("Required skills you're missing (jobs you liked)", "Jobs", analytics.applicantMissingSkills(applicantId, 8),
                CLAY, true);
    }

    public byte[] dailyActivity(int days) {
        DefaultCategoryDataset ds = new DefaultCategoryDataset();
        List<AnalyticsDao.DailyActivity> rows = analytics.dailyActivity(days);
        for (AnalyticsDao.DailyActivity r : rows) {
            String label = r.day().getDayOfMonth() + "/" + r.day().getMonthValue();
            ds.addValue(r.swipes(), "Swipes", label);
            ds.addValue(r.matches(), "Matches", label);
        }
        JFreeChart chart = ChartFactory.createBarChart("Platform activity (last " + days + " days)", null, "Count", ds,
                PlotOrientation.VERTICAL, true, false, false);
        style(chart, SAGE);
        ((BarRenderer) chart.getCategoryPlot().getRenderer()).setSeriesPaint(1, CLAY);
        chart.getCategoryPlot().getDomainAxis().setCategoryLabelPositions(CategoryLabelPositions.UP_45);
        return png(chart, 760, 340);
    }

    private byte[] bars(String title, String valueAxis, Map<String, ? extends Number> data, Color color, boolean horizontal) {
        DefaultCategoryDataset ds = new DefaultCategoryDataset();
        data.forEach((k, v) -> ds.addValue(v, "value", k));
        JFreeChart chart = ChartFactory.createBarChart(title, null, valueAxis, ds,
                horizontal ? PlotOrientation.HORIZONTAL : PlotOrientation.VERTICAL, false, false, false);
        style(chart, color);
        return png(chart, 640, Math.max(220, 60 + 34 * data.size()));
    }

    private static void style(JFreeChart chart, Color color) {
        chart.setBackgroundPaint(CARD);
        chart.getTitle().setPaint(FOREST);
        CategoryPlot plot = chart.getCategoryPlot();
        plot.setBackgroundPaint(CARD);
        plot.setRangeGridlinePaint(new Color(0xE2, 0xE0, 0xD2));
        plot.setOutlineVisible(false);
        BarRenderer r = (BarRenderer) plot.getRenderer();
        r.setBarPainter(new StandardBarPainter());
        r.setShadowVisible(false);
        r.setSeriesPaint(0, color);
        r.setMaximumBarWidth(0.12);
    }

    private static byte[] png(JFreeChart chart, int w, int h) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            ChartUtils.writeChartAsPNG(out, chart, w, h);
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
