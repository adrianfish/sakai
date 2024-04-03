package org.sakaiproject.pages.impl;

import org.sakaiproject.authz.api.FunctionManager;
import org.sakaiproject.authz.api.SecurityService;
import org.sakaiproject.pages.api.PagesPermissionException;
import org.sakaiproject.pages.api.PagesService;
import org.sakaiproject.pages.api.PageTransferBean;
import org.sakaiproject.pages.api.Permissions;
import org.sakaiproject.pages.api.repository.PageRepository;
import org.sakaiproject.site.api.SiteService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class PagesServiceImpl implements PagesService {

    @Autowired FunctionManager functionManager;

    @Autowired PageRepository pageRepository;

    @Autowired SecurityService securityService;

    @Autowired SiteService siteService;

    public void init() {

        functionManager.registerFunction(Permissions.ADD_PAGE, true);
        functionManager.registerFunction(Permissions.DELETE_PAGE, true);
        functionManager.registerFunction(Permissions.EDIT_PAGE, true);
    }

    public PageTransferBean savePage(PageTransferBean bean) throws PagesPermissionException {

        if (!securityService.unlock(Permissions.ADD_PAGE, siteService.siteReference(bean.siteId))) {
            throw new PagesPermissionException();
        }

        return PageTransferBean.of(pageRepository.save(bean.asPage()));
    }

    public List<PageTransferBean> getPagesForSite(String siteId, boolean populate) throws PagesPermissionException {

        if (!securityService.unlock(SiteService.SITE_VISIT, siteService.siteReference(siteId))) {
            throw new PagesPermissionException();
        }

        return pageRepository.findBySiteId(siteId).stream().map(page -> {

            PageTransferBean bean = PageTransferBean.of(page);
            if (!populate) {
                bean.content = "";
            }
            return bean;
        }).collect(Collectors.toList());
    }

    public Optional<PageTransferBean> getPage(String siteId, String pageId) throws PagesPermissionException {

        if (!securityService.unlock(SiteService.SITE_VISIT, siteService.siteReference(siteId))) {
            throw new PagesPermissionException();
        }

        return pageRepository.findById(pageId).map(PageTransferBean::of);
    }

    public void deletePage(String siteId, String pageId) throws PagesPermissionException {

        if (!securityService.unlock(Permissions.DELETE_PAGE, siteService.siteReference(siteId))) {
            throw new PagesPermissionException();
        }

        pageRepository.deleteById(pageId);
    }
}
