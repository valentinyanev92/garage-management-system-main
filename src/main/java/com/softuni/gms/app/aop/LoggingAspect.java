package com.softuni.gms.app.aop;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.*;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Arrays;

@Aspect
@Component
@Slf4j
public class LoggingAspect {

    @Pointcut("execution(* com.softuni.gms.app..service..*(..))")
    public void allServiceMethods() {

    }

    @Before("allServiceMethods()")
    public void beforeAllServiceMethods(JoinPoint joinPoint) {

        String method = joinPoint.getSignature().toShortString();
        Object[] args = joinPoint.getArgs();

        log.info("[AOP] Starting method {} with arguments: {}", method, Arrays.toString(args));
    }

    @AfterReturning(pointcut = "allServiceMethods()", returning = "result")
    public void afterAllServiceMethods(JoinPoint joinPoint, Object result) {

        String method = joinPoint.getSignature().toShortString();
        String resultString = formatResult(result);

        log.info("[AOP] Finished method {} ,Result: {}", method, resultString);
    }

    @AfterThrowing(pointcut = "allServiceMethods()", throwing = "exc")
    public void afterThrowServiceMethods(JoinPoint joinPoint, Throwable exc){

        log.info("[AOP] exception in method: {} -> {}", joinPoint.getSignature(), exc.getMessage());
    }

    private String formatResult(Object result) {

        if (result == null) return "null";

        try {
            Method getId = result.getClass().getMethod("getId");
            Object id = getId.invoke(result);
            return result.getClass().getSimpleName() + "(id=" + id + ")";
        } catch (Exception e) {
            return result.toString();
        }
    }
}
