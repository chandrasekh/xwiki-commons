/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.xwiki.extension.repository.internal.core;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.net.URL;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.environment.Environment;
import org.xwiki.extension.repository.internal.ExtensionSerializer;

/**
 * Store resolve core extension to not have to resolve it again at next restart.
 * 
 * @version $Id$
 * @since 6.4M1
 */
@Component(roles = CoreExtensionCache.class)
@Singleton
public class CoreExtensionCache implements Initializable
{
    @Inject
    private Environment environment;

    @Inject
    private ExtensionSerializer serializer;

    @Inject
    private Logger logger;

    private File folder;

    @Override
    public void initialize() throws InitializationException
    {
        File permanentDirectory = this.environment.getPermanentDirectory();
        if (permanentDirectory != null) {
            this.folder = new File(permanentDirectory, "cache/extension/core/");
        }
    }

    /**
     * @param extension the extension to store
     * @throws Exception when failing to store the extension
     */
    public void store(DefaultCoreExtension extension) throws Exception
    {
        if (this.folder == null) {
            return;
        }

        File file = getFile(extension.getDescriptorURL());

        // Make sure the file parents exist
        if (!file.exists()) {
            file.getParentFile().mkdirs();
        }

        try (FileOutputStream stream = new FileOutputStream(file)) {
            this.serializer.saveExtensionDescriptor(extension, stream);
        }
    }

    /**
     * @param repository the repository to set in the new extension instance
     * @param descriptorURL the extension descriptor URL
     * @return the extension corresponding to the passed descriptor URL, null if none could be found
     */
    public DefaultCoreExtension getExtension(DefaultCoreExtensionRepository repository, URL descriptorURL)
    {
        if (this.folder == null) {
            return null;
        }

        File file = getFile(descriptorURL);

        if (file.exists()) {
            try (FileInputStream stream = new FileInputStream(file)) {
                DefaultCoreExtension coreExtension =
                    this.serializer.loadCoreExtensionDescriptor(repository, descriptorURL, stream);

                coreExtension.setCached(true);

                return coreExtension;
            } catch (Exception e) {
                this.logger.warn("Failed to parse cached core extension", e);
            }
        }

        return null;
    }

    private File getFile(URL url)
    {
        // FIXME: make it more unique
        String fileName = String.valueOf(url.toExternalForm().hashCode());

        return new File(this.folder, fileName + ".xed");
    }
}