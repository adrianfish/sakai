package org.sakaiproject.pages.impl;

import org.sakaiproject.authz.api.FunctionManager;
import org.sakaiproject.authz.api.SecurityService;
import org.sakaiproject.component.api.ServerConfigurationService;
import org.sakaiproject.entity.api.Entity;
import org.sakaiproject.entity.api.EntityManager;
import org.sakaiproject.entity.api.EntityProducer;
import org.sakaiproject.entity.api.Reference;
import org.sakaiproject.pages.api.PagesPermissionException;
import org.sakaiproject.pages.api.PageReferenceReckoner;
import org.sakaiproject.pages.api.PagesService;
import org.sakaiproject.pages.api.PageTransferBean;
import org.sakaiproject.pages.api.Permissions;
import org.sakaiproject.pages.api.repository.PageRepository;
import org.sakaiproject.site.api.Site;
import org.sakaiproject.site.api.SiteService;
import org.sakaiproject.site.api.ToolConfiguration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class PagesServiceImpl implements PagesService, EntityProducer {

    @Autowired EntityManager entityManager;

    @Autowired FunctionManager functionManager;

    @Autowired PageRepository pageRepository;

    @Autowired SecurityService securityService;
    @Autowired ServerConfigurationService serverConfigurationService;

    @Autowired SiteService siteService;

    public void init() {

        functionManager.registerFunction(Permissions.ADD_PAGE, true);
        functionManager.registerFunction(Permissions.DELETE_PAGE, true);
        functionManager.registerFunction(Permissions.EDIT_PAGE, true);
        functionManager.registerFunction(Permissions.PUBLISH_PAGE, true);

        entityManager.registerEntityProducer(this, REFERENCE_ROOT);
    }

    public PageTransferBean savePage(PageTransferBean bean) throws PagesPermissionException {

        if (!securityService.unlock(Permissions.ADD_PAGE, siteService.siteReference(bean.siteId))) {
            throw new PagesPermissionException();
        }

        return addPermissions(PageTransferBean.of(pageRepository.save(bean.asPage())));
    }

    public Collection<PageTransferBean> getPagesForSite(String siteId, boolean populate) throws PagesPermissionException {

        if (!securityService.unlock(SiteService.SITE_VISIT, siteService.siteReference(siteId))) {
            throw new PagesPermissionException();
        }

        return pageRepository.findBySiteId(siteId).stream().map(page -> {

            PageTransferBean bean = PageTransferBean.of(page);
            if (!populate) {
                bean.content = "";
            }
            return addPermissions(bean);
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

    private PageTransferBean addPermissions(PageTransferBean bean) {

        bean.canDelete = securityService.unlock(Permissions.DELETE_PAGE, siteService.siteReference(bean.siteId));
        bean.canEdit = securityService.unlock(Permissions.EDIT_PAGE, siteService.siteReference(bean.siteId));
        return bean;
    }

    public boolean parseEntityReference(String reference, Reference ref) {
        return reference.startsWith(REFERENCE_ROOT);
    }

    public Entity getEntity(Reference ref) {

        PageReferenceReckoner.PageReference pageReference = PageReferenceReckoner.reckoner().reference(ref.getReference()).reckon();
        try {
            return getPage(pageReference.getSiteId(), pageReference.getId()).orElse(null);
        } catch (PagesPermissionException ppe) {
            log.warn("No permission to get page with id {}", pageReference.getId());
        }

        return null;
    }

    public Optional<String> getEntityUrl(Reference ref, Entity.UrlType urlType) {

        PageReferenceReckoner.PageReference pageReference = PageReferenceReckoner.reckoner().reference(ref.getReference()).reckon();

        try {
            Site site = siteService.getSite(pageReference.getSiteId());
            ToolConfiguration tc = site.getToolForCommonId(PagesService.TOOL_ID);
            return Optional.of(serverConfigurationService.getPortalUrl() + "/directtool/" + tc.getId() + "/pages/" + pageReference.getId());
        } catch (Exception e) {
            log.warn("Failed to url for reference {}: {}", ref.getReference(), e.toString());
        }

        return Optional.empty();
    }
}
