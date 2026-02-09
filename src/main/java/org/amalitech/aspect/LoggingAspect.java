package org.amalitech.aspect;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Aspect
@Component
public class LoggingAspect {

    private static final Logger logger = LoggerFactory.getLogger(LoggingAspect.class);
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Log all controller methods
     */
    @Pointcut("execution(* org.amalitech.controllers..*(..))")
    public void controllerMethods() {}

    /**
     * Log all service methods
     */
    @Pointcut("execution(* org.amalitech.service..*(..))")
    public void serviceMethods() {}

    /**
     * Log all DAO methods
     */
    @Pointcut("execution(* org.amalitech.dao..*(..))")
    public void daoMethods() {}

    /**
     * Before advice - log method entry
     */
    @Before("controllerMethods() || serviceMethods()")
    public void logMethodEntry(JoinPoint joinPoint) {
        String className = joinPoint.getSignature().getDeclaringTypeName();
        String methodName = joinPoint.getSignature().getName();
        Object[] args = joinPoint.getArgs();

        try {
            logger.info("ENTRY -> {}.{}() with arguments: {}",
                    className, methodName, objectMapper.writeValueAsString(args));
        } catch (Exception e) {
            logger.info("ENTRY -> {}.{}() with arguments: {}",
                    className, methodName, Arrays.toString(args));
        }
    }

    /**
     * AfterReturning advice - log successful method execution
     */
    @AfterReturning(pointcut = "controllerMethods() || serviceMethods()")
    public void logMethodExit(JoinPoint joinPoint) {
        String className = joinPoint.getSignature().getDeclaringTypeName();
        String methodName = joinPoint.getSignature().getName();

        logger.info("EXIT -> {}.{}()", className, methodName);
    }

    /**
     * AfterThrowing advice - log exceptions
     */
    @AfterThrowing(pointcut = "controllerMethods() || serviceMethods() || daoMethods()",
            throwing = "exception")
    public void logException(JoinPoint joinPoint, Throwable exception) {
        String className = joinPoint.getSignature().getDeclaringTypeName();
        String methodName = joinPoint.getSignature().getName();

        logger.error("EXCEPTION in {}.{}(): {} - {}",
                className, methodName, exception.getClass().getSimpleName(), exception.getMessage(), exception);
    }

    /**
     * Around advice for database operations - detailed logging
     */
    @Around("daoMethods()")
    public Object logDatabaseOperation(ProceedingJoinPoint joinPoint) throws Throwable {
        String methodName = joinPoint.getSignature().getName();
        long startTime = System.currentTimeMillis();

        logger.debug("DB QUERY START -> {}", methodName);

        try {
            Object result = joinPoint.proceed();
            long duration = System.currentTimeMillis() - startTime;

            logger.debug("DB QUERY END -> {} completed in {}ms", methodName, duration);

            if (duration > 1000) {
                logger.warn("SLOW QUERY DETECTED -> {} took {}ms", methodName, duration);
            }

            return result;
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            logger.error("DB QUERY FAILED -> {} after {}ms: {}", methodName, duration, e.getMessage());
            throw e;
        }
    }
}