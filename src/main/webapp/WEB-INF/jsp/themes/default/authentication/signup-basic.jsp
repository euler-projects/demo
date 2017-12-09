<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html lang="en">

<head>

<meta charset="utf-8">
<meta http-equiv="X-UA-Compatible" content="IE=edge">
<meta name="viewport" content="width=device-width, initial-scale=1">
<link href="${__ASSETS_PATH}/lib/bootstrap/css/bootstrap.min.css"
    rel="stylesheet">
<link href="${__ASSETS_PATH}/lib/loader/loaders.min.css"
    rel="stylesheet">
<link href="${__ASSETS_PATH}/css/themes/default/demo/single-form.css"
    rel="stylesheet">

<style>
.loading {
    border: 2px solid #666;
    border-top-color: transparent;
    border-radius: 100%;
    width: 14px;
    height: 14px;
    margin: 10px;
    animation: circle infinite 1s linear;
}

.has-error .form-control-feedback-msg {
    color: #a94442;
}

.form-control-feedback-msg {
    display: block;
    margin-bottom: -15px;
    line-height: 15px;
    font-size: 0.6em;
}

.captcha-group .form-control {
    width: 60%;
    display: inline-block;
}

.captcha-group label {
    display: block;
}

.captcha-group .captcha {
    display: inline-block;
    width: 40%;
    text-align: right;
}

.captcha-group .form-control-feedback {
    right: 120px;
}
</style>

<title>${__SITENAME}</title>

</head>
<body>
    <div class="wrapper">
        <div class="wrapper-inner">
            <div class="title-wrapper">
                <span class="">新用户注册</span>
            </div>
            <div class="form-wrapper">
                <form action="${__CONTEXT_PATH}/signup-basic" id="main-form" class="full-line-form" method="post">
                    <div class="form-group has-feedback">
                        <label class="control-label" for="username">${e:i18n('_USERNAME')}</label><input
                            class="form-control" type="text" id="username" name="username" />
                    </div>
                    <div class="form-group has-feedback">
                        <label class="control-label" for="password">${e:i18n('_PASSWORD')}</label><input
                            class="form-control" type="password" id="password" name="password" />
                    </div>
                    <div class="form-group has-feedback">
                        <label class="control-label" for="confirm-password">${e:i18n('_CONFIRM_PASSWORD')}</label><input
                            class="form-control" type="password" id="confirm-password"/>
                    </div>
                    <div class="form-group">
                        <label class="control-label" for="givenName">${e:i18n('_GIVEN_NAME')}</label><input
                            class="form-control" type="text" id="givenName" name="givenName" />
                    </div>
                    <div class="form-group">
                        <label class="control-label" for="familyName">${e:i18n('_FAMILY_NAME')}</label><input
                            class="form-control" type="text" id="familyName" name="familyName" />
                    </div>
                    <div class="form-group captcha-group has-feedback">
                        <label class="control-label" for="captcha">${e:i18n('_CAPTCHA')}</label><input
                            class="form-control" type="text" id="captcha" name="captcha"/><span class="captcha"><a href="javascript:void(0)"><img 
                            src="${__CONTEXT_PATH}/captcha/simple" 
                            onclick="this.src='${__CONTEXT_PATH}/captcha/simple?_r=' + new Date().getTime()"></a></span>
                    </div>
                    <div class="form-group">
                        <button type="button" class="btn btn-success" onclick="signup()">${e:i18n('_SIGN_UP')}</button>
                    </div>
                </form>
            </div>
        </div>
        <footer
            class="navbar navbar-inverse navbar-fixed-bottom footer-wrapper">
            <span><a href="${__CONTEXT_PATH}/">${__SITENAME}<%-- &nbsp;${__PROJECT_VERSION} --%></a></span>
            <span>&copy;<fmt:formatDate value="${__NOW}"
                    pattern="yyyy" />&nbsp;${__COPYRIGHT_HOLDER}
            </span>
        </footer>
    </div>
    <script src="${__ASSETS_PATH}/lib/jquery/jquery-1.12.4.min.js"></script>
    <script src="${__ASSETS_PATH}/lib/bootstrap/js/bootstrap.min.js"></script>
    
