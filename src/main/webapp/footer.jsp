<%@ page language="java" import="gov.nysenate.openleg.util.JSPHelper, java.util.*, java.text.*,java.io.*,gov.nysenate.openleg.*,gov.nysenate.openleg.model.*" pageEncoding="UTF-8"%>
<%
    String searchType = (String)request.getAttribute("type");
    if (searchType == null) {
        searchType = "";
    }
%>
<div id="content" >
    <div id="footer">
        <div class="footer-part footer-part-first">
            <ul>
                <li class="head">Open Legislation</li>
                <%if (searchType.startsWith("bill")||searchType.equals("search")||searchType.equals("sponsor")||searchType.equals("committee")){ %>
                <li><a href="<%=JSPHelper.getLink(request, "/bills/")%>"  class="linkActivated" title="Browse and search Senate and Assembly bills by number, keyword, sponsor and more">Bills</a></li>
                <%}else{ %>
                <li><a href="<%=JSPHelper.getLink(request, "/bills/")%>" title="Browse and search Senate and Assembly bills by number, keyword, sponsor and more">Bills</a></li>
                <%} %>
                <li><a href="<%=JSPHelper.getLink(request, "/resolutions/")%>" <%if (searchType.startsWith("resolution")) {%>class="linkActivated"<%} %> title="View senate and assembly resolutions.">Resolutions</a></li>
                <li><a href="<%=JSPHelper.getLink(request, "/calendars/")%>"  <%if (searchType.startsWith("calendar")){%>class="linkActivated"<%} %> title="View recent and search floor calendars and active lists by number or date (i.e. 11/07/2009)">Calendars</a></li>
                <li><a href="<%=JSPHelper.getLink(request, "/meetings/")%>"  <%if (searchType.startsWith("meeting")){%>class="linkActivated"<%} %> title="View upcoming and recent committee meetings, and search by committee, chairperson, location, date (i.e. 11/07/2009) and more.">Meetings</a></li>
                <li><a href="<%=JSPHelper.getLink(request, "/transcripts/")%>" <%if (searchType.startsWith("transcript")){%>class="linkActivated"<%} %> title="View and search Senate floor full text transcripts">Transcripts</a></li>
                <li><a href="<%=JSPHelper.getLink(request, "/actions/")%>"  <%if (searchType.startsWith("action")){%>class="linkActivated"<%} %> title="View and filter Floor Actions on Bills from the Floor of the Senate">Actions</a>
                <li><a href="<%=JSPHelper.getLink(request, "/senators")%>">Browse by Sponsor</a></li>
                <li><a href="<%=JSPHelper.getLink(request, "/committees")%>">Browse by Committee</a></li>
            </ul>
        </div>
        <div class="footer-part footer-part-second">
            <ul>
                <li class="head">Connect</li>
                <li><a href="http://billbuzz.nysenate.gov">BillBuzz</a></li>
                <li><a href="<%=JSPHelper.getLink(request, "/comments/")%>">View Comments</a></li>
                <li><a href="<%=JSPHelper.getLink(request, "/feedback")%>">Feedback</a></li>
                <li><a href="<%=JSPHelper.getLink(request, "/developers")%>">Developers</a></li>
            </ul>
        </div>
        <div class="footer-part footer-part-third">
            <ul>
                <li class="head">NYSenate.gov</li>
                <li><a href="http://www.nysenate.gov/senators">Senators</a></li>
                <li><a href="http://www.nysenate.gov/committees">Committees</a></li>
                <li><a href="http://www.nysenate.gov/issues-initiatives">Issues &amp; Initiatives</a></li>
                <li><a href="http://www.nysenate.gov/newsroom">Newsroom</a></li>
                <li><a href = "http://www.nysenate.gov/privacy-policy">Privacy Policy</a></li>
            </ul>
        </div>

        <div class="footer-part footer-part-fourth">
            <span><img class="cc-logo" align="left" src="http://i.creativecommons.org/l/by-nc-nd/3.0/us/88x31.png" alt="Creative Commons License"></span><br/><br/>
            <br/><p>This content is licensed under <a rel="license" href="http://creativecommons.org/licenses/by-nc-nd/3.0/us/">Creative Commons BY-NC-ND 3.0</a>. Permissions beyond the scope of this license are available <a cc="http://creativecommons.org/ns#" href="http://www.nysenate.gov/copyright-policy" rel="morePermissions">here</a>.</p>
             <p>The <a href="https://github.com/nysenate/OpenLegislation">software</a> and <a href="http://openlegislation.readthedocs.org/en/latest/">services</a> provided under this site are offered under the BSD License and the GPL v3 License.</p>
         </div>
    </div>
</div>
<script type="text/javascript">
    var gaJsHost = (("https:" == document.location.protocol) ? "https://ssl." : "http://www.");
    document.write(unescape("%3Cscript src='" + gaJsHost + "google-analytics.com/ga.js' type='text/javascript'%3E%3C/script%3E"));

    try {
        var pageTracker = _gat._getTracker("UA-8171983-6");
        pageTracker._trackPageview();
    } catch(err) {}
</script>
</body>
</html>
