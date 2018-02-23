package net.eulerframework.startweb;

import net.eulerframework.startweb.entity.User;
import net.eulerframework.startweb.repository.UserRepository;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest
public class StartWebApplicationTests {

	@Autowired
	private UserRepository testRepository;

	@Test
	public void contextLoads() {
		User user = this.testRepository.findUserByUsername("test");
		Assert.assertEquals(user.getId(), "123");
	}

}
