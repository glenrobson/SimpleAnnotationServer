package uk.org.llgc.annotation.store.filters;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletRequest;

import java.io.IOException;

public class CorsFilter implements Filter {

	public void init(final FilterConfig filterConfig) {
	}

	public void doFilter(final ServletRequest pReq, final ServletResponse pRes, final FilterChain pChain) throws IOException, ServletException {
        HttpServletResponse pResponse = (HttpServletResponse)pRes;
        HttpServletRequest pRequest = (HttpServletRequest)pReq;
        // Never seems to returng from doFilter so setting CORS headers early. 
        pResponse.addHeader("Access-Control-Allow-Origin", "*");
        pResponse.addHeader("Access-Control-Allow-Headers", "X-Requested-With,Content-Type");
        pResponse.addHeader("Access-Control-Allow-Methods", "GET,PUT,POST,DELETE");

        if (pRequest.getRequestURI().endsWith("xhtml")) {
            // Don't cache xhtml files
            pResponse.addHeader("Cache-Control", "no-store, max-age=0");
        }

        pChain.doFilter(pReq, pRes);
	}

	public void destroy() {
	}
}
