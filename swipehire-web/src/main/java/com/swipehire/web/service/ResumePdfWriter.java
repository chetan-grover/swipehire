package com.swipehire.web.service;

import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfWriter;
import com.lowagie.text.pdf.draw.LineSeparator;
import com.swipehire.common.enums.ResumeItemType;
import com.swipehire.common.tailoring.ResumeItemData;
import com.swipehire.common.tailoring.TailoredResume;
import org.springframework.stereotype.Component;

import java.awt.Color;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Renders a TailoredResume to a one-page PDF with OpenPDF. Matched skills are printed in bold,
 * both in the skills line and wherever they appear inside the applicant's own bullet text.
 */
@Component
public class ResumePdfWriter {

    private static final Color FOREST = new Color(0x2F, 0x4A, 0x35);   // matches the web theme
    private static final Color GREY = new Color(0x6B, 0x73, 0x66);
    private static final Font NAME = new Font(Font.HELVETICA, 20, Font.BOLD, FOREST);
    private static final Font SECTION = new Font(Font.HELVETICA, 11, Font.BOLD, FOREST);
    private static final Font BODY = new Font(Font.HELVETICA, 10, Font.NORMAL);
    private static final Font BODY_BOLD = new Font(Font.HELVETICA, 10, Font.BOLD);
    private static final Font META = new Font(Font.HELVETICA, 9, Font.NORMAL, GREY);
    private static final Font FOOTER = new Font(Font.HELVETICA, 7, Font.ITALIC, GREY);

    /** Applicant details printed at the top of the page. */
    public record Header(String name, String contactLine, String headline, String collegeLine) {
    }

    public void write(Path file, Header header, TailoredResume resume, List<String> orderedSkillNames,
                      List<String> highlightNames, String jobTitle, String companyName) throws IOException {
        Files.createDirectories(file.getParent());
        Document doc = new Document(PageSize.A4, 42, 42, 36, 36);
        try (OutputStream out = Files.newOutputStream(file)) {
            PdfWriter.getInstance(doc, out);
            doc.open();
            Pattern highlight = highlightPattern(highlightNames);

            doc.add(new Paragraph(header.name(), NAME));
            doc.add(new Paragraph(header.contactLine(), META));
            if (header.headline() != null) {
                doc.add(new Paragraph(header.headline(), BODY));
            }
            if (header.collegeLine() != null) {
                doc.add(new Paragraph(header.collegeLine(), META));
            }

            section(doc, "SKILLS");
            Phrase skills = new Phrase();
            for (int i = 0; i < orderedSkillNames.size(); i++) {
                String s = orderedSkillNames.get(i);
                skills.add(new Chunk(s, highlightNames.contains(s) ? BODY_BOLD : BODY));
                if (i < orderedSkillNames.size() - 1) {
                    skills.add(new Chunk("  ·  ", META));
                }
            }
            doc.add(new Paragraph(skills));

            for (TailoredResume.Section s : resume.sections()) {
                section(doc, sectionTitle(s.type()));
                for (TailoredResume.ScoredItem scored : s.items()) {
                    ResumeItemData item = scored.item();
                    Paragraph title = new Paragraph();
                    title.add(new Chunk(item.getTitle(), BODY_BOLD));
                    if (item.getOrganization() != null) {
                        title.add(new Chunk("  —  " + item.getOrganization(), BODY));
                    }
                    if (item.getDateRange() != null && !item.getDateRange().isBlank()) {
                        title.add(new Chunk("    " + item.getDateRange(), META));
                    }
                    title.setSpacingBefore(4);
                    doc.add(title);
                    if (!item.getDescription().isBlank()) {
                        Paragraph desc = new Paragraph(highlighted(item.getDescription(), highlight));
                        desc.setIndentationLeft(10);
                        doc.add(desc);
                    }
                }
            }

            Paragraph footer = new Paragraph("Tailored for " + jobTitle + " at " + companyName
                    + " by SwipeHire. Content was selected and reordered from the applicant's own profile; "
                    + "nothing was written by the system.", FOOTER);
            footer.setSpacingBefore(14);
            doc.add(footer);
            // Close the document BEFORE try-with-resources closes the stream: close() writes the PDF trailer.
            doc.close();
        } catch (DocumentException e) {
            throw new IOException("PDF generation failed", e);
        }
    }

    private static void section(Document doc, String title) throws DocumentException {
        Paragraph p = new Paragraph(title, SECTION);
        p.setSpacingBefore(10);
        doc.add(p);
        doc.add(new Chunk(new LineSeparator(0.5f, 100, FOREST, com.lowagie.text.Element.ALIGN_CENTER, -2)));
    }

    private static String sectionTitle(ResumeItemType type) {
        return switch (type) {
            case EDUCATION -> "EDUCATION";
            case EXPERIENCE -> "EXPERIENCE";
            case PROJECT -> "PROJECTS";
            case ACHIEVEMENT -> "ACHIEVEMENTS";
            case CERTIFICATION -> "CERTIFICATIONS";
        };
    }

    /** One regex matching any highlighted skill name as a whole word, longest names first. */
    static Pattern highlightPattern(List<String> names) {
        if (names.isEmpty()) {
            return null;
        }
        String alternatives = names.stream().sorted(Comparator.comparingInt(String::length).reversed())
                .map(Pattern::quote).collect(Collectors.joining("|"));
        return Pattern.compile("(?i)(?<![A-Za-z0-9])(" + alternatives + ")(?![A-Za-z0-9+#])");
    }

    static Phrase highlighted(String text, Pattern pattern) {
        Phrase phrase = new Phrase();
        if (pattern == null) {
            phrase.add(new Chunk(text, BODY));
            return phrase;
        }
        Matcher m = pattern.matcher(text);
        int last = 0;
        while (m.find()) {
            phrase.add(new Chunk(text.substring(last, m.start()), BODY));
            phrase.add(new Chunk(m.group(), BODY_BOLD));
            last = m.end();
        }
        phrase.add(new Chunk(text.substring(last), BODY));
        return phrase;
    }
}
