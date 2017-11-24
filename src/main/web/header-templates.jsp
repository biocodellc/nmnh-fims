<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<%
   response.setHeader( "Pragma", "no-cache" );
   response.setHeader( "Cache-Control", "no-cache" );
   response.setDateHeader( "Expires", 0 );
%>

<!DOCTYPE HTML>

<html>
    <head>
    <title>NMNH FIMS Template Generator</title>

    <link rel="stylesheet" type="text/css" href="/css/jquery-ui.css" />
    <link rel="stylesheet" type="text/css" href="/css/bootstrap.min.css">
    <link rel="stylesheet" type="text/css" href="/css/alerts.css"/>
    <link rel="stylesheet" type="text/css" href="/css/biscicol.css"/>

    <script>var sessionMaxInactiveInterval = ${pageContext.session.maxInactiveInterval}</script>
    <script type="text/javascript" src="/js/jquery.js"></script>
    <script type="text/javascript" src="/js/jquery-ui.min.js"></script>
    <script type="text/javascript" src="/js/jquery.form.js"></script>
    <script type="text/javascript" src="/js/BrowserDetect.js"></script>

    <script type="text/javascript" src="/js/nmnh-fims.js"></script>
    <script type="text/javascript" src="/js/bootstrap.min.js"></script>

    <link rel="short icon" href="/docs/images/fimsicon.png" />

</head>

<body>

<%@ include file="header-menus.jsp" %>
