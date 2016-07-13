package org.sakaiproject.snappoll.tool.entityprovider;

import java.util.Map;
import java.util.UUID;

import javax.servlet.http.HttpServletResponse;

import org.sakaiproject.component.api.ServerConfigurationService;
import org.sakaiproject.db.api.SqlService;
import org.sakaiproject.entitybroker.EntityView;
import org.sakaiproject.entitybroker.entityprovider.annotations.EntityCustomAction;
import org.sakaiproject.entitybroker.entityprovider.capabilities.ActionsExecutable;
import org.sakaiproject.entitybroker.entityprovider.capabilities.AutoRegisterEntityProvider;
import org.sakaiproject.entitybroker.entityprovider.capabilities.Describeable;
import org.sakaiproject.entitybroker.entityprovider.extension.ActionReturn;
import org.sakaiproject.entitybroker.entityprovider.extension.Formats;
import org.sakaiproject.entitybroker.exception.EntityException;
import org.sakaiproject.entitybroker.util.AbstractEntityProvider;

import org.apache.commons.lang.StringUtils;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Setter @Slf4j
public class SnapPollEntityProvider extends AbstractEntityProvider implements AutoRegisterEntityProvider, Describeable, ActionsExecutable {

    public final static String ENTITY_PREFIX = "snap-poll";

    private ServerConfigurationService serverConfigurationService;
    private SqlService sqlService;

    public void init() {

        if (serverConfigurationService.getBoolean("auto.ddl", true)) {
            sqlService.ddl(this.getClass().getClassLoader(), "snappoll_tables");
        }
    }

    public String getEntityPrefix() {
        return ENTITY_PREFIX;
    }

    public String[] getHandledOutputFormats() {
        return new String[] { Formats.TXT };
    }

    @EntityCustomAction(action = "showPollNow", viewKey = EntityView.VIEW_LIST)
    public String handleShowPollNow(EntityView view, Map<String, Object> params) {
        return (Math.random() <= 0.5D) ? "true" : "false";
    }

    @EntityCustomAction(action = "submitResponse", viewKey = EntityView.VIEW_NEW)
    public void handleSubmitResponse(EntityView view, Map<String, Object> params) {
        
        String userId = developerHelperService.getCurrentUserId();
        
        if (userId == null) {
            throw new EntityException("Not logged in", "", HttpServletResponse.SC_UNAUTHORIZED);
        }

        String siteId = (String) params.get("siteId");
        String response = (String) params.get("response");
        String reason = (String) params.get("reason");
        String tool = (String) params.get("tool");
        String context = (String) params.get("context");

        if (StringUtils.isEmpty(siteId) || StringUtils.isEmpty(tool)
                || StringUtils.isEmpty(response) || StringUtils.isEmpty(context) || StringUtils.isEmpty(reason)) {
            throw new EntityException("Bad request", "", HttpServletResponse.SC_BAD_REQUEST);
        }

        String id = UUID.randomUUID().toString();

        boolean success = sqlService.dbWrite("INSERT INTO SNAP_POLL_SUBMISSION VALUES(?,?,?,?,?,?,?)"
                                , new Object[] {id, userId, siteId, response, reason, tool, context});

        if (!success) {
            log.error("Failed to store submission.");
        }
    }
}
