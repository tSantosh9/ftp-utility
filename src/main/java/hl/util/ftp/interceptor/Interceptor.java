package hl.util.ftp.interceptor;

import org.json.JSONObject;

public interface Interceptor {

	/**
	 * 
	 * @param interceptorContext
	 */
	void intercept(JSONObject interceptorContext);
	
}
