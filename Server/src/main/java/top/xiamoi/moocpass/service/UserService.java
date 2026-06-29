package top.xiamoi.moocpass.service;

import com.baomidou.mybatisplus.extension.service.IService;
import top.xiamoi.moocpass.dto.LoginDTO;
import top.xiamoi.moocpass.dto.RegisterDTO;
import top.xiamoi.moocpass.entity.User;
import top.xiamoi.moocpass.vo.LoginVO;
import top.xiamoi.moocpass.vo.UserVO;

/**
 * 用户服务接口
 */
public interface UserService extends IService<User> {

    /**
     * 用户注册
     */
    UserVO register(RegisterDTO registerDTO);

    /**
     * 用户登录
     */
    LoginVO login(LoginDTO loginDTO);

    /**
     * 获取指定用户信息
     */
    UserVO getUserById(Long userId);
}
