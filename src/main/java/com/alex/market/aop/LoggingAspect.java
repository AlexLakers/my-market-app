package com.alex.market.aop;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.springframework.stereotype.Component;

@Aspect
@Slf4j
@Component
public class LoggingAspect {

    @Pointcut("@annotation(com.alex.market.aop.annotation.Loggable)")
    public void annotationPointcut() {
    }

    @Pointcut("execution(public * com.alex.market.controller..*(..))")
    public void controllerPointcut() {
    }

    @Around("annotationPointcut()")
    public Object logMethodExecution(ProceedingJoinPoint joinPoint) throws Throwable {
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = joinPoint.getSignature().getName();
        String fullMethodName = className + "." + methodName;

        log.info("The method: {}() was started with args: {}",
                fullMethodName, java.util.Arrays.toString(joinPoint.getArgs()));
        long startTime = System.currentTimeMillis();

        try {
            Object result = joinPoint.proceed();
            log.info("The method {}() finished successfully . The result: {}",
                    fullMethodName, result != null ? result : "void");

            long executionTime = System.currentTimeMillis() - startTime;

            log.debug("Method {}() is executed for {} ms", fullMethodName, executionTime);
            return result;
        } catch (Exception e) {
            log.error("An error in method{}(): {} has been detected", fullMethodName, e.getMessage(), e);

            throw e;
        }
    }

    @Before("controllerPointcut()")
    public void logBeforeMethodExecution(JoinPoint joinPoint) throws Throwable {
        log.info("-----The endpoint '{}' was started-----", getFullMethodName(joinPoint));

    }

    @After("controllerPointcut()")
    public void logAfterMethodExecution(JoinPoint joinPoint) throws Throwable {
        log.info("-----The endpoint '{}' was finished-----", getFullMethodName(joinPoint));
    }

    private String getFullMethodName(JoinPoint joinPoint) {
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = joinPoint.getSignature().getName();
        return className + "." + methodName;
    }
}



