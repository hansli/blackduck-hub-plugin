/*******************************************************************************
 * Copyright (C) 2016 Black Duck Software, Inc.
 * http://www.blackducksoftware.com/
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License version 2 only
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License version 2
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 *******************************************************************************/
package com.blackducksoftware.integration.hub.jenkins.remote;

import hudson.remoting.Callable;

import java.io.File;

import org.jenkinsci.remoting.Role;
import org.jenkinsci.remoting.RoleChecker;

import com.blackducksoftware.integration.hub.cli.CLIInstaller;
import com.blackducksoftware.integration.hub.jenkins.HubJenkinsLogger;

public class GetCLIExists implements Callable<Boolean, Exception> {
    private static final long serialVersionUID = 3459269768733083577L;

    private final HubJenkinsLogger logger;

    private final String directoryToInstallTo;

    public GetCLIExists(HubJenkinsLogger logger, String directoryToInstallTo) {
        this.logger = logger;
        this.directoryToInstallTo = directoryToInstallTo;
    }

    @Override
    public Boolean call() throws Exception {
        CLIInstaller installer = new CLIInstaller(new File(directoryToInstallTo));
        return installer.getCLIExists(logger);
    }

    @Override
    public void checkRoles(RoleChecker checker) throws SecurityException {
        checker.check(this, new Role(GetCLIExists.class));
    }
}
