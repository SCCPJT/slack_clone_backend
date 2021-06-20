package our.yurivongella.instagramclone.service;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import our.yurivongella.instagramclone.controller.dto.auth.SigninReqDto;
import our.yurivongella.instagramclone.controller.dto.auth.SignupReqDto;
import our.yurivongella.instagramclone.controller.dto.auth.TokenResDto;
import our.yurivongella.instagramclone.controller.dto.auth.TokenReqDto;
import our.yurivongella.instagramclone.entity.Member;
import our.yurivongella.instagramclone.repository.MemberRepository;
import our.yurivongella.instagramclone.exception.CustomException;
import our.yurivongella.instagramclone.exception.ErrorCode;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("가입/로그인 테스트")
@SpringBootTest
@Transactional
public class AuthServiceTest {
    @MockBean
    private S3Service s3Service;

    @Autowired
    private AuthService authService;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private final String displayId = "test";
    private final String nickname = "testNickname";
    private final String email = "authService1@test.net";
    private final String password = "1q2w3e4r";

    @BeforeEach
    public void signupBeforeTest() {
        // given
        SignupReqDto signupReqDto = SignupReqDto.builder()
                                                            .displayId(displayId)
                                                            .nickname(nickname)
                                                            .email(email)
                                                            .password(password)
                                                            .build();

        authService.signup(signupReqDto);
    }

    @DisplayName("가입하기")
    @Nested
    class SignupTest {

        @DisplayName("성공")
        @Test
        public void successSignup() {
            // then
            Optional<Member> member = memberRepository.findByEmail(email);
            assertThat(member.isPresent()).isTrue();
            assertThat(member.get().getNickname()).isEqualTo(nickname);
            assertThat(member.get().getDisplayId()).isEqualTo(displayId);
            assertThat(member.get().getEmail()).isEqualTo(email);
            assertThat(
                    passwordEncoder.matches(password, member.get().getPassword())
            ).isTrue();
        }

        @DisplayName("중복 가입해서 실패")
        @Test
        public void failSignup() {
            SignupReqDto signupReqDto = SignupReqDto.builder()
                                                                .displayId("test2")
                                                                .nickname("testNickname2")
                                                                .email(email)
                                                                .password("1q2w3e4r5t")
                                                                .build();
            Assertions.assertThrows(
                    RuntimeException.class,
                    () -> authService.signup(signupReqDto)
            );
        }
    }

    @DisplayName("로그인")
    @Nested
    class LoginTest {

        @DisplayName("로그인 성공")
        @Test
        public void successLogin() {
            // given
            SigninReqDto signinReqDto = SigninReqDto.builder()
                                                                .email(email)
                                                                .password(password)
                                                                .build();
            // when
            TokenResDto tokenResDto = authService.signin(signinReqDto);

            // then
            assertThat(tokenResDto.getAccessToken()).isNotNull();
            assertThat(tokenResDto.getRefreshToken()).isNotNull();
            assertThat(tokenResDto.getGrantType()).isNotNull();
            assertThat(tokenResDto.getAccessTokenExpiresIn()).isNotNull();
        }

        @DisplayName("이메일 불일치로 실패")
        @Test
        public void mismatchEmail() {
            // given
            SigninReqDto signinReqDto = SigninReqDto.builder()
                                                                .email("mismatch" + email)
                                                                .password(password)
                                                                .build();

            // when
            Assertions.assertThrows(
                    RuntimeException.class,
                    () -> authService.signin(signinReqDto)
            );
        }

        @DisplayName("비밀번호 불일치로 실패")
        @Test
        public void mismatchPassword() {
            // given
            SigninReqDto signinReqDto = SigninReqDto.builder()
                                                                .email(email)
                                                                .password(password + "adfsdf")
                                                                .build();

            // when
            Assertions.assertThrows(
                    BadCredentialsException.class,
                    () -> authService.signin(signinReqDto)
            );
        }
    }

    @DisplayName("토큰 재발급")
    @Nested
    class ReissueTest {

        private TokenResDto tokenResDto;

