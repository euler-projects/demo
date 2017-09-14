<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html lang="en">

<head>

<meta charset="utf-8">
<meta http-equiv="X-UA-Compatible" content="IE=edge">

<title>${__SITENAME}</title>

</head>
<body>
    <form action="${__CONTEXT_PATH}/settings/account/change-password" method="post">
        <p>
            <label for="old-pass">Old Password</label> <input type="password"
                id="old-pass" name="oldPassword">
        </p>
        <p>
            <label for="new-pass">New Password</label> <input
                type="password" id="new-pass" name="newPassword">
        </p>
        <button type="submit">Change Password</button>
    </form>
</body>
</html>