/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2013 Kai Reinhard (k.reinhard@micromata.de)
//
// ProjectForge is dual-licensed.
//
// This community edition is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License as published
// by the Free Software Foundation; version 3 of the License.
//
// This community edition is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
// Public License for more details.
//
// You should have received a copy of the GNU General Public License along
// with this program; if not, see http://www.gnu.org/licenses/.
//
/////////////////////////////////////////////////////////////////////////////

package org.projectforge.plugins.teamcal.rest;

import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.projectforge.plugins.teamcal.admin.TeamCalDO;
import org.projectforge.plugins.teamcal.admin.TeamCalDao;
import org.projectforge.plugins.teamcal.admin.TeamCalFilter;
import org.projectforge.registry.Registry;
import org.projectforge.web.rest.JsonUtils;

/**
 * REST-Schnittstelle für {@link TeamCalDao}
 * 
 * @author Kai Reinhard (k.reinhard@micromata.de)
 * 
 */
@Path("teamcal")
public class TeamCalDaoRest
{
  private final TeamCalDao teamCalDao;

  public TeamCalDaoRest()
  {
    this.teamCalDao = Registry.instance().getDao(TeamCalDao.class);
  }

  /**
   * Rest-Call für: {@link TeamCalDao#getList(org.projectforge.core.BaseSearchFilter)}
   * 
   * @param searchTerm
   */
  @GET
  @Path("callist")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getList()
  {
    final TeamCalFilter filter = new TeamCalFilter();
    final List<TeamCalDO> list = teamCalDao.getList(filter);
    final String json = JsonUtils.toJson(list);
    return Response.ok(json).build();
  }
}
