package org.sakaiproject.pages.impl;

import org.sakaiproject.pages.api.PagesService;
import org.sakaiproject.pages.api.PageTransferBean;
import org.sakaiproject.pages.api.repository.PageRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class PagesServiceImpl implements PagesService {

    @Autowired PageRepository pageRepository;

    public PageTransferBean savePage(PageTransferBean bean) {

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
}