        @BeforeEach
        public void signinBeforeTest() {
            tokenResDto = authService.signin(
                    SigninReqDto.builder()
                                    .email(email)
                                    .password(password)
                                    .build()
            );
        }

        @DisplayName("Access + Refresh 요청해서 성공")
        @Test
        public void accessRefresh() {
            // given
            TokenReqDto tokenReqDto = TokenReqDto.builder()
                                                             .accessToken(tokenResDto.getAccessToken())
                                                             .refreshToken(tokenResDto.getRefreshToken())
                                                             .build();

            // when
            TokenResDto reissue = authService.reissue(tokenReqDto);

            // then
            boolean accessIsLongerThanRefresh = reissue.getAccessToken().length() > reissue.getRefreshToken().length();

            assertThat(accessIsLongerThanRefresh).isTrue();
            assertThat(reissue.getAccessToken()).isInstanceOf(String.class);
            assertThat(reissue.getRefreshToken()).isInstanceOf(String.class);
            assertThat(reissue.getGrantType()).isEqualTo("Bearer");
            assertThat(reissue.getAccessTokenExpiresIn()).isInstanceOf(Long.class);
        }

        @DisplayName("Access + Access 요청해서 실패")
        @Test
        public void accessAccess() {
            // given
            TokenReqDto tokenReqDto = TokenReqDto.builder()
                                                             .accessToken(tokenResDto.getAccessToken())
                                                             .refreshToken(tokenResDto.getAccessToken())
                                                             .build();

            // when
            Assertions.assertThrows(
                    RuntimeException.class,
                    () -> authService.reissue(tokenReqDto)
            );
        }

        @DisplayName("Refresh + Refresh 요청해서 실패")
        @Test
        public void refreshRefresh() {
            // given
            TokenReqDto tokenReqDto = TokenReqDto.builder()
                                                             .accessToken(tokenResDto.getRefreshToken())
                                                             .refreshToken(tokenResDto.getRefreshToken())
                                                             .build();

            // when
            Assertions.assertThrows(
                    RuntimeException.class,
                    () -> authService.reissue(tokenReqDto)
            );
        }

        @DisplayName("Refresh + Access 요청해서 실패")
        @Test
        public void refreshAccess() {
            // given
            TokenReqDto tokenReqDto = TokenReqDto.builder()
                                                             .accessToken(tokenResDto.getRefreshToken())
                                                             .refreshToken(tokenResDto.getAccessToken())
                                                             .build();

            // when
            Assertions.assertThrows(
                    RuntimeException.class,
                    () -> authService.reissue(tokenReqDto)
            );
        }
    }

    @DisplayName("중복 검사")
    @Nested
    class ValidateResource {
        @DisplayName("검사 요청을 한 이메일이 DB에 존재하지 않을 때")
        @Test
        public void check_email_success() {
            boolean validate = authService.validate("adamdoha@naver.com");
            Assertions.assertTrue(validate);
        }

        @DisplayName("검사 요청을 한 이메일이 DB에 이미 존재할 때")
        @Test
        public void check_email_fail() {
            CustomException customException = Assertions.assertThrows(CustomException.class, () -> authService.validate("authService1@test.net"));
            assertEquals(ErrorCode.DUPLICATE_RESOURCE, customException.getErrorCode());
        }

        @DisplayName("검사 요청을 한 displayId가 DB에 존재하지 않을 때")
        @Test
        public void check_displayId_success() {
            boolean validate = authService.validate("adamdoha");
            Assertions.assertTrue(validate);
        }

        @DisplayName("검사 요청을 한 displayId가 DB에 이미 존재할 때")
        @Test
        public void check_displayId_fail() {
            CustomException customException = Assertions.assertThrows(CustomException.class, () -> authService.validate("test"));
            assertEquals(ErrorCode.DUPLICATE_RESOURCE, customException.getErrorCode());
        }
    }

    @DisplayName("activate 테스트")
    @Test
    void activate() {

    }

    @DisplayName("de-activate 테스트")
    @Test
    void deactivate() {

    }

    @DisplayName("delete 테스트")
    @Test
    void delete() {

    }
}
