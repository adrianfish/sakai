/**
 * Copyright (c) 2007 The Apereo Foundation
 *
 * Licensed under the Educational Community License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *             http://opensource.org/licenses/ecl2
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.sakaiproject.scorm.dao.hibernate;

import java.util.Date;
import java.util.List;

import lombok.extern.slf4j.Slf4j;

import org.adl.datamodels.IDataManager;
import org.adl.datamodels.SCODataManager;

import org.sakaiproject.scorm.dao.api.DataManagerDao;

import org.springframework.orm.hibernate4.support.HibernateDaoSupport;

@Slf4j
public class DataManagerDaoImpl extends HibernateDaoSupport implements DataManagerDao
{
	@Override
	public List<IDataManager> find(long contentPackageId, String learnerId, long attemptNumber)
	{
		StringBuilder buffer = new StringBuilder();
		buffer.append("from ").append(SCODataManager.class.getName()).append(" where contentPackageId=? and userId=? and attemptNumber=? ");
		return (List<IDataManager>) getHibernateTemplate().find(buffer.toString(), new Object[] { contentPackageId, learnerId, attemptNumber });
	}

	@Override
	public IDataManager find(long contentPackageId, String learnerId, long attemptNumber, String scoId)
	{
		StringBuilder buffer = new StringBuilder();
		buffer.append("from ").append(SCODataManager.class.getName()).append(" where contentPackageId=? and userId=? and attemptNumber=? and scoId=?");
		List r = getHibernateTemplate().find(buffer.toString(), new Object[] { contentPackageId, learnerId, attemptNumber, scoId });

		if (r.isEmpty())
		{
			return null;
		}

		SCODataManager dm = (SCODataManager) r.get(0);
		return dm;
	}

	public List<IDataManager> find(String courseId)
	{
		List r = getHibernateTemplate().find("from " + SCODataManager.class.getName() + " where courseId=? ", new Object[] { courseId });
		return r;
	}

	@Override
	public IDataManager find(String courseId, String scoId, String userId, boolean fetchAll, long attemptNumber)
	{
		StringBuilder buffer = new StringBuilder();
		buffer.append("from ").append(SCODataManager.class.getName());

		if (fetchAll)
		{
			buffer.append(" fetch all properties ");
		}

		buffer.append(" where courseId=? and scoId=? and userId=? and attemptNumber=? ");
		List r = getHibernateTemplate().find(buffer.toString(), new Object[] { courseId, scoId, userId, attemptNumber });

		log.debug("DataManagerDaoImpl::find: records: {}", r.size());

		if (r.isEmpty())
		{
			return null;
		}

		SCODataManager dm = (SCODataManager) r.get(r.size() - 1);
		return dm;
	}

	@Override
	public IDataManager find(String courseId, String scoId, String userId, long attemptNumber)
	{
		return find(courseId, scoId, userId, true, attemptNumber);
	}

	@Override
	public IDataManager findByActivityId(long contentPackageId, String activityId, String userId, long attemptNumber)
	{
		StringBuilder buffer = new StringBuilder();
		buffer.append("from ").append(SCODataManager.class.getName());
		buffer.append(" where contentPackageId=? and activityId=? and userId=? and attemptNumber=? ");
		List r = getHibernateTemplate().find(buffer.toString(), new Object[] { contentPackageId, activityId, userId, attemptNumber });

		log.debug("DataManagerDaoImpl::findByActivityId: records: {}", r.size());

		if (r.isEmpty())
		{
			return null;
		}

		SCODataManager dm = (SCODataManager) r.get(r.size() - 1);
		return dm;
	}

	@Override
	public IDataManager load(long id)
	{
		return (IDataManager) getHibernateTemplate().load(SCODataManager.class, id);
	}

	@Override
	public void save(IDataManager dataManager)
	{
		saveOrUpdate(dataManager, true);
	}

	private void saveOrUpdate(boolean isFirstTime, Object object)
	{
		getHibernateTemplate().saveOrUpdate(object);
	}

	private void saveOrUpdate(IDataManager dataManager, boolean isFirstTime)
	{
		dataManager.setLastModifiedDate(new Date());
		saveOrUpdate(isFirstTime, dataManager);
	}

	@Override
	public void update(IDataManager dataManager)
	{
		saveOrUpdate(dataManager, false);
	}
}
