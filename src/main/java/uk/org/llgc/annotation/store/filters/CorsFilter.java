package uk.org.llgc.annotation.store.filters;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

import java.io.IOException;

public class CorsFilter implements Filter {

	public void init(final FilterConfig filterConfig) {
	}

	public void doFilter(final ServletRequest pReq, final ServletResponse pRes, final FilterChain pChain) throws IOException, ServletException {
        // Never seems to returng from doFilter so setting CORS headers early. 
        ((HttpServletResponse)pRes).addHeader("Access-Control-Allow-Origin", "*");
        ((HttpServletResponse)pRes).addHeader("Access-Control-Allow-Headers", "X-Requested-With,Content-Type");
        ((HttpServletResponse)pRes).addHeader("Access-Control-Allow-Methods", "GET,PUT,POST,DELETE");

        pChain.doFilter(pReq, pRes);
	}

	public void destroy() {
	}
}
