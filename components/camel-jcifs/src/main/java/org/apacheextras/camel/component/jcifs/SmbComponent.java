/**************************************************************************************
 https://camel-extra.github.io

 This program is free software; you can redistribute it and/or
 modify it under the terms of the GNU Lesser General Public License
 as published by the Free Software Foundation; either version 3
 of the License, or (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU Lesser General Public License for more details.


 You should have received a copy of the GNU Lesser General Public
 License along with this program; if not, write to the Free Software
 Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 02110-1301, USA.

 http://www.gnu.org/licenses/lgpl-3.0-standalone.html
 ***************************************************************************************/
package org.apacheextras.camel.component.jcifs;

import java.net.URI;
import java.util.Map;

import org.apache.camel.CamelContext;
import org.apache.camel.component.file.GenericFileComponent;
import org.apache.camel.component.file.GenericFileEndpoint;

import jcifs.smb.SmbFile;
import org.apache.camel.spi.Metadata;
import org.apache.camel.spi.annotations.Component;

@Component("smb")
public class SmbComponent extends GenericFileComponent<SmbFile> {

    @Metadata(label = "advanced")
    private SmbApiFactory smbApiFactory;

    public SmbComponent() {

    }

    public SmbComponent(final CamelContext context) {
        super(context);
    }

    /**
     * The factory for creating jcifs API objects.
     */
    public void setSmbApiFactoryClass(final SmbApiFactory smbApiFactory) {
        this.smbApiFactory = smbApiFactory;
    }

    public SmbApiFactory getSmbApiFactoryClass() {
        return smbApiFactory;
    }

    @Override
    protected SmbEndpoint buildFileEndpoint(String uri, final String remaining, final Map<String, Object> parameters) throws Exception {
        log.debug("buildFileEndpoint() uri[{}] remaining[{}] parameters[{}]", uri, remaining, parameters);
        uri = fixSpaces(uri);
        SmbConfiguration config = new SmbConfiguration(new URI(uri));
        return new SmbEndpoint(uri, this, config);
    }

    @Override
    protected void afterPropertiesSet(final GenericFileEndpoint<SmbFile> endpoint) throws Exception {
        log.debug("afterPropertiesSet()");
    }

    private String fixSpaces(final String input) {
        return input.replace(" ", "%20");
    }
}
