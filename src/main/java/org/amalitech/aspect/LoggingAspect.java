package org.amalitech.aspect;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.amalitech.logging.LogService;

import java.util.Arrays;

@Aspect

public class LoggingAspect {

    private static final Logger logger = LoggerFactory.getLogger(LoggingAspect.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final LogService logService;

    public LoggingAspect(LogService logService) {
        this.logService = logService;
    }

    /**
     * Log all controller methods
     */
    @Pointcut("execution(* org.amalitech.controllers..*(..))")
    public void controllerMethods() {}


    @Pointcut("execution(* org.amalitech.graphqlResolver..*(..))")
    public void resolverMethods() {}

    /**
     * Log all service methods
     */
    @Pointcut("execution(* org.amalitech.service..*(..))")
    public void serviceMethods() {}

    /**
     * Before advice - log method entry
     */
    @Before("controllerMethods() || serviceMethods() || resolverMethods()")
    public void logMethodEntry(JoinPoint joinPoint) {
        String className = joinPoint.getSignature().getDeclaringTypeName();
        String methodName = joinPoint.getSignature().getName();
        Object[] args = joinPoint.getArgs();

        String argsJson;
        try {
            argsJson = objectMapper.writeValueAsString(args);
            logger.info("ENTRY -> {}.{}() with arguments: {}",
                    className, methodName, argsJson);
        } catch (Exception e) {
            argsJson = Arrays.toString(args);
            logger.info("ENTRY -> {}.{}() with arguments: {}",
                    className, methodName, argsJson);
        }
        // also append to in-memory log store
        try {
            logService.append("INFO", logger.getName(), className, methodName, "ENTRY", argsJson, null);
        } catch (Exception ignore) {
            // best effort, do not break application flow
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
        try {
            logService.append("INFO", logger.getName(), className, methodName, "EXIT", null, null);
        } catch (Exception ignore) {
        }
    }

    /**
     * AfterThrowing advice - log exceptions
     */
    @AfterThrowing(pointcut = "controllerMethods() || serviceMethods()",
            throwing = "exception")
    public void logException(JoinPoint joinPoint, Throwable exception) {
        String className = joinPoint.getSignature().getDeclaringTypeName();
        String methodName = joinPoint.getSignature().getName();

        logger.error("EXCEPTION in {}.{}(): {} - {}",
                className, methodName, exception.getClass().getSimpleName(), exception.getMessage(), exception);
        try {
            logService.append("ERROR", logger.getName(), className, methodName, exception.getMessage(), null, exception.toString());
        } catch (Exception ignore) {
        }
    }
}