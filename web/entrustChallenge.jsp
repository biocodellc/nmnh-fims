<%
   response.setHeader( "Pragma", "no-cache" );
   response.setHeader( "Cache-Control", "no-Store,no-Cache" );
   response.setDateHeader( "Expires", 0 );
%>
<%@ page isELIgnored="false" %>
<%@ include file="header-home.jsp" %>

<div class="section">
    <div class="sectioncontent" id="challengeQuestions">
        <h2>Challenge Questions</h2>
        <form method="POST" autocomplete="off">
            <table>
                <tr>
                    <td align="right">${param.question_1}</td>
                    <td><input type="password" name="question_1" autocomplete="off" autofocus></td>
                </tr>
                <tr>
                    <td align="right">${param.question_2}</td>
                    <td><input type="password" name="question_2" autocomplete="off"></td>
                </tr>
                <tr>
                    <td></td>
                    <td><input type="hidden" name="username" value="${param.username}"></td>
                <tr>
                <!-- TODO: get the following jstl to work
                <c:forEach var="par" items="${paramValues}">
                    <c:choose>
                        <c:when test="${par.key.startsWith('question')}">

                        </c:when>
                        <c:otherwise>
                            <tr>
                                <td></td>
                                <td><input type="hidden" name="${par.key}" value="${par.value}"></td>
                            </tr>
                        </c:otherwise>
                    </c:choose>
                </c:forEach>
                --!>
                <tr></tr>
                <tr>
                    <td></td>
                    <td class="error" align="center"></td>
                </tr>
                <tr>
                    <td></td>
                    <td ><input type="button" value="Submit" onclick="challengeResponse();"></td>
                </tr>
            </table>
        </form>
        <p>
            For assistance with your account login or challenge questions call (202) 633-4000 or
            visit <a href="http://myid.si.edu/">http://myid.si.edu/</a>
    </div>
</div>
</div> <!—-End Container—>

<%@ include file="footer.jsp" %>
