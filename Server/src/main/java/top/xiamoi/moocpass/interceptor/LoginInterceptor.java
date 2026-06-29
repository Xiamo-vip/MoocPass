package top.xiamoi.moocpass.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import tools.jackson.databind.ObjectMapper;
import top.xiamoi.moocpass.annotation.RequireLogin;
import top.xiamoi.moocpass.common.Result;
import top.xiamoi.moocpass.common.UserContext;
import top.xiamoi.moocpass.utils.JwtUtils;

/**
 * 鉴权拦截器
 */
@Component
@RequiredArgsConstructor
public class LoginInterceptor implements HandlerInterceptor {

    private final JwtUtils jwtUtils;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }

        RequireLogin requireLogin = handlerMethod.getMethodAnnotation(RequireLogin.class);
        if (requireLogin == null) {
            requireLogin = handlerMethod.getBeanType().getAnnotation(RequireLogin.class);
        }

        String token = request.getHeader("Authorization");
        if (StringUtils.hasText(token) && token.startsWith("Bearer ")) {
            token = token.substring(7);
        } else if (!StringUtils.hasText(token)) {
            token = request.getHeader("token");
        }

        boolean validToken = StringUtils.hasText(token) && jwtUtils.validateToken(token);

        if (validToken) {
            Long userId = jwtUtils.getUserIdFromToken(token);
            UserContext.setUserId(userId);
        }

        if (requireLogin != null && requireLogin.required()) {
            if (!validToken) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write(objectMapper.writeValueAsString(Result.error(401, "未登录或登录凭证已过期")));
                return false;
            }
        }

        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex){
        UserContext.remove();
    }
}
