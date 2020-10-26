package org.dice.ida.logging;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.*;
import org.dice.ida.constant.IDAConst;
import org.dice.ida.model.ChatMessageResponse;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;

@Component
@Aspect
@EnableAspectJAutoProxy
public class AspectLogger {

	@Autowired
	private Logger log;

	@Autowired
	private HttpServletRequest request;

	@Autowired
	private ChatMessageResponse response;

	/**
	 * Creates the point cut for the MessageRestController
	 *
	 */
	@Pointcut("execution(* org.dice.ida.controller.*.*(..))")
	public void controller() {
		//PointCut for MessageRestController
		log.info("[EXECUTION] controller execution started");
	}

	/**
	 * Method to log the Request to Message Rest Controller
	 * @param joinPoint
	 */
	@Before("controller()")
	public void logBeforeMethod(JoinPoint joinPoint) {
		StringBuffer logMessage = new StringBuffer();
		logMessage.append("[REQUEST] - ");
		commonMsgBody(joinPoint, logMessage);
		log.info(logMessage.toString());
	}

	/**
	 * Method to log the exceptions
	 * @param joinPoint
	 * @param exception
	 */
	@AfterThrowing(pointcut = "execution(* org.dice.ida..*(..))", throwing = "exception")
	public void logAfterThrowingMethod(JoinPoint joinPoint, Exception exception) throws Throwable {
		StringBuffer logMessage = new StringBuffer();
		logMessage.append("[EXCEPTION] - ");
		commonMsgBody(joinPoint, logMessage);
		response.setErrCode(1);
		log.error(logMessage.toString(), exception);
	}

	/** Method to log the responses
	 * @param joinPoint
	 * @param retVal
	 */
	@AfterReturning(pointcut = "controller()", returning = "retVal")
	public void logAfterReturningMethod(JoinPoint  joinPoint, Object retVal) {
		StringBuffer logMessage = new StringBuffer();
		logMessage.append("[RESPONSE] - ");
		commonMsgBody(joinPoint, logMessage);
		logMessage.append(" [RETURN VALUE]: ");
		ChatMessageResponse returnBean = (ChatMessageResponse) retVal;
		if(returnBean.getUiAction() == IDAConst.UIA_LOADDS) {
			// We dont want to log full dataset content so that's why
			logMessage.append("[DATASET CONTENT]");
		} else {
			logMessage.append(retVal.toString());
		}
		log.info(logMessage.toString());
	}

	/**
	 * Method to provide basic logging elements for example Remote address,
	 * method name and its parameter
	 *
	 * @param joinPoint
	 * @param logMessage
	 */
	private void commonMsgBody(JoinPoint joinPoint, StringBuffer logMessage) {
		logMessage.append(request.getRemoteAddr());
		logMessage.append(" ");
		logMessage.append(joinPoint.getTarget().getClass().getName());
		logMessage.append(".");
		logMessage.append(joinPoint.getSignature().getName());
		logMessage.append("(");
		Object[] args = joinPoint.getArgs();
		for (int i = 0; i < args.length; i++) {
			logMessage.append(args[i]).append(",");
		}
		if (args.length > 0) {
			logMessage.deleteCharAt(logMessage.length() - 1);
		}

		logMessage.append(")");
	}
}