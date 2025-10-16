package org.sakaiproject.tags.tool;

import org.sakaiproject.portal.util.PortalUtils;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import javax.servlet.http.HttpServletRequest;

@Controller
public class IndexController {

    @GetMapping("/")
    public String index(Model model, HttpServletRequest request) {

        model.addAttribute("cdnQuery", PortalUtils.getCDNQuery());
        model.addAttribute("sakaiHtmlHead", (String) request.getAttribute("sakai.html.head"));

        return "bootstrap";
    }
}
