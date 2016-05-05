package biocode.fims.rest;

import org.eclipse.jetty.http.HttpStatus;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * redirect requests to the new resolver location
 */
public class RedirectFilter implements Filter {

    @Override
    public void init(FilterConfig config) throws ServletException {
        //
    }

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) throws ServletException, IOException {
        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) res;
        String requestURI = request.getRequestURI();

        if (requestURI.matches("^/fims/rest/ark:/[0-9]{5}/.*$") || requestURI.matches("^/fims/rest/metadata/ark:/[0-9]{5}.*$")) {
            String newURI = "/fims/id/" + requestURI.substring(requestURI.lastIndexOf("rest/") + 5, requestURI.length());
            response.setStatus(HttpStatus.MOVED_PERMANENTLY_301);
            response.sendRedirect(newURI);
        } else {
            chain.doFilter(req, res);
        }
    }

    @Override
    public void destroy() {
        //
    }
}
