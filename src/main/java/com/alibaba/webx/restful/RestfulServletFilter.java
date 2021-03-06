package com.alibaba.webx.restful;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.UriInfo;

import org.springframework.context.ApplicationContext;

import com.alibaba.webx.restful.model.ApplicationImpl;
import com.alibaba.webx.restful.model.Resource;
import com.alibaba.webx.restful.model.finder.ClassInfo;
import com.alibaba.webx.restful.model.finder.ResourceFinder;
import com.alibaba.webx.restful.model.finder.WebAppResourcesScanner;
import com.alibaba.webx.restful.model.param.ParameterProviderImpl;
import com.alibaba.webx.restful.process.ApplicationHandler;
import com.alibaba.webx.restful.process.RestfulComponent;
import com.alibaba.webx.restful.process.impl.UriInfoImpl;
import com.alibaba.webx.restful.spi.ParameterProvider;
import com.alibaba.webx.restful.util.ApplicationContextUtils;
import com.alibaba.webx.restful.util.ResourceUtils;

public class RestfulServletFilter implements Filter {

    private RestfulComponent component = null;

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        ApplicationContext applicationContxt = ApplicationContextUtils.getApplicationContext(filterConfig.getServletContext());

        ApplicationImpl applicationConfig = createResourceConfig(filterConfig, applicationContxt);

        component = new RestfulComponent(applicationConfig, applicationContxt);
    }

    @Override
    public void destroy() {

    }

    public RestfulComponent getComponent() {
        return component;
    }

    private ApplicationImpl createResourceConfig(FilterConfig filterConfig, ApplicationContext applicationContxt)
                                                                                                                 throws ServletException {

        final ApplicationImpl applicationConfig = new ApplicationImpl();

        List<ResourceFinder> resourceFinders = new ArrayList<ResourceFinder>();
        resourceFinders.add(new WebAppResourcesScanner(filterConfig.getServletContext()));

        ApplicationContextUtils.setApplicationContext(applicationContxt);

        ParameterProvider parameterProvider = new ParameterProviderImpl(applicationContxt);

        String[] packageNames = getConfigPackageNames(filterConfig);
        if (packageNames != null && packageNames.length != 0) {

        }

        Map<Class<?>, ClassInfo> scanResult = ResourceUtils.scanResources(resourceFinders, packageNames);

        for (Map.Entry<Class<?>, ClassInfo> entry : scanResult.entrySet()) {
            Resource resource = ResourceUtils.buildResource(applicationContxt, parameterProvider, entry.getKey(),
                                                            entry.getValue(), null);

            if (resource == null) {
                continue;
            }

            applicationConfig.addResource(resource);
        }

        return applicationConfig;
    }

    private String[] getConfigPackageNames(FilterConfig filterConfig) {
        final Map<String, Object> initParams = getInitParams(filterConfig);
        String packageProperty = (String) initParams.get(Constants.PROVIDER_PACKAGES);
        String[] packageNames = ResourceUtils.parsePropertyValue(packageProperty);
        return packageNames;
    }

    private Map<String, Object> getInitParams(FilterConfig webConfig) {
        Map<String, Object> props = new HashMap<String, Object>();
        Enumeration<?> names = webConfig.getInitParameterNames();
        while (names.hasMoreElements()) {
            String name = (String) names.nextElement();
            props.put(name, webConfig.getInitParameter(name));
        }
        return props;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException,
                                                                                             ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        ApplicationHandler handler = component.getHandler();

        UriInfo uriInfo = new UriInfoImpl(httpRequest);

        handler.service(httpRequest, httpResponse, uriInfo);
    }

}
