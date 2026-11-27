package com.school.material.sys.aspect;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.school.material.common.Result;
import com.school.material.sys.entity.SysOperationLog;
import com.school.material.sys.service.SysOperationLogService;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 全局操作日志切面：拦截所有 @RestController 的增删改请求，自动写入 mat_operation_log。
 * <p>
 * 日志写入失败不得影响业务返回，因此写库异常只记录 warn 日志。
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class OperationLogAspect {

    private static final Set<String> WRITE_METHODS = Set.of("POST", "PUT", "DELETE", "PATCH");
    private static final int MAX_PARAM_LENGTH = 2000;
    private static final int MAX_DETAIL_LENGTH = 500;
    private static final String ERROR_SEPARATOR = " | 失败: ";
    private static final Pattern PASSWORD_PATTERN =
            Pattern.compile("(\"password\"\\s*:\\s*\")[^\"]*(\")", Pattern.CASE_INSENSITIVE);

    /** 控制器基础路径 → 业务模块名称 */
    private static final Map<String, String> MODULE_NAMES = Map.ofEntries(
            Map.entry("/categories", "物资分类"),
            Map.entry("/materials", "物资档案"),
            Map.entry("/warehouses", "仓库管理"),
            Map.entry("/stock", "库存管理"),
            Map.entry("/stocks", "库存管理"),
            Map.entry("/purchase", "发票登记"),
            Map.entry("/stock-apply", "库存申请"),
            Map.entry("/sys/user", "用户管理"),
            Map.entry("/sys/dept", "部门管理"),
            Map.entry("/sys/role", "角色管理"),
            Map.entry("/sys/log", "操作日志")
    );

    private final SysOperationLogService sysOperationLogService;
    private final ObjectMapper objectMapper;

    @Around("@within(org.springframework.web.bind.annotation.RestController)")
    public Object record(ProceedingJoinPoint joinPoint) throws Throwable {
        HttpServletRequest request = currentRequest();
        if (request == null || !WRITE_METHODS.contains(request.getMethod())) {
            return joinPoint.proceed();
        }

        long start = System.currentTimeMillis();
        Object result = null;
        Throwable error = null;
        try {
            result = joinPoint.proceed();
            return result;
        } catch (Throwable ex) {
            error = ex;
            throw ex;
        } finally {
            try {
                saveLog(joinPoint, request, result, error, System.currentTimeMillis() - start);
            } catch (Exception ex) {
                log.warn("操作日志写入失败: {}", ex.getMessage());
            }
        }
    }

    private void saveLog(ProceedingJoinPoint joinPoint, HttpServletRequest request,
                         Object result, Throwable error, long costMs) {
        String params = serializeArgs(joinPoint);
        String errorMsg = null;
        int status = 1;
        if (error != null) {
            status = 0;
            errorMsg = error.getMessage() == null ? error.getClass().getSimpleName() : error.getMessage();
        } else if (result instanceof Result<?> res && res.getCode() != 0) {
            status = 0;
            errorMsg = res.getMsg();
        }

        String detail = errorMsg == null ? params : params + ERROR_SEPARATOR + errorMsg;

        SysOperationLog entity = new SysOperationLog();
        entity.setOperatorId(0L);
        entity.setUserName(truncate(resolveOperator(request), 30));
        entity.setModule(truncate(resolveModule(joinPoint), 50));
        entity.setAction(truncate(resolveOperationType(request), 50));
        entity.setTarget(truncate(request.getMethod() + " " + request.getRequestURI(), 120));
        entity.setDetail(truncate(detail, MAX_DETAIL_LENGTH));
        entity.setIp(truncate(resolveIp(request), 40));
        entity.setResult(status);
        entity.setCostMs(costMs);
        entity.setOperateTime(LocalDateTime.now());

        sysOperationLogService.save(entity);
    }

    private String resolveOperator(HttpServletRequest request) {
        String operator = request.getHeader("X-User-No");
        if (!StringUtils.hasText(operator)) {
            operator = request.getHeader("X-User-Name");
        }
        if (!StringUtils.hasText(operator)) {
            return "系统";
        }
        return decodeName(operator.trim());
    }

    /** 前端以 encodeURIComponent 传递中文操作人，这里按 UTF-8 还原 */
    private String decodeName(String value) {
        try {
            return java.net.URLDecoder.decode(value, java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception ex) {
            return value;
        }
    }

    private String resolveModule(ProceedingJoinPoint joinPoint) {
        RequestMapping mapping = AnnotatedElementUtils.findMergedAnnotation(
                joinPoint.getTarget().getClass(), RequestMapping.class);
        if (mapping != null && mapping.value().length > 0) {
            String basePath = mapping.value()[0];
            String module = MODULE_NAMES.get(basePath);
            if (StringUtils.hasText(module)) {
                return module;
            }
            return basePath.replace("/", "");
        }
        return joinPoint.getTarget().getClass().getSimpleName();
    }

    private String resolveOperationType(HttpServletRequest request) {
        String method = request.getMethod();
        if ("DELETE".equals(method)) {
            return "删除";
        }
        if ("PUT".equals(method) || "PATCH".equals(method)) {
            return "修改";
        }
        String uri = request.getRequestURI();
        if (uri.contains("/audit")) {
            return "审核";
        }
        if (uri.contains("/assign-role")) {
            return "分配角色";
        }
        if (uri.contains("/register")) {
            return "登记";
        }
        if (uri.contains("/submit")) {
            return "提交";
        }
        return "新增";
    }

    private String serializeArgs(ProceedingJoinPoint joinPoint) {
        List<Object> params = new ArrayList<>();
        for (Object arg : joinPoint.getArgs()) {
            if (arg == null || arg instanceof ServletRequest || arg instanceof ServletResponse
                    || arg instanceof MultipartFile || arg instanceof BindingResult) {
                continue;
            }
            params.add(arg);
        }
        try {
            String json = objectMapper.writeValueAsString(params);
            json = PASSWORD_PATTERN.matcher(json).replaceAll("$1******$2");
            return truncate(json, MAX_PARAM_LENGTH);
        } catch (Exception ex) {
            return "[参数序列化失败]";
        }
    }

    private String resolveIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(forwarded)) {
            return forwarded.split(",")[0].trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (StringUtils.hasText(realIp)) {
            return realIp.trim();
        }
        return request.getRemoteAddr();
    }

    private String truncate(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        return value.length() > maxLength
                ? value.substring(0, Math.max(0, maxLength - 3)) + "..."
                : value;
    }

    private HttpServletRequest currentRequest() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
            return attributes.getRequest();
        }
        return null;
    }
}
