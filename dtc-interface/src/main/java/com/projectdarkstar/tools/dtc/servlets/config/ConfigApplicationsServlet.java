/*
 * Copyright 2008 Sun Microsystems, Inc.
 *
 * This file is part of the Darkstar Test Cluster
 *
 * Darkstar Test Cluster is free software: you can redistribute it
 * and/or modify it under the terms of the GNU General Public License
 * version 2 as published by the Free Software Foundation and
 * distributed hereunder to you.
 *
 * Darkstar Test Cluster is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.projectdarkstar.tools.dtc.servlets.config;

/**
 * Services configuration http requests.
 */
public class ConfigApplicationsServlet extends ConfigServlet {
    
    @Override
    protected String getView() {
        return "/jsp/config-applications.jsp";
    }
    
    @Override
    public void init() {
        super.init();
        getTrail().add("Applications");
        getTrail().add(this.getServletContext().getContextPath() + "/applications");
    }
}