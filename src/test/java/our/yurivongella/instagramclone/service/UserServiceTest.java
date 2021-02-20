package our.yurivongella.instagramclone.service;

import static org.junit.jupiter.api.Assertions.*;

import javax.persistence.EntityManager;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

import our.yurivongella.instagramclone.controller.dto.UsersRequestDto;

@Transactional
@SpringBootTest
class UserServiceTest {
    @Autowired
    private UserService userService;

    @DisplayName("가입하기")
    @Test
    @Rollback(value = false)
    public void signIn() throws Exception {
        UsersRequestDto usersRequestDto1 = new UsersRequestDto();
        usersRequestDto1.setName("test1");
        UsersRequestDto usersRequestDto2 = new UsersRequestDto();
        usersRequestDto2.setName("test2");
        UsersRequestDto usersRequestDto3 = new UsersRequestDto();
        usersRequestDto3.setName("test3");

        Long aLong = userService.signUp(usersRequestDto1);
        Long aLong1 = userService.signUp(usersRequestDto2);
        Long aLong2 = userService.signUp(usersRequestDto3);

        boolean follow1 = userService.follow(1L, 2L);
        boolean follow2 = userService.follow(1L, 3L);
        boolean follow3 = userService.follow(2L, 1L);
        boolean follow4 = userService.follow(2L, 3L);
        boolean follow5= userService.follow(3L, 1L);
        boolean follow6 = userService.follow(3L, 2L);

        assertEquals(1,aLong);
        assertEquals(2,aLong1);
        assertEquals(3,aLong2);
    }


}