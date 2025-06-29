// Copyright (C) 2025 VoidSingularity

// This program is free software: you can redistribute it and/or modify it
// under the terms of the GNU Lesser General Public License as published by
// the Free Software Foundation, either version 3 of the License, or (at
// your option) any later version.

// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU Lesser General Public License for more details.

// You should have received a copy of the GNU Lesser General Public License
// along with this program.  If not, see <https://www.gnu.org/licenses/>.

package berry.api;

import java.util.List;

import berry.api.mixins.MixinInitialize;
import berry.loader.BerryClassLoader;
import berry.loader.BerryLoader;
import berry.loader.BerryModInitializer;
import berry.loader.JarContainer;
import berry.utils.StringSorter;

public class BuiltinAPIBootstrap implements BerryModInitializer {
    private static JarContainer container;
    public static JarContainer getContainer () {
        return container;
    }
    @Override
    public void preinit (StringSorter sorter, JarContainer jar, String name) {
        sorter.addValue (name);
        container = jar;
        try { ExternalLibrariesGenerated.init (); }
        catch (Exception e) { throw new RuntimeException (e); }
    }
    private static final List <String> services = List.of (
        "org.spongepowered.asm.service.IGlobalPropertyService",
        "org.spongepowered.asm.service.IMixinService",
        "org.spongepowered.asm.service.IMixinServiceBootstrap"
    );
    public void initialize (String[] argv) {
        // Get URL for services
        try {
            for (var service : services)
            BerryClassLoader.getInstance () .controlResources (
                "META-INF/services/" + service,
                BerryClassLoader.toURL ("META-INF/services/" + service, container.filepath ())
            );
        } catch (Exception e) { throw new RuntimeException (e); }

        // Mixin bootstrap
        MixinInitialize.initialize ();
        BerryLoader.preloaders.add (cl -> MixinInitialize.addcfg ());
    }
}
