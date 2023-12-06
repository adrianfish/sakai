package org.sakaiproject.pages.impl;

import org.sakaiproject.pages.api.PagesService;
import org.sakaiproject.pages.api.PageTransferBean;
import org.sakaiproject.pages.api.repository.PageRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

public class PagesServiceImpl implements PagesService {

    @Autowired PageRepository pageRepository;

    public PageTransferBean savePage(PageTransferBean bean) {

        return PageTransferBean.of(pageRepository.save(bean.asPage()));
    }

    public List<PageTransferBean> getPagesForSite(String siteId) {

        return pageRepository.findBySiteId(siteId).stream().map(PageTransferBean::of).collect(Collectors.toList());
    }
}