<!--     <span class="glyphicon glyphicon-ok form-control-feedback"></span>
    <span class="glyphicon glyphicon-remove form-control-feedback"></span> -->
                        
    <script>
        var eulerForm = new Object();
        eulerForm.setLoadStatus = function(element) {
            this.clearStatus(element);
            $(element).parent().append('<span class="form-control-feedback"><div class="loading"></div></span>');
        };
        eulerForm.setSuccessStatus = function(element) {
            this.clearStatus(element);
            $(element).parent().addClass('has-success');
            $(element).parent().append('<span class="glyphicon glyphicon-ok form-control-feedback"></span>');
        };
        eulerForm.setErrorStatus = function(element, msg) {
            this.clearStatus(element);
            $(element).parent().addClass('has-error');
            $(element).parent().append('<span class="glyphicon glyphicon-remove form-control-feedback"></span><span class="form-control-feedback-msg">' + msg + '</span>');
        };
        eulerForm.clearStatus = function(element) {
            $(element).parent().removeClass('has-error has-success');
            var objs = $(element).nextAll();
            for(var i = 0;i < objs.length; i++) {
                if($(objs[i]).hasClass("form-control-feedback") === true ||
                        $(objs[i]).hasClass("form-control-feedback-msg") === true) {
                    $(objs[i]).remove();
                }
            }
        };
        eulerForm.validForm = function(element) {
            var objs = $(element).children('.form-group.has-error');
            if(typeof(objs) == 'undefined' || objs == null || objs.length == 0) {
                return true;
            }
            return false;
        };
    
        $("#username").blur(function(){
            var input = this;
            eulerForm.setLoadStatus(input);

            $.ajax({
                url:'${__CONTEXT_PATH}/ajax/validUsername',
                type:'GET',
                async:true,
                data: {username: $(input).val()},
                error:function(XMLHttpRequest, textStatus, errorThrown) {
                    var response = JSON.parse(XMLHttpRequest.responseText);
                    eulerForm.setErrorStatus(input, response.error_description);
                },
                success:function(data, textStatus) {
                    eulerForm.setSuccessStatus(input);
                }
            });
        });
        
        $("#password").blur(function(){
            var input = this;
            eulerForm.setLoadStatus(input);

            $.ajax({
                url:'${__CONTEXT_PATH}/ajax/validPassword',
                type:'GET',
                async:true,
                data: {password: $(input).val()},
                error:function(XMLHttpRequest, textStatus, errorThrown) {
                    var response = JSON.parse(XMLHttpRequest.responseText);
                    eulerForm.setErrorStatus(input, response.error_description);
                },
                success:function(data, textStatus) {
                    eulerForm.setSuccessStatus(input);
                }
            });
        });
        
        $("#confirm-password").blur(function(){
            if($(this).val() == null || $(this).val() == "") {
                return;
            }
            
            if($(this).val() === $("#password").val()) {
                eulerForm.setSuccessStatus(this);
            } else {
                eulerForm.setErrorStatus(this, "${e:i18n('_CONFIRM_PASSWORD_NOT_MATCH')}");
            }
        });
        
        $("#captcha").blur(function(){
            var input = this;
            eulerForm.setLoadStatus(input);

            $.ajax({
                url:'${__CONTEXT_PATH}/ajax/validCaptcha',
                type:'GET',
                async:true,
                data: {captcha: $(input).val()},
                error:function(XMLHttpRequest, textStatus, errorThrown) {
                    var response = JSON.parse(XMLHttpRequest.responseText);
                    eulerForm.setErrorStatus(input, response.error_description);
                },
                success:function(data, textStatus) {
                    eulerForm.setSuccessStatus(input);
                }
            });
        });
        
        function signup() {
            if(!eulerForm.validForm('#main-form')) {
                return;
            }
            $('#main-form').submit();
        }
        
    </script>
</body>
</html>