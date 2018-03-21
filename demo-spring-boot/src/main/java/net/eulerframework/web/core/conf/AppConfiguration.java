package net.eulerframework.web.core.conf;

import net.eulerframework.web.core.conf.reader.AppConfigReader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(AppConfigReader.class)
public class AppConfiguration {

    private static AppConfigReader reader;

    @Autowired
    public void setReader(AppConfigReader reader) {
        AppConfiguration.reader = reader;
    }

    public static AppConfigReader getReader() {
        return reader;
    }
}
