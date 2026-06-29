package top.xiamoi.moocpass.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import top.xiamoi.moocpass.annotation.RequireLogin;
import top.xiamoi.moocpass.common.Result;
import top.xiamoi.moocpass.common.UserContext;
import top.xiamoi.moocpass.dto.LoginDTO;
import top.xiamoi.moocpass.dto.RegisterDTO;
import top.xiamoi.moocpass.service.UserService;
import top.xiamoi.moocpass.vo.LoginVO;
import top.xiamoi.moocpass.vo.UserVO;

/**
 * 用户控制器
 */
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
@CrossOrigin
public class UserController {

    private final UserService userService;

    /**
     * 用户注册
     */
    @PostMapping("/register")
    public Result<UserVO> register(@RequestBody RegisterDTO registerDTO) {
        UserVO userVO = userService.register(registerDTO);
        return Result.success(userVO, "注册成功");
    }

    /**
     * 用户登录
     */
    @PostMapping("/login")
    public Result<LoginVO> login(@RequestBody LoginDTO loginDTO) {
        LoginVO loginVO = userService.login(loginDTO);
        return Result.success(loginVO, "登录成功");
    }

    /**
     * 获取当前登录用户信息 (需要登录认证)
     */
    @RequireLogin
    @GetMapping("/info")
    public Result<UserVO> getCurrentUserInfo() {
        Long currentUserId = UserContext.getUserId();
        UserVO userVO = userService.getUserById(currentUserId);
        return Result.success(userVO);
    }
}
