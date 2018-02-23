<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html lang="en" style="margin:0;">

<head>

<meta charset="utf-8">
<meta http-equiv="X-UA-Compatible" content="IE=edge">

<title>${__SITENAME}</title>

</head>
<body style="max-width:800px; margin:0 auto; background-color:#eee;">
<div style="background-color:#fff;padding:20px;">
<h1 style="text-align:center;margin-top:0;">${post.title}</h1>
<p style="text-align:center;">${post.authorUsername}</p>
<p style="text-align:center;">${post.updateDate}</p>
<p>${post.content}</p>
</div>
</body>
</html>