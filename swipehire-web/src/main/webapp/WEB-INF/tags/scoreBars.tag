<%@ tag body-content="empty" description="Explainable score: one bar per component, with its weight" %>
<%@ attribute name="s" required="true" type="com.swipehire.common.dto.ScoreBreakdown" %>
<div class="bars">
    <div class="bar"><span>Required skills (45%)</span><div class="track"><div class="fill" style="width:${s.skillCoveragePercent}%"></div></div><b>${s.skillCoveragePercent}%</b></div>
    <div class="bar"><span>Text similarity (20%)</span><div class="track"><div class="fill" style="width:${s.tfidfPercent}%"></div></div><b>${s.tfidfPercent}%</b></div>
    <div class="bar"><span>Your swipe taste (15%)</span><div class="track"><div class="fill" style="width:${s.preferencePercent}%"></div></div><b>${s.preferencePercent}%</b></div>
    <div class="bar"><span>Location / mode (10%)</span><div class="track"><div class="fill" style="width:${s.locationPercent}%"></div></div><b>${s.locationPercent}%</b></div>
    <div class="bar"><span>Stipend (10%)</span><div class="track"><div class="fill" style="width:${s.stipendPercent}%"></div></div><b>${s.stipendPercent}%</b></div>
</div>
