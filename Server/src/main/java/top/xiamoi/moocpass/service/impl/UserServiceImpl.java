package top.xiamoi.moocpass.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import top.xiamoi.moocpass.dto.LoginDTO;
import top.xiamoi.moocpass.dto.RegisterDTO;
import top.xiamoi.moocpass.entity.User;
import top.xiamoi.moocpass.mapper.UserMapper;
import top.xiamoi.moocpass.service.UserService;
import top.xiamoi.moocpass.utils.JwtUtils;
import top.xiamoi.moocpass.utils.PasswordUtils;
import top.xiamoi.moocpass.vo.LoginVO;
import top.xiamoi.moocpass.vo.UserVO;

import java.time.LocalDateTime;

/**
 * 用户服务实现类
 */
@Service
@RequiredArgsConstructor
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    private final JwtUtils jwtUtils;

    @Override
    public UserVO register(RegisterDTO registerDTO) {
        if (registerDTO == null || !StringUtils.hasText(registerDTO.getUsername()) || !StringUtils.hasText(registerDTO.getPassword())) {
            throw new RuntimeException("用户名和密码不能为空");
        }

        // 校验用户名是否重复
        long usernameCount = this.count(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, registerDTO.getUsername()));
        if (usernameCount > 0) {
            throw new RuntimeException("用户名已存在");
        }

        // 校验邮箱是否重复
        if (StringUtils.hasText(registerDTO.getEmail())) {
            long emailCount = this.count(new LambdaQueryWrapper<User>()
                    .eq(User::getEmail, registerDTO.getEmail()));
            if (emailCount > 0) {
                throw new RuntimeException("邮箱已存在");
            }
        }

        // 默认昵称
        String nickname = StringUtils.hasText(registerDTO.getNickname()) ? registerDTO.getNickname() : registerDTO.getUsername();

        // 密码加密
        String encryptedPassword = PasswordUtils.encrypt(registerDTO.getPassword());

        User user = User.builder()
                .username(registerDTO.getUsername())
                .nickname(nickname)
                .password(encryptedPassword)
                .email(registerDTO.getEmail())
                .createTime(LocalDateTime.now())
                .updateTime(LocalDateTime.now())
                .build();

        this.save(user);

        return convertToVO(user);
    }

    @Override
    public LoginVO login(LoginDTO loginDTO) {
        if (loginDTO == null || !StringUtils.hasText(loginDTO.getUsername()) || !StringUtils.hasText(loginDTO.getPassword())) {
            throw new RuntimeException("用户名和密码不能为空");
        }

        // 支持用户名或邮箱登录
        User user = this.getOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, loginDTO.getUsername())
                .or()
                .eq(User::getEmail, loginDTO.getUsername()));

        if (user == null) {
            throw new RuntimeException("用户不存在或密码错误");
        }

        // 验证密码
        if (!PasswordUtils.verify(loginDTO.getPassword(), user.getPassword())) {
            throw new RuntimeException("用户不存在或密码错误");
        }

        // 生成 JWT Token
        String token = jwtUtils.generateToken(user.getId());

        return LoginVO.builder()
                .token(token)
                .user(convertToVO(user))
                .build();
    }

    @Override
    public UserVO getUserById(Long userId) {
        if (userId == null) {
            throw new RuntimeException("用户ID不能为空");
        }
        User user = this.getById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        return convertToVO(user);
    }

    private UserVO convertToVO(User user) {
        return UserVO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .nickname(user.getNickname())
                .email(user.getEmail())
                .createTime(user.getCreateTime())
                .build();
    }
}
