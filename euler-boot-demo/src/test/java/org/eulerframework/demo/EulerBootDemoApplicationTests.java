package org.eulerframework.demo;

import org.eulerframework.boot.autoconfigure.support.web.core.EulerBootPropertySource;
import org.eulerframework.common.util.property.PropertyNotFoundException;
import org.eulerframework.web.config.WebConfig.WebConfigKey;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.time.Duration;
import java.util.Locale;

@RunWith(SpringRunner.class)
@SpringBootTest
public class EulerBootDemoApplicationTests {

	@Autowired
	public EulerBootPropertySource eulerBootPropertySource;

	@Test
	public void contextLoads() {
	}

	@Test
	public void getDuration() throws PropertyNotFoundException {
		Assert.assertEquals(Duration.class, eulerBootPropertySource.getProperty(WebConfigKey.CORE_CACHE_RAM_CACHE_POOL_CLEAN_FREQ, Duration.class).getClass());
		Assert.assertEquals(Duration.ofHours(2), eulerBootPropertySource.getProperty(WebConfigKey.CORE_CACHE_RAM_CACHE_POOL_CLEAN_FREQ, Duration.class));
	}
	@Test
	public void getLocale() throws PropertyNotFoundException {
		Assert.assertEquals(Locale.class, eulerBootPropertySource.getProperty(WebConfigKey.WEB_LANGUAGE_DEFAULT, Locale.class).getClass());
		Assert.assertEquals(Locale.UK, eulerBootPropertySource.getProperty(WebConfigKey.WEB_LANGUAGE_DEFAULT, Locale.class));
	}

	@Test
	public void getLocaleArray() throws PropertyNotFoundException {
		Assert.assertEquals(Locale[].class, eulerBootPropertySource.getProperty(WebConfigKey.WEB_LANGUAGE_SUPPORT_LANGUAGES, Locale[].class).getClass());
		Assert.assertArrayEquals(new Locale[]{Locale.UK, Locale.CHINA, Locale.TAIWAN, Locale.US}, eulerBootPropertySource.getProperty(WebConfigKey.WEB_LANGUAGE_SUPPORT_LANGUAGES, Locale[].class));
	}
}
