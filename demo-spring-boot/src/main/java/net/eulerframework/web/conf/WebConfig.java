package net.eulerframework.web.conf;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class WebConfig {

    private class WebConfigKey {
        private final static String TEST_INT = "config.test.int";
    }

    private class WebConfigDefault {
        private final static int TEST_INT = 101;
    }

    @Component
    public class WebConfigBean {

        @Value("${" + WebConfigKey.TEST_INT + ":" + WebConfigDefault.TEST_INT + "}")
        private int value;
    }

    private static WebConfigBean webConfigBean;

    @Autowired
    public void setWebConfigBean(WebConfigBean webConfigBean) {
        WebConfig.webConfigBean = webConfigBean;
    }



    public static int getValue() {
        return webConfigBean.value;
    }
}
