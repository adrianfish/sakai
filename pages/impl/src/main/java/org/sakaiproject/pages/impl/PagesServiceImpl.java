package org.sakaiproject.pages.impl;

import org.sakaiproject.authz.api.FunctionManager;
import org.sakaiproject.authz.api.SecurityService;
import org.sakaiproject.pages.api.PagesPermissionException;
import org.sakaiproject.pages.api.PagesService;
import org.sakaiproject.pages.api.PageTransferBean;
import org.sakaiproject.pages.api.Permissions;
import org.sakaiproject.pages.api.repository.PageRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class PagesServiceImpl implements PagesService {

    @Autowired FunctionManager functionManager;

    @Autowired PageRepository pageRepository;

    @Autowired SecurityService securityService;

    public void init() {

        functionManager.registerFunction(Permissions.ADD_PAGE, true);
        functionManager.registerFunction(Permissions.DELETE_PAGE, true);
    }

    public PageTransferBean savePage(PageTransferBean bean) throws PagesPermissionException {

        if (!securityService.unlock(Permissions.ADD_PAGE, "/site/" + bean.siteId)) {
            throw new PagesPermissionException();
        }

        System.out.println(bean.asPage().getId());

        return PageTransferBean.of(pageRepository.save(bean.asPage()));
    }

    public List<PageTransferBean> getPagesForSite(String siteId, boolean populate) {

        // TODO: Add some bloody security

        return pageRepository.findBySiteId(siteId).stream().map(page -> {

            PageTransferBean bean = PageTransferBean.of(page);
            if (!populate) {
                bean.content = "";
            }
            return bean;
        }).collect(Collectors.toList());
    }

    public Optional<PageTransferBean> getPage(String siteId, String pageId) {

        // TODO: Add some bloody security

        return pageRepository.findById(pageId).map(PageTransferBean::of);
    }

    public void deletePage(String pageId, String siteId) throws PagesPermissionException {

        if (!securityService.unlock(Permissions.DELETE_PAGE, "/site/" + siteId)) {
            throw new PagesPermissionException();
        }

        pageRepository.deleteById(pageId);
    }
}
