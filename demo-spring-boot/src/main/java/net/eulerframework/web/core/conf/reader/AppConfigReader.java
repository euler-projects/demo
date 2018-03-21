package net.eulerframework.web.core.conf.reader;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Date;

@ConfigurationProperties(prefix="config")
public class AppConfigReader {

    private final Test test = new Test();

    private Date date;

    private Long longValue = Long.MAX_VALUE;

    private Integer intValue = Integer.MAX_VALUE;

    public Test getTest() {
        return test;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public Long getLongValue() {
        return longValue;
    }

    public void setLongValue(Long longValue) {
        this.longValue = longValue;
    }

    public Integer getIntValue() {
        return intValue;
    }

    public void setIntValue(Integer intValue) {
        this.intValue = intValue;
    }
}
